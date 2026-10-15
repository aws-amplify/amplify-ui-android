/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.liveness.state

import androidx.annotation.VisibleForTesting
import com.amplifyframework.core.Amplify
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.Logger
import com.amplifyframework.ui.liveness.camera.LivenessCoordinator
import com.amplifyframework.ui.liveness.ml.FaceDetector
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * Emits diagnostic log lines for one liveness session.
 *
 * All throttling lives here: each line is emitted once, only when a tracked value changes, or by the
 * face detection watchdog. Nothing is emitted per frame, because frames arrive at 24fps.
 */
internal class LivenessSessionDiagnostics(
    private val sessionId: String,
    private val deviceInfo: DeviceInfo,
    private val logger: Logger = Amplify.Logging.forNamespace("Liveness"),
    private val clock: () -> Long = System::currentTimeMillis
) {
    @Volatile
    private var framesAnalyzed = 0

    @Volatile
    private var maxDetectionScore = 0f

    @Volatile
    private var analyzeErrors = 0

    @Volatile
    private var faceEverDetected = false

    @Volatile
    private var ovalEverMatched = false

    @Volatile
    private var faceDetectionThreshold = 0f

    @Volatile
    private var firstFrameAt: Long? = null

    private var lastFaceCount: Int? = null
    private var lastOvalPosition: FaceDetector.FaceOvalPosition? = null
    private var firstFrameLogged = false
    private var frameBeforeSessionReadyLogged = false
    private var firstOvalMatchLogged = false
    private var firstAnalyzeErrorLogged = false
    private var watchdogStarted = false
    private var watchdogFired = false
    private var watchdogTimer: Timer? = null
    private var watchdogStartedAt: Long? = null
    private var gateProvider: (() -> GateSnapshot)? = null
    private var destroyed = false

    fun onSessionStartRequested(attempt: Int, codec: String, preCheckEnabled: Boolean) = emit(
        SessionStarting(
            sessionId = sessionId,
            attempt = attempt,
            device = deviceInfo.device,
            api = deviceInfo.sdkInt,
            livenessVersion = deviceInfo.livenessVersion,
            codec = codec,
            preCheckEnabled = preCheckEnabled
        )
    )

    fun onSessionReady(
        challengeType: String,
        challengeId: String,
        faceDetectionThreshold: Float,
        ovalFitTimeout: Int
    ) {
        this.faceDetectionThreshold = faceDetectionThreshold
        emit(
            SessionReady(
                sessionId = sessionId,
                challengeType = challengeType,
                challengeId = challengeId,
                faceDetectionThreshold = faceDetectionThreshold,
                ovalFitTimeout = ovalFitTimeout
            )
        )
    }

    fun onCameraBound(facing: String) = emit(CameraBound(sessionId = sessionId, facing = facing))

    fun onFirstFrame(
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int,
        format: String
    ) {
        if (firstFrameLogged) return
        firstFrameLogged = true
        firstFrameAt = clock()
        val expectedRowStride = width * BYTES_PER_RGBA_PIXEL
        val matchesTarget = width == LivenessCoordinator.TARGET_WIDTH &&
            height == LivenessCoordinator.TARGET_HEIGHT
        emit(
            FirstFrame(
                sessionId = sessionId,
                frame = "${width}x$height",
                targetFrame = "${LivenessCoordinator.TARGET_WIDTH}x${LivenessCoordinator.TARGET_HEIGHT}",
                resolutionMatchesTarget = matchesTarget,
                rowStride = rowStride,
                expectedRowStride = expectedRowStride,
                rowStridePadded = rowStride != expectedRowStride,
                pixelStride = pixelStride,
                rotation = rotationDegrees,
                format = format
            )
        )
    }

    fun onFrameBeforeSessionReady() {
        if (frameBeforeSessionReadyLogged) return
        frameBeforeSessionReadyLogged = true
        emit(FrameBeforeSessionReady)
    }

    fun onFrameAnalyzed(faceCount: Int, topScore: Float) {
        framesAnalyzed += 1
        if (topScore > maxDetectionScore) maxDetectionScore = topScore
        if (faceCount >= 1) {
            faceEverDetected = true
            cancelWatchdog()
        } else {
            startWatchdogIfNeeded()
        }

        val previous = lastFaceCount
        if (previous != faceCount) {
            lastFaceCount = faceCount
            if (previous != null) {
                emit(
                    FaceCountChanged(
                        from = previous,
                        to = faceCount,
                        framesAnalyzed = framesAnalyzed,
                        topScore = topScore
                    )
                )
            }
        }
    }

    fun onOvalPositionChanged(position: FaceDetector.FaceOvalPosition) {
        if (position == FaceDetector.FaceOvalPosition.MATCHED) {
            ovalEverMatched = true
            if (!firstOvalMatchLogged) {
                firstOvalMatchLogged = true
                emit(FirstOvalMatch(framesAnalyzed = framesAnalyzed))
            }
        }
        val previous = lastOvalPosition
        if (previous == position) return
        lastOvalPosition = position
        emit(OvalPositionChanged(from = previous, to = position))
    }

    fun onInitialFaceDetectedSent() = emit(
        InitialFaceDetected(
            sessionId = sessionId,
            elapsedSinceFirstFrameMs = firstFrameAt?.let { clock() - it } ?: 0L
        )
    )

    fun onAnalyzeError(throwable: Throwable) {
        analyzeErrors += 1
        if (firstAnalyzeErrorLogged) return
        firstAnalyzeErrorLogged = true
        emit(AnalyzeFailed(sessionId = sessionId, throwable = throwable))
    }

    fun onSessionError(type: String, stopSession: Boolean, closeCode: String) {
        cancelWatchdog()
        emit(
            SessionError(
                sessionId = sessionId,
                type = type,
                stopSession = stopSession,
                closeCode = closeCode
            )
        )
    }

    @Synchronized
    fun onDestroy(closeCode: String) {
        if (destroyed) return
        destroyed = true
        cancelWatchdog()
        emit(
            SessionEnded(
                sessionId = sessionId,
                framesAnalyzed = framesAnalyzed,
                faceEverDetected = faceEverDetected,
                ovalEverMatched = ovalEverMatched,
                analyzeErrors = analyzeErrors,
                closeCode = closeCode
            )
        )
    }

    /** Supplies the gate flags the watchdog reads. Called by [LivenessState], which owns them. */
    fun registerGateProvider(provider: () -> GateSnapshot) {
        gateProvider = provider
    }

    @Synchronized
    private fun startWatchdogIfNeeded() {
        if (watchdogStarted || destroyed) return
        watchdogStarted = true
        watchdogStartedAt = clock()
        val timer = Timer(WATCHDOG_THREAD_NAME, true)
        watchdogTimer = timer
        timer.schedule(FACE_DETECTION_WATCHDOG_TIMEOUT_MS) { emitWatchdogSnapshot() }
    }

    /** Cancels the timer itself, not just the task, so its thread is released immediately. */
    @Synchronized
    private fun cancelWatchdog() {
        watchdogTimer?.cancel()
        watchdogTimer = null
    }

    /** Visible so tests can fire the watchdog without waiting on wall-clock time. */
    @VisibleForTesting
    @Synchronized
    internal fun emitWatchdogSnapshot() {
        if (watchdogFired || destroyed || faceEverDetected) return
        watchdogFired = true
        val gates = gateProvider?.invoke()
        emit(
            FaceDetectionWatchdog(
                sessionId = sessionId,
                elapsedMs = watchdogStartedAt?.let { clock() - it } ?: FACE_DETECTION_WATCHDOG_TIMEOUT_MS,
                framesAnalyzed = framesAnalyzed,
                maxDetectionScore = maxDetectionScore,
                faceDetectionThreshold = faceDetectionThreshold,
                analyzeErrors = analyzeErrors,
                showingStartView = gates?.showingStartView ?: false,
                readyForOval = gates?.readyForOval ?: false,
                faceTargetChallenge = when {
                    gates == null -> "unknown"
                    gates.faceTargetChallengePresent -> "present"
                    else -> "absent"
                },
                initialFaceDistanceCheckPassed = gates?.initialFaceDistanceCheckPassed ?: false,
                checkState = gates?.checkState ?: "unknown"
            )
        )
    }

    private fun emit(event: LivenessDiagnosticEvent) {
        val message = "Liveness $event"
        when (event.level) {
            LogLevel.ERROR -> logger.error(message, event.throwable)
            LogLevel.WARN -> logger.warn(message, event.throwable)
            LogLevel.INFO -> logger.info(message)
            LogLevel.DEBUG -> logger.debug(message)
            LogLevel.VERBOSE -> logger.verbose(message)
            LogLevel.NONE -> Unit
        }
    }

    companion object {
        @VisibleForTesting
        const val FACE_DETECTION_WATCHDOG_TIMEOUT_MS = 10_000L
        private const val BYTES_PER_RGBA_PIXEL = 4
        private const val WATCHDOG_THREAD_NAME = "LivenessFaceDetectionWatchdog"
    }
}
