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

import com.amplifyframework.logging.LogLevel
import com.amplifyframework.ui.liveness.ml.FaceDetector

/**
 * A single diagnostic log line. The rendered form of each event is its [toString], so a data class
 * supplies the `key=value` payload for free.
 */
internal sealed interface LivenessDiagnosticEvent {
    val level: LogLevel

    /** Passed to the logger separately so a stack trace is preserved rather than stringified. */
    val throwable: Throwable? get() = null
}

/** Events that carry the session identifier, declared first so it renders first. */
internal sealed interface SessionEvent : LivenessDiagnosticEvent {
    val sessionId: String
}

/** Static device and build metadata, captured once per session. */
internal data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val livenessVersion: String
) {
    val device: String get() = "$manufacturer/$model"
}

/** The gate flags the watchdog reads when it fires. */
internal data class GateSnapshot(
    val showingStartView: Boolean,
    val readyForOval: Boolean,
    val faceTargetChallengePresent: Boolean,
    val initialFaceDistanceCheckPassed: Boolean,
    val checkState: String
)

/**
 * Which part of the check a face observation belongs to.
 *
 * [COLOR_SEQUENCE] is reachable only for a colour challenge; a movement-only challenge never
 * displays a colour, so it stays at [OVAL_FIT] for the whole check.
 */
internal enum class ChallengePhase {
    PRE_CHECK,
    OVAL_FIT,
    COLOR_SEQUENCE
}

internal data class SessionStarting(
    override val sessionId: String,
    val attempt: Int,
    val device: String,
    val api: Int,
    val livenessVersion: String,
    val codec: String,
    val preCheckEnabled: Boolean
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class FirstFrame(
    override val sessionId: String,
    val frame: String,
    val targetFrame: String,
    val resolutionMatchesTarget: Boolean,
    val rowStride: Int,
    val expectedRowStride: Int,
    val rowStridePadded: Boolean,
    val pixelStride: Int,
    val rotation: Int,
    val format: String
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class SessionReady(
    override val sessionId: String,
    val challengeType: String,
    val challengeId: String,
    val faceDetectionThreshold: Float,
    val targetIouThreshold: Float,
    val ovalFitTimeout: Int
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class CameraBound(
    override val sessionId: String,
    val facing: String
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class InitialFaceDetected(
    override val sessionId: String,
    val elapsedSinceFirstFrameMs: Long
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class SessionEnded(
    override val sessionId: String,
    val framesAnalyzed: Int,
    val faceEverDetected: Boolean,
    val ovalEverMatched: Boolean,
    /**
     * Every face oval position change observed, including the rapid ones suppressed by the
     * reporting debounce. A count far above the number of logged changes means the face sat on the
     * match boundary, where frame-to-frame variation tips the position back and forth.
     */
    val ovalPositionChanges: Int,
    /**
     * The lowest and mean intersection-over-union between the detected face and the oval, over
     * every frame for which the oval was displayed.
     *
     * A graded measure is used rather than a count of matched frames because `MATCHED` requires an
     * IoU threshold and three edge alignments to hold at once — a boundary the face passes through
     * rather than rests in, so any count derived from it saturates. These are comparable between
     * sessions and are read against the `targetIouThreshold` reported at session ready.
     */
    val minIou: Float,
    val meanIou: Float,
    val analyzeErrors: Int,
    val closeCode: String
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class SessionError(
    override val sessionId: String,
    val type: String,
    val stopSession: Boolean,
    val closeCode: String
) : SessionEvent {
    override val level = LogLevel.WARN
}

internal data class FaceDetectionWatchdog(
    override val sessionId: String,
    val elapsedMs: Long,
    val framesAnalyzed: Int,
    val maxDetectionScore: Float,
    val faceDetectionThreshold: Float,
    val analyzeErrors: Int,
    val showingStartView: Boolean,
    val readyForOval: Boolean,
    val faceTargetChallenge: String,
    val initialFaceDistanceCheckPassed: Boolean,
    val checkState: String
) : SessionEvent {
    override val level = LogLevel.WARN
}

/**
 * Not a data class: a generated [toString] would render the throwable into the message as well as
 * handing it to the logger, printing it twice.
 */
internal class AnalyzeFailed(
    override val sessionId: String,
    override val throwable: Throwable
) : SessionEvent {
    override val level = LogLevel.ERROR
    override fun toString() = "AnalyzeFailed(sessionId=$sessionId)"
}

internal data object FrameBeforeSessionReady : LivenessDiagnosticEvent {
    override val level = LogLevel.DEBUG
}

internal data class FaceCountChanged(
    override val sessionId: String,
    val from: Int,
    val to: Int,
    val framesAnalyzed: Int,
    val topScore: Float,
    val phase: ChallengePhase
) : SessionEvent {
    override val level = LogLevel.INFO
}

internal data class OvalPositionChanged(
    override val sessionId: String,
    val from: FaceDetector.FaceOvalPosition?,
    val to: FaceDetector.FaceOvalPosition,
    val phase: ChallengePhase
) : SessionEvent {
    // Reported at debug: the oval match bounds are tuned to coach the user, not to match what the
    // service tolerates, so a position change does not predict a failed check. The share of the
    // challenge spent off target, reported once in SessionEnded, is the comparable signal.
    override val level = LogLevel.DEBUG
}

internal data class FirstOvalMatch(
    override val sessionId: String,
    val framesAnalyzed: Int,
    val phase: ChallengePhase
) : SessionEvent {
    override val level = LogLevel.INFO
}

/**
 * The measured apparent face size. The value rises as the face moves closer, so a falling value
 * means the face is moving away.
 *
 * The raw value is reported rather than a classification. [faceDistanceThresholdMin] is an upper
 * bound on closeness, so it can only identify a face that is too near; the service provides no
 * lower bound, which means a face that has moved too far away is detectable only from the trend in
 * [distance].
 */
internal data class FaceDistanceChanged(
    override val sessionId: String,
    val distance: Float,
    val faceDistanceThresholdMin: Float,
    val exceedsThresholdMin: Boolean,
    val phase: ChallengePhase
) : SessionEvent {
    // Reported at debug for the same reason as OvalPositionChanged: a single measurement is not
    // interpretable without the session's outcome, so it serves a per-device deep dive rather
    // than field aggregation.
    override val level = LogLevel.DEBUG
}

internal data class ColorSequenceStarted(
    override val sessionId: String
) : SessionEvent {
    override val level = LogLevel.INFO
}

/**
 * The active challenge has finished and the session is waiting on its result. Bounds the period
 * during which the user's face still mattered, separating it from the time spent waiting for the
 * service.
 *
 * [phase] identifies which challenge ran: `COLOR_SEQUENCE` for a colour challenge,
 * `OVAL_FIT` for a movement-only one.
 */
internal data class ChallengeCompleted(
    override val sessionId: String,
    val framesAnalyzed: Int,
    val phase: ChallengePhase
) : SessionEvent {
    override val level = LogLevel.INFO
}
