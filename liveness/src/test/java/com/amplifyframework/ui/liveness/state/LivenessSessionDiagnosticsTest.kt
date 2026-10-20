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
import com.amplifyframework.logging.Logger
import com.amplifyframework.ui.liveness.ml.FaceDetector
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test

class LivenessSessionDiagnosticsTest {

    @Test
    fun `session events render sessionId as the first field`() {
        val event = CameraBound(sessionId = "session-1", facing = "front")

        event.toString() shouldBe "CameraBound(sessionId=session-1, facing=front)"
        event.level shouldBe LogLevel.INFO
    }

    @Test
    fun `debug events do not carry a sessionId`() {
        val event = FaceCountChanged(from = 0, to = 1, framesAnalyzed = 42, topScore = 0.93f)

        event.toString() shouldNotContain "sessionId"
        event.level shouldBe LogLevel.DEBUG
    }

    @Test
    fun `AnalyzeFailed keeps the throwable out of its rendered message`() {
        val cause = RuntimeException("copyPixelsFromBuffer boom")

        val event = AnalyzeFailed(sessionId = "session-1", throwable = cause)

        event.toString() shouldBe "AnalyzeFailed(sessionId=session-1)"
        event.toString() shouldNotContain "boom"
        event.throwable shouldBe cause
        event.level shouldBe LogLevel.ERROR
    }

    @Test
    fun `events without a throwable default to null`() {
        CameraBound(sessionId = "session-1", facing = "front").throwable shouldBe null
    }

    @Test
    fun `FrameBeforeSessionReady renders without parentheses`() {
        FrameBeforeSessionReady.toString() shouldBe "FrameBeforeSessionReady"
        FrameBeforeSessionReady.level shouldBe LogLevel.DEBUG
    }

    @Test
    fun `the watchdog event carries every gate flag`() {
        val event = FaceDetectionWatchdog(
            sessionId = "session-1",
            elapsedMs = 10_000,
            framesAnalyzed = 238,
            maxDetectionScore = 0.11f,
            faceDetectionThreshold = 0.70f,
            analyzeErrors = 0,
            showingStartView = false,
            readyForOval = true,
            faceTargetChallenge = "present",
            initialFaceDistanceCheckPassed = false,
            checkState = "Running"
        )

        event.level shouldBe LogLevel.WARN
        val rendered = event.toString()
        rendered shouldContain "sessionId=session-1"
        rendered shouldContain "maxDetectionScore=0.11"
        rendered shouldContain "showingStartView=false"
        rendered shouldContain "readyForOval=true"
        rendered shouldContain "faceTargetChallenge=present"
        rendered shouldContain "initialFaceDistanceCheckPassed=false"
        rendered shouldContain "checkState=Running"
    }

    private class FakeLogger : Logger {
        val infos = mutableListOf<String>()
        val warns = mutableListOf<String>()
        val debugs = mutableListOf<String>()
        val errors = mutableListOf<Pair<String?, Throwable?>>()

        override fun getThresholdLevel() = LogLevel.VERBOSE
        override fun getNamespace() = "Liveness"
        override fun error(message: String?) {
            errors += message to null
        }
        override fun error(message: String?, error: Throwable?) {
            errors += message to error
        }
        override fun warn(message: String?) {
            warns += message.orEmpty()
        }
        override fun warn(message: String?, issue: Throwable?) {
            warns += message.orEmpty()
        }
        override fun info(message: String?) {
            infos += message.orEmpty()
        }
        override fun debug(message: String?) {
            debugs += message.orEmpty()
        }
        override fun verbose(message: String?) = Unit
    }

    private val deviceInfo = DeviceInfo(
        manufacturer = "Google",
        model = "Pixel7",
        sdkInt = 34,
        livenessVersion = "1.12.0"
    )

    private fun diagnostics(logger: Logger) = LivenessSessionDiagnostics(
        sessionId = "session-1",
        deviceInfo = deviceInfo,
        logger = logger,
        clock = { 1_000L }
    )

