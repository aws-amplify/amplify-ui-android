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
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import io.kotest.matchers.collections.shouldBeEmpty
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
        val event = FrameBeforeSessionReady

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

        logger.infos.none { it.contains("FaceCountChanged") } shouldBe true
    }

    @Test
    fun `a zero one zero face count sequence logs exactly two changes`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(3) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.01f) }
        repeat(3) { subject.onFrameAnalyzed(faceCount = 1, topScore = 0.93f) }
        repeat(3) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.02f) }

        logger.infos.filter { it.contains("FaceCountChanged") } shouldHaveSize 2
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

        logger.infos.filter { it.contains("FirstOvalMatch") } shouldHaveSize 1
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
            faceDistanceThresholdMin = 0f,
            targetIouThreshold = 0f,
            ovalFitTimeout = 7000
        )

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.emitWatchdogSnapshot()

        logger.warns.last() shouldContain "faceDetectionThreshold=0.7"
    }

    @Test
    fun `the watchdog stays silent once a session error has cancelled it`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.registerGateProvider { gateSnapshot() }

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.onSessionError(
            FaceLivenessDetectionException.VideoEncodingException(),
            stopSession = true,
            closeCode = "RUNTIME_ERROR"
        )
        // Stands in for a watchdog task that had already started running when the timer was
        // cancelled, which cancelling the timer cannot stop.
        subject.emitWatchdogSnapshot()

        logger.warns.filter { it.contains("SessionError") } shouldHaveSize 1
        logger.warns.none { it.contains("FaceDetectionWatchdog") } shouldBe true
    }

    @Test
    fun `an unmapped error is identified by its cause chain rather than its type`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        // The shape an error reported by the service arrives in: the predictions layer wraps it in
        // a message that reads the same for every one of them, so only the innermost cause says
        // what actually happened.
        subject.onSessionError(
            FaceLivenessDetectionException(
                message = "An error occurred during the face liveness flow.",
                throwable = RuntimeException(
                    "An error occurred during the face liveness flow.",
                    IllegalStateException("Session id is not valid")
                )
            ),
            stopSession = false,
            closeCode = "RUNTIME_ERROR"
        )

        val line = logger.warns.last()
        line shouldContain "type=FaceLivenessDetectionException"
        line shouldContain "message=An error occurred during the face liveness flow."
        line shouldContain "RuntimeException: An error occurred during the face liveness flow."
        line shouldContain "IllegalStateException: Session id is not valid"
    }

    @Test
    fun `an error without a cause reports none`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onSessionError(
            FaceLivenessDetectionException.UserCancelledException(),
            stopSession = true,
            closeCode = "CANCELED"
        )

        val line = logger.warns.last()
        line shouldContain "type=UserCancelledException"
        line shouldContain "message=User cancelled the face liveness check."
        line shouldContain "cause=null"
    }

    @Test
    fun `a cause chain that refers back to itself is not followed forever`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        val cyclic = object : RuntimeException("round and round") {
            override val cause: Throwable get() = this
        }

        subject.onSessionError(
            FaceLivenessDetectionException(message = "Boom", throwable = cyclic),
            stopSession = false,
            closeCode = "RUNTIME_ERROR"
        )

        logger.warns.last().split("round and round") shouldHaveSize 6
    }

    @Test
    fun `promoted face events are session events carrying the phase`() {
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
        ovalPosition.level shouldBe LogLevel.DEBUG
        firstMatch.level shouldBe LogLevel.INFO
        faceCount.toString() shouldContain "sessionId=session-1"
        faceCount.toString() shouldContain "phase=OVAL_FIT"
        ovalPosition.toString() shouldContain "phase=COLOR_SEQUENCE"
        firstMatch.toString() shouldContain "sessionId=session-1"
    }

    @Test
    fun `face distance renders the raw value and the closeness threshold`() {
        val event = FaceDistanceChanged(
            sessionId = "session-1",
            distance = 0.31f,
            faceDistanceThresholdMin = 0.9f,
            exceedsThresholdMin = false,
            phase = ChallengePhase.COLOR_SEQUENCE
        )

        event.level shouldBe LogLevel.DEBUG
        val rendered = event.toString()
        rendered shouldContain "distance=0.31"
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

    @Test
    fun `face events are tagged PRE_CHECK before the initial face is detected`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f)

        logger.infos.single { it.contains("FaceCountChanged") } shouldContain "phase=PRE_CHECK"
    }

    @Test
    fun `the phase advances to OVAL_FIT once the initial face is detected`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onInitialFaceDetectedSent()
        repeat(3) { subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR) }

        logger.debugs.single { it.contains("OvalPositionChanged") } shouldContain "phase=OVAL_FIT"
    }

    @Test
    fun `the phase advances to COLOR_SEQUENCE once a colour is displayed`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onInitialFaceDetectedSent()
        subject.onColorDisplayed()
        repeat(3) { subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR) }

        logger.debugs.single { it.contains("OvalPositionChanged") } shouldContain "phase=COLOR_SEQUENCE"
    }

    @Test
    fun `the phase stays at OVAL_FIT when no colour is ever displayed`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onInitialFaceDetectedSent()
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)

        logger.infos.none { it.contains("COLOR_SEQUENCE") } shouldBe true
        logger.infos.none { it.contains("ColorSequenceStarted") } shouldBe true
    }

    @Test
    fun `color sequence started is emitted once across repeated colours`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(8) { subject.onColorDisplayed() }

        logger.infos.filter { it.contains("ColorSequenceStarted") } shouldHaveSize 1
    }

    @Test
    fun `an oval position change is not logged until it has held for three frames`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR)
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR)
        logger.debugs.none { it.contains("OvalPositionChanged") } shouldBe true

        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR)
        logger.debugs.filter { it.contains("OvalPositionChanged") } shouldHaveSize 1
    }

    @Test
    fun `a position flapping on the match boundary is not logged`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        repeat(3) { subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED) }

        // Alternating every frame, as a face resting on the boundary does.
        repeat(6) {
            subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR)
            subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        }

        logger.debugs.filter { it.contains("OvalPositionChanged") } shouldHaveSize 1
    }

    @Test
    fun `every observed position change is counted into the session summary`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(6) {
            subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.TOO_FAR)
            subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        }
        subject.onDestroy("NORMAL")

        // 12 observations alternating from a null start gives 11 changes.
        logger.infos.last() shouldContain "ovalPositionChanges=11"
    }

    @Test
    fun `a session with no face reports no position changes`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onDestroy("NORMAL")

        logger.infos.last() shouldContain "ovalPositionChanges=0"
    }

    @Test
    fun `challenge completed reports the colour phase for a colour challenge`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onInitialFaceDetectedSent()
        subject.onColorDisplayed()
        subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f)
        subject.onChallengeCompleted()

        val line = logger.infos.single { it.contains("ChallengeCompleted") }
        line shouldContain "phase=COLOR_SEQUENCE"
        line shouldContain "framesAnalyzed=1"
        line shouldContain "sessionId=session-1"
    }

    @Test
    fun `challenge completed reports the oval fit phase for a movement only challenge`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onInitialFaceDetectedSent()
        subject.onChallengeCompleted()

        logger.infos.single { it.contains("ChallengeCompleted") } shouldContain "phase=OVAL_FIT"
    }

    @Test
    fun `challenge completed is emitted once`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(5) { subject.onChallengeCompleted() }

        logger.infos.filter { it.contains("ChallengeCompleted") } shouldHaveSize 1
    }

    @Test
    fun `iou statistics report the lowest and mean overlap after the first match`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)

        subject.onFaceIou(0.8f)
        subject.onFaceIou(0.4f)
        subject.onFaceIou(0.6f)
        subject.onDestroy("NORMAL")

        val line = logger.infos.last()
        line shouldContain "challengeMinIou=0.4"
        line shouldContain "challengeMeanIou=0.6"
    }

    @Test
    fun `overlap before the first oval match is excluded`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        // The approach, while the face is still being brought into position.
        subject.onFaceIou(0.05f)
        subject.onFaceIou(0.1f)

        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        subject.onFaceIou(0.7f)
        subject.onDestroy("NORMAL")

        val line = logger.infos.last()
        line shouldContain "challengeMinIou=0.7"
        line shouldContain "challengeMeanIou=0.7"
    }

    @Test
    fun `iou statistics emit nothing per frame`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        logger.infos.clear()

        repeat(100) { subject.onFaceIou(0.5f) }

        logger.infos.shouldBeEmpty()
    }

    @Test
    fun `a session that never matched the oval reports zero overlap not a sentinel`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(5) { subject.onFaceIou(0.2f) }
        subject.onDestroy("NORMAL")

        val line = logger.infos.last()
        line shouldContain "challengeMinIou=0.0"
        line shouldContain "challengeMeanIou=0.0"
    }

    @Test
    fun `iou statistics survive a session that never completed its challenge`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)

        subject.onFaceIou(0.3f)
        subject.onSessionError(
            FaceLivenessDetectionException.VideoEncodingException(),
            stopSession = true,
            closeCode = "RUNTIME_ERROR"
        )
        subject.onDestroy("RUNTIME_ERROR")

        logger.infos.none { it.contains("ChallengeCompleted") } shouldBe true
        logger.infos.last() shouldContain "challengeMinIou=0.3"
    }

    @Test
    fun `challenge frames count from the first match and expose the overlap sample size`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        // Four frames before the match, none of which should be in the challenge window.
        repeat(4) { subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f) }
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        subject.onFaceIou(0.7f)

        // Two further analysed frames, only one of which carries a face.
        subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f)
        subject.onFaceIou(0.6f)
        subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f)
        subject.onDestroy("NORMAL")

        val line = logger.infos.last()
        line shouldContain "challengeFrames=3"
        line shouldContain "challengeFramesWithFace=2"
    }

    @Test
    fun `a face that leaves the frame shows as a gap between frames and frames with face`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f)
        subject.onOvalPositionChanged(FaceDetector.FaceOvalPosition.MATCHED)
        subject.onFaceIou(0.7f)

        // The face is gone for the rest of the challenge, so no overlap is sampled.
        repeat(20) { subject.onFrameAnalyzed(faceCount = 0, topScore = 0.1f) }
        subject.onDestroy("NORMAL")

        val line = logger.infos.last()
        line shouldContain "challengeFrames=21"
        line shouldContain "challengeFramesWithFace=1"
        // The surviving sample still reads high, which is why the counts must accompany it.
        line shouldContain "challengeMeanIou=0.7"
    }

    @Test
    fun `a session that never matched the oval reports no challenge frames`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        repeat(10) { subject.onFrameAnalyzed(faceCount = 1, topScore = 0.9f) }
        subject.onDestroy("NORMAL")

        val line = logger.infos.last()
        line shouldContain "challengeFrames=0"
        line shouldContain "challengeFramesWithFace=0"
    }

    @Test
    fun `the session summary reports why the session ended not how it was disposed`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onSessionError(
            FaceLivenessDetectionException.UserCancelledException(),
            stopSession = true,
            closeCode = "CANCELED"
        )
        // The host removes the detector in response, which disposes the coordinator.
        subject.onDestroy("DISPOSED")

        logger.infos.last() shouldContain "closeCode=CANCELED"
    }

    @Test
    fun `the disposal code is reported when the session ended without an error`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onDestroy("DISPOSED")

        logger.infos.last() shouldContain "closeCode=DISPOSED"
    }

    @Test
    fun `the first error is the one reported when several follow`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onSessionError(
            FaceLivenessDetectionException.FaceInOvalMatchExceededTimeLimitException(),
            true,
            "TIMEOUT"
        )
        subject.onSessionError(FaceLivenessDetectionException.VideoEncodingException(), true, "RUNTIME_ERROR")
        subject.onDestroy("DISPOSED")

        logger.infos.last() shouldContain "closeCode=TIMEOUT"
    }

    @Test
    fun `face distance is emitted on the first observation`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onFaceDistance(0.5f)

        logger.debugs.filter { it.contains("FaceDistanceChanged") } shouldHaveSize 1
    }

    @Test
    fun `face distance inside the hysteresis band is not re-emitted`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onFaceDistance(0.50f)
        subject.onFaceDistance(0.52f)
        subject.onFaceDistance(0.48f)
        subject.onFaceDistance(0.53f)

        logger.debugs.filter { it.contains("FaceDistanceChanged") } shouldHaveSize 1
    }

    @Test
    fun `face distance crossing the hysteresis band is emitted again`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)

        subject.onFaceDistance(0.50f)
        subject.onFaceDistance(0.56f)
        subject.onFaceDistance(0.30f)

        logger.debugs.filter { it.contains("FaceDistanceChanged") } shouldHaveSize 3
    }

    @Test
    fun `face distance reports the thresholds captured at session ready`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.onSessionReady(
            challengeType = "FaceMovementAndLightChallenge",
            challengeId = "challenge-1",
            faceDetectionThreshold = 0.75f,
            faceDistanceThresholdMin = 0.9f,
            targetIouThreshold = 0.75f,
            ovalFitTimeout = 30000
        )

        subject.onFaceDistance(0.31f)

        val line = logger.debugs.single { it.contains("FaceDistanceChanged") }
        line shouldContain "faceDistanceThresholdMin=0.9"
        line shouldContain "exceedsThresholdMin=false"
    }

    @Test
    fun `exceedsThresholdMin is true when the face is closer than the minimum`() {
        val logger = FakeLogger()
        val subject = diagnostics(logger)
        subject.onSessionReady(
            challengeType = "FaceMovementAndLightChallenge",
            challengeId = "challenge-1",
            faceDetectionThreshold = 0.75f,
            faceDistanceThresholdMin = 0.9f,
            targetIouThreshold = 0.75f,
            ovalFitTimeout = 30000
        )

        subject.onFaceDistance(0.95f)

        logger.debugs.single { it.contains("FaceDistanceChanged") } shouldContain
            "exceedsThresholdMin=true"
    }
}
