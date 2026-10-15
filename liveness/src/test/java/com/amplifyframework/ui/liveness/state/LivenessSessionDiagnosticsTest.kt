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
}