    private fun gateSnapshot() = GateSnapshot(
        showingStartView = false,
        readyForOval = true,
        faceTargetChallengePresent = true,
        initialFaceDistanceCheckPassed = false,
        checkState = "Running"
    )

    @Test
    fun `every message is prefixed with Liveness`() {
        val logger = FakeLogger()

        diagnostics(logger).onCameraBound("front")

        logger.infos shouldHaveSize 1
        logger.infos[0] shouldBe "Liveness CameraBound(sessionId=session-1, facing=front)"
    }

    @Test
    fun `face count is only logged when it changes`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(100) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.01f) }

        logger.debugs.none { it.contains("FaceCountChanged") } shouldBe true
    }

    @Test
    fun `a zero one zero face count sequence logs exactly two changes`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(3) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.01f) }
        repeat(3) { subject.onFrameAnalyzed(faceCount = 1, topScore = 0.93f) }
        repeat(3) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.02f) }

        logger.debugs.filter { it.contains("FaceCountChanged") } shouldHaveSize 2
    }

    @Test
    fun `oval position is only logged when it changes`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(5) { subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR) }
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        repeat(5) { subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED) }

        logger.debugs.filter { it.contains("OvalPositionChanged") } shouldHaveSize 2
    }

    @Test
    fun `first oval match is logged once`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(5) { subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED) }

        logger.debugs.filter { it.contains("FirstOvalMatch") } shouldHaveSize 1
    }

    @Test
    fun `first frame is logged once and flags a padded row stride`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(3) {
            subject.onFirstFrame(
                width = 640,
                height = 480,
                rowStride = 3000,
                pixelStride = 4,
                rotationDegrees = 0,
                format = "RGBA_8888"
            )
        }

        val lines = logger.infos.filter { it.contains("FirstFrame") }
        lines shouldHaveSize 1
        lines[0] shouldContain "rowStridePadded=true"
        lines[0] shouldContain "expectedRowStride=2560"
        lines[0] shouldContain "resolutionMatchesTarget=false"
        lines[0] shouldContain "frame=640x480"
        lines[0] shouldContain "targetFrame=480x640"
    }

    @Test
    fun `a quarter-turned frame that matches the target after rotation is not a mismatch`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        // A camera reporting a quarter turn delivers the target resolution transposed.
        subject.onFirstFrame(
            width = 640,
            height = 480,
            rowStride = 2560,
            pixelStride = 4,
            rotationDegrees = 270,
            format = "RGBA_8888"
        )

        val line = logger.infos.single { it.contains("FirstFrame") }
        line shouldContain "resolutionMatchesTarget=true"
        line shouldContain "rowStridePadded=false"
    }

    @Test
    fun `frame before session ready is logged once`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(10) { subject.onFrameBeforeSessionReady() }

        logger.debugs.filter { it.contains("FrameBeforeSessionReady") } shouldHaveSize 1
    }

    @Test
    fun `only the first analyze error is logged and the rest are counted`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        val cause = RuntimeException("boom")

        repeat(50) { subject.onAnalyzeError(cause) }
        subject.onDestroy("DISPOSED")

        logger.errors shouldHaveSize 1
        logger.errors[0].first shouldBe "Liveness AnalyzeFailed(sessionId=session-1)"
        logger.errors[0].second shouldBe cause
        logger.infos.last() shouldContain "analyzeErrors=50"
    }

    @Test
    fun `session ended reports accumulated counters`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(4) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f) }
        subject.onFrameAnalyzed(faceCount = 1, topScore = 0.5f)
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        subject.onDestroy("DISPOSED")

        val line = logger.infos.last()
        line shouldContain "framesAnalyzed=5"
        line shouldContain "faceEverDetected=true"
        line shouldContain "ovalEverMatched=true"
        line shouldContain "closeCode=DISPOSED"
    }

    @Test
    fun `max detection score keeps the highest sub-threshold score`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.05f)
        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.68f)
        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.11f)
        subject.emitWatchdogSnapshot()

        logger.warns.last() shouldContain "maxDetectionScore=0.68"
    }

    @Test
    fun `the watchdog does not fire once a face has been detected`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f)
        subject.emitWatchdogSnapshot()

        logger.warns.none { it.contains("FaceDetectionWatchdog") } shouldBe true
    }

    @Test
    fun `the watchdog fires at most once`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        repeat(5) { subject.emitWatchdogSnapshot() }

        logger.warns.filter { it.contains("FaceDetectionWatchdog") } shouldHaveSize 1
    }

    @Test
    fun `the watchdog does not fire after destroy`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.onDestroy("DISPOSED")
        subject.emitWatchdogSnapshot()

        logger.warns.none { it.contains("FaceDetectionWatchdog") } shouldBe true
    }

    @Test
    fun `the watchdog renders gates as unknown when no provider was registered`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.emitWatchdogSnapshot()

        val line = logger.warns.last()
        line shouldContain "faceTargetChallenge=unknown"
        line shouldContain "checkState=unknown"
    }

    @Test
    fun `the watchdog reports the threshold captured at session ready`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }
        subject.onSessionReady(
            challengeType = "FaceMovementAndLightChallenge",
            challengeId = "challenge-1",
            faceDetectionThreshold = 0.7f,
            ovalFitTimeout = 7000
        )

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.emitWatchdogSnapshot()

        logger.warns.last() shouldContain "faceDetectionThreshold=0.7"
    }

    @Test
    fun `session error cancels the watchdog`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.onSessionError("VideoEncodingException", stopSession = true, closeCode = "RUNTIME_ERROR")

        logger.warns.filter { it.contains("SessionError") } shouldHaveSize 1
    }

    @Test
    fun `promoted face events are session events at info level`() {
        val faceCount = FaceCountChanged(
            sessionId = "session-1",
            from = 0,
            to = 1,
            framesAnalyzed = 42,
            topScore = 0.93f,
            phase = ChallengePhase.OVAL_FIT
        )
        val ovalPosition = OvalPositionChanged(
            sessionId = "session-1",
            from = FaceDetector.FaceOvalPosition.MATCHED,
            to = FaceDetector.FaceOvalPosition.TOO_FAR,
            phase = ChallengePhase.COLOR_SEQUENCE
        )
        val firstMatch = FirstOvalMatch(
            sessionId = "session-1",
            framesAnalyzed = 120,
            phase = ChallengePhase.OVAL_FIT
        )

        faceCount.level shouldBe LogLevel.INFO
        ovalPosition.level shouldBe LogLevel.INFO
        firstMatch.level shouldBe LogLevel.INFO
        faceCount.toString() shouldContain "sessionId=session-1"
        faceCount.toString() shouldContain "phase=OVAL_FIT"
        ovalPosition.toString() shouldContain "phase=COLOR_SEQUENCE"
        firstMatch.toString() shouldContain "sessionId=session-1"
    }

    @Test
    fun `face distance renders the raw value and both thresholds`() {
        val event = FaceDistanceChanged(
            sessionId = "session-1",
            distance = 0.31f,
            faceDistanceThreshold = 0.45f,
            faceDistanceThresholdMin = 0.9f,
            exceedsThresholdMin = false,
            phase = ChallengePhase.COLOR_SEQUENCE
        )

        event.level shouldBe LogLevel.INFO
        val rendered = event.toString()
        rendered shouldContain "distance=0.31"
        rendered shouldContain "faceDistanceThreshold=0.45"
        rendered shouldContain "faceDistanceThresholdMin=0.9"
        rendered shouldContain "exceedsThresholdMin=false"
        rendered shouldContain "phase=COLOR_SEQUENCE"
    }

    @Test
    fun `color sequence started is a session event at info level`() {
        val event = ColorSequenceStarted(sessionId = "session-1")

        event.level shouldBe LogLevel.INFO
        event.toString() shouldBe "ColorSequenceStarted(sessionId=session-1)"
    }
}
