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
import kotlin.math.abs

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

    @Volatile
    private var ovalPositionChanges = 0

    @Volatile
    private var iouSum = 0f

    @Volatile
    private var iouSamples = 0

    @Volatile
    private var lowestIou = Float.MAX_VALUE

    @Volatile
    private var framesAnalyzedAtFirstMatch = 0

    @Volatile
    private var terminalCloseCode: String? = null

    private var lastFaceCount: Int? = null
    private var lastOvalPosition: FaceDetector.FaceOvalPosition? = null
    private var lastObservedOvalPosition: FaceDetector.FaceOvalPosition? = null
    private var candidateOvalPosition: FaceDetector.FaceOvalPosition? = null
    private var candidateOvalPositionFrames = 0
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

    @Volatile
    private var phase: ChallengePhase = ChallengePhase.PRE_CHECK

    private var colorSequenceStartedLogged = false
    private var challengeCompletedLogged = false

    @Volatile
    private var faceDistanceThresholdMin = 0f

    @Volatile
    private var lastEmittedDistance: Float? = null

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
        faceDistanceThresholdMin: Float,
        targetIouThreshold: Float,
        ovalFitTimeout: Int
    ) {
        this.faceDetectionThreshold = faceDetectionThreshold
        this.faceDistanceThresholdMin = faceDistanceThresholdMin
        emit(
            SessionReady(
                sessionId = sessionId,
                challengeType = challengeType,
                challengeId = challengeId,
                faceDetectionThreshold = faceDetectionThreshold,
                targetIouThreshold = targetIouThreshold,
                ovalFitTimeout = ovalFitTimeout
            )
        )
    }

    fun onCameraBound(facing: String) = emit(CameraBound(sessionId = sessionId, facing = facing))

    /**
     * Called for every colour displayed during a colour challenge. Only the first call changes the
     * phase and emits a marker; the rest are ignored.
     */
    fun onColorDisplayed() {
        phase = ChallengePhase.COLOR_SEQUENCE
        if (colorSequenceStartedLogged) return
        colorSequenceStartedLogged = true
        emit(ColorSequenceStarted(sessionId = sessionId))
    }

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
        // Row stride is reported in buffer coordinates, so it is compared against the delivered
        // width before any rotation is applied.
        val expectedRowStride = width * BYTES_PER_RGBA_PIXEL

        // The target resolution describes the frame after rotation, which a camera reporting a
        // quarter turn delivers with its dimensions transposed. Comparing raw dimensions would
        // report a mismatch for every such device.
        val quarterTurned = rotationDegrees == 90 || rotationDegrees == 270
        val rotatedWidth = if (quarterTurned) height else width
        val rotatedHeight = if (quarterTurned) width else height
        val matchesTarget = rotatedWidth == LivenessCoordinator.TARGET_WIDTH &&
            rotatedHeight == LivenessCoordinator.TARGET_HEIGHT
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

    /**
     * Called when the active challenge finishes. Emitted once; the movement-only call site sits in
     * a composable effect, so a repeat call is possible and is ignored.
     */
    fun onChallengeCompleted() {
        if (challengeCompletedLogged) return
        challengeCompletedLogged = true
        emit(
            ChallengeCompleted(
                sessionId = sessionId,
                framesAnalyzed = framesAnalyzed,
                phase = phase
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
                        sessionId = sessionId,
                        from = previous,
                        to = faceCount,
                        framesAnalyzed = framesAnalyzed,
                        topScore = topScore,
                        phase = phase
                    )
                )
            }
        }
    }

    /**
     * Reports the face oval position measured on this frame.
     *
     * A position change is only logged once it has held for [OVAL_POSITION_DEBOUNCE_FRAMES]
     * consecutive frames. A face resting on the match boundary flips position on frame-to-frame
     * variation alone, which would otherwise emit a burst of changes for a user who is holding
     * still. Every observed change is still counted into [SessionEnded], so the oscillation
     * remains visible as a number.
     */
    /**
     * Records the overlap between the detected face and the oval for this frame. Accumulated only;
     * the statistics are reported once in [SessionEnded], so this emits nothing per frame.
     *
     * Frames before the first oval match are ignored. The face is still being brought into
     * position then, so including them measures how long the user took to line up rather than how
     * well they held position once the challenge was underway.
     */
    fun onFaceIou(iou: Float) {
        if (!ovalEverMatched) return
        iouSum += iou
        iouSamples += 1
        if (iou < lowestIou) lowestIou = iou
    }

    fun onOvalPositionChanged(position: FaceDetector.FaceOvalPosition) {
        if (position == FaceDetector.FaceOvalPosition.MATCHED) {
            ovalEverMatched = true
            if (!firstOvalMatchLogged) {
                firstOvalMatchLogged = true
                framesAnalyzedAtFirstMatch = framesAnalyzed
                emit(
                    FirstOvalMatch(
                        sessionId = sessionId,
                        framesAnalyzed = framesAnalyzed,
                        phase = phase
                    )
                )
            }
        }

        // Counted before the debounce, so the summary reflects what was measured.
        if (position != lastObservedOvalPosition) {
            if (lastObservedOvalPosition != null) {
                ovalPositionChanges += 1
            }
            lastObservedOvalPosition = position
        }

        if (position == lastOvalPosition) {
            candidateOvalPosition = null
            candidateOvalPositionFrames = 0
            return
        }

        if (position == candidateOvalPosition) {
            candidateOvalPositionFrames += 1
        } else {
            candidateOvalPosition = position
            candidateOvalPositionFrames = 1
        }
        if (candidateOvalPositionFrames < OVAL_POSITION_DEBOUNCE_FRAMES) return

        val previous = lastOvalPosition
        lastOvalPosition = position
        candidateOvalPosition = null
        candidateOvalPositionFrames = 0
        emit(
            OvalPositionChanged(
                sessionId = sessionId,
                from = previous,
                to = position,
                phase = phase
            )
        )
    }

    /**
     * Reports the apparent face size measured on this frame. Emits only when the value has moved
     * at least [DISTANCE_REPORT_DELTA] from the last emitted one, because the measurement is
     * continuous and arrives on every analysed frame.
     */
    fun onFaceDistance(distance: Float) {
        val last = lastEmittedDistance
        if (last != null && abs(distance - last) < DISTANCE_REPORT_DELTA) return
        lastEmittedDistance = distance
        emit(
            FaceDistanceChanged(
                sessionId = sessionId,
                distance = distance,
                faceDistanceThresholdMin = faceDistanceThresholdMin,
                exceedsThresholdMin = distance >= faceDistanceThresholdMin,
                phase = phase
            )
        )
    }

    fun onInitialFaceDetectedSent() {
        phase = ChallengePhase.OVAL_FIT
        emit(
            InitialFaceDetected(
                sessionId = sessionId,
                elapsedSinceFirstFrameMs = firstFrameAt?.let { clock() - it } ?: 0L
            )
        )
    }

    fun onAnalyzeError(throwable: Throwable) {
        analyzeErrors += 1
        if (firstAnalyzeErrorLogged) return
        firstAnalyzeErrorLogged = true
        emit(AnalyzeFailed(sessionId = sessionId, throwable = throwable))
    }

    fun onSessionError(type: String, stopSession: Boolean, closeCode: String) {
        cancelWatchdog()
        // Retained for the session summary. The host removes the detector in response to the
        // error, so by the time the session ends the only code available describes that disposal
        // rather than the reason the session actually ended. The first error is kept, since a
        // later one is a consequence of it.
        if (terminalCloseCode == null) {
            terminalCloseCode = closeCode
        }
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
                ovalPositionChanges = ovalPositionChanges,
                // Derived rather than counted, so it cannot drift out of step with the sample
                // count regardless of the order the per-frame hooks are called in.
                challengeFrames = if (ovalEverMatched) framesAnalyzed - framesAnalyzedAtFirstMatch + 1 else 0,
                challengeFramesWithFace = iouSamples,
                challengeMinIou = if (iouSamples == 0) 0f else lowestIou,
                challengeMeanIou = if (iouSamples == 0) 0f else iouSum / iouSamples,
                analyzeErrors = analyzeErrors,
                closeCode = terminalCloseCode ?: closeCode
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
        private const val DISTANCE_REPORT_DELTA = 0.05f
        private const val OVAL_POSITION_DEBOUNCE_FRAMES = 3
        private const val WATCHDOG_THREAD_NAME = "LivenessFaceDetectionWatchdog"
    }
}
