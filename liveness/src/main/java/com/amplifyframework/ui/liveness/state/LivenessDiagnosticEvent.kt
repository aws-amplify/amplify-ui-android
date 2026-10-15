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
    val from: Int,
    val to: Int,
    val framesAnalyzed: Int,
    val topScore: Float
) : LivenessDiagnosticEvent {
    override val level = LogLevel.DEBUG
}

internal data class OvalPositionChanged(
    val from: FaceDetector.FaceOvalPosition?,
    val to: FaceDetector.FaceOvalPosition
) : LivenessDiagnosticEvent {
    override val level = LogLevel.DEBUG
}

internal data class FirstOvalMatch(
    val framesAnalyzed: Int
) : LivenessDiagnosticEvent {
    override val level = LogLevel.DEBUG
}
