/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.predictions.aws.models.ColorChallenge
import com.amplifyframework.predictions.aws.models.FaceTargetChallenge
import com.amplifyframework.predictions.aws.models.FaceTargetChallengeResponse
import com.amplifyframework.predictions.aws.models.InitialFaceDetected
import com.amplifyframework.predictions.models.ChallengeResponseEvent
import com.amplifyframework.predictions.models.FaceLivenessChallengeType
import com.amplifyframework.predictions.models.FaceLivenessSession
import com.amplifyframework.predictions.models.FaceLivenessSessionChallenge
import com.amplifyframework.predictions.models.VideoEvent
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.model.LivenessCheckState
import com.amplifyframework.ui.liveness.util.WebSocketCloseCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LivenessStateTest {

    private lateinit var livenessState: LivenessState
    private val onCaptureReady = mockk<() -> Unit>(relaxed = true)
    private val onSessionError =
        mockk<(FaceLivenessDetectionException, Boolean) -> Unit>(relaxed = true)
    private val onFinalEventsSent = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUp() {
        livenessState = LivenessState(
            "1234",
            ApplicationProvider.getApplicationContext(),
            false,
            onCaptureReady,
            onSessionError,
            onFinalEventsSent
        )
        livenessState.onStartViewComplete()
    }

    @Test
    fun `start view blocks state from proceeding`() {
        // given
        val stateWithStartView = LivenessState(
            "1234",
            ApplicationProvider.getApplicationContext(),
            false,
            onCaptureReady,
            onSessionError,
            onFinalEventsSent
        )

        val faceTargetChallenge = mockk<FaceTargetChallenge>(relaxed = true)
        val challenges = listOf<FaceLivenessSessionChallenge>(
            faceTargetChallenge
        )
        val faceLivenessSession = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = { }
        )
        stateWithStartView.onLivenessSessionReady(faceLivenessSession)

        // then
        assertFalse(stateWithStartView.onFrameAvailable())
        stateWithStartView.onFrameFaceUpdate(
            RectF(0f, 0f, 1f, 1f),
            FaceDetector.Landmark(0f, 0f),
            FaceDetector.Landmark(1f, 0f),
            FaceDetector.Landmark(1f, 1f)
        )

        // when
        stateWithStartView.onStartViewComplete()

        // then
        assertTrue(stateWithStartView.onFrameAvailable())
        assertTrue(
            stateWithStartView.onFrameFaceUpdate(
                RectF(0f, 0f, 0f, 0f),
                FaceDetector.Landmark(0f, 0f),
                FaceDetector.Landmark(0f, 0f),
                FaceDetector.Landmark(0f, 0f)
            )
        )
    }

    @Test
    fun `disabling start view immediately starts processing`() {
        // given
        val stateWithoutStartView = LivenessState(
            "1234",
            ApplicationProvider.getApplicationContext(),
            true,
            onCaptureReady,
            onSessionError,
            onFinalEventsSent
        )

        val faceTargetChallenge = mockk<FaceTargetChallenge>(relaxed = true)
        val challenges = listOf<FaceLivenessSessionChallenge>(
            faceTargetChallenge
        )
        val faceLivenessSession = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = { }
        )
        stateWithoutStartView.onLivenessSessionReady(faceLivenessSession)

        // then
        assertTrue(stateWithoutStartView.onFrameAvailable())
        assertTrue(
            stateWithoutStartView.onFrameFaceUpdate(
                RectF(0f, 0f, 0f, 0f),
                FaceDetector.Landmark(0f, 0f),
                FaceDetector.Landmark(0f, 0f),
                FaceDetector.Landmark(0f, 0f)
            )
        )
    }

    @Test
    fun `beginning state is running`() {
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Running)
    }

    @Test
    fun `state is error after on error`() {
        livenessState.onError(true, WebSocketCloseCode.RUNTIME_ERROR)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Error)
    }

    @Test
    fun `session is stopped when stopLivenessSession is true and error occurs`() {
        val challenges = mockk<List<FaceLivenessSessionChallenge>>(relaxed = true)
        val stopSession = mockk<(Int?) -> Unit>(relaxed = true)
        livenessState.livenessSessionInfo = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = stopSession
        )
        livenessState.onError(true, WebSocketCloseCode.RUNTIME_ERROR)
        verify(exactly = 1) { stopSession(WebSocketCloseCode.RUNTIME_ERROR.code) }
    }

    @Test
    fun `proper code is sent when provided in onDestroy`() {
        val challenges = mockk<List<FaceLivenessSessionChallenge>>(relaxed = true)
        val stopSession = mockk<(Int?) -> Unit>(relaxed = true)
        livenessState.livenessSessionInfo = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = stopSession
        )
        livenessState.onDestroy(true, WebSocketCloseCode.DISPOSED)
        verify(exactly = 1) { stopSession(WebSocketCloseCode.DISPOSED.code) }
    }

    @Test
    fun `null close code is sent when no close code provided in onDestroy`() {
        val challenges = mockk<List<FaceLivenessSessionChallenge>>(relaxed = true)
        val stopSession = mockk<(Int?) -> Unit>(relaxed = true)
        livenessState.livenessSessionInfo = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = stopSession
        )
        livenessState.onDestroy(true, null)
        verify(exactly = 1) { stopSession(null) }
    }

    @Test
    fun `session is not stopped when stopLivenessSession is false and error occurs`() {
        val challenges = mockk<List<FaceLivenessSessionChallenge>>(relaxed = true)
        val stopSession = mockk<(Int?) -> Unit>(relaxed = true)
        livenessState.livenessSessionInfo = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = stopSession
        )
        livenessState.onError(false, WebSocketCloseCode.RUNTIME_ERROR)
        verify(exactly = 0) { stopSession(any()) }
    }

    @Test
    fun `face target challenge is retrieved from session info`() {
        val faceTargetChallenge = mockk<FaceTargetChallenge>(relaxed = true)
        val challenges = listOf<FaceLivenessSessionChallenge>(
            faceTargetChallenge
        )
        val faceLivenessSession = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = { }
        )
        livenessState.onLivenessSessionReady(faceLivenessSession)
        assertEquals(faceTargetChallenge, livenessState.faceTargetChallenge)
    }

    @Test
    fun `color challenge is retrieved from session info`() {
        val colorChallenge = mockk<ColorChallenge>(relaxed = true)
        val challenges = listOf<FaceLivenessSessionChallenge>(
            colorChallenge
        )
        val faceLivenessSession = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = { }
        )
        livenessState.onLivenessSessionReady(faceLivenessSession)
        assertEquals(colorChallenge, livenessState.colorChallenge)
    }

    @Test
    fun `challenge runs after retrieving session info`() {
        val faceLivenessSession = mockk<FaceLivenessSession>(relaxed = true)
        livenessState.onLivenessSessionReady(faceLivenessSession)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Running)
        assertTrue(livenessState.readyForOval)
    }

    @Test
    fun `ready to send final events after completing challenges`() {
        livenessState.onFullChallengeComplete()
        assertTrue(livenessState.readyToSendFinalEvents)
    }

    @Test
    fun `liveness challenge execution stops running on challenge completion`() {
        livenessState.onLivenessChallengeComplete()
        assertFalse(livenessState.faceMatched)
    }

    @Test
    fun `state is success after challenge completes and no errors occur`() {
        livenessState.faceGuideRect = mockk(relaxed = true)
        livenessState.onLivenessChallengeComplete()
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Success)
    }

    @Test
    fun `state is error after challenge completes and an error occurs`() {
        livenessState.faceGuideRect = mockk(relaxed = true)
        livenessState.onError(false, WebSocketCloseCode.RUNTIME_ERROR)
        livenessState.onLivenessChallengeComplete()
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Error)
    }

    @Test
    fun `stop processing frames if in error state`() {
        livenessState.livenessCheckState = LivenessCheckState.Error
        assertFalse(livenessState.onFrameAvailable())
    }

    @Test
    fun `keep processing frames if not in success or error state`() {
        livenessState.livenessCheckState = LivenessCheckState.Running.withMoveFaceMessage()
        assertTrue(livenessState.onFrameAvailable())
    }

    @Test
    fun `final events are sent when ready to send final events`() {
        val faceGuideRect = mockk<RectF>(relaxed = true)
        livenessState.livenessCheckState = LivenessCheckState.Success(faceGuideRect)
        livenessState.readyToSendFinalEvents = true
        val challenges = mockk<List<FaceLivenessSessionChallenge>>(relaxed = true)
        val sendVideoEvent = mockk<(VideoEvent) -> Unit>(relaxed = true)
        val sendChallengeResponse = mockk<(ChallengeResponseEvent) -> Unit>(relaxed = true)
        livenessState.livenessSessionInfo = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = sendVideoEvent,
            onChallengeResponseEvent = sendChallengeResponse,
            stopLivenessSession = { }
        )
        livenessState.colorChallenge = mockk(relaxed = true)
        livenessState.faceMatchOvalStart = 0L
        livenessState.faceMatchOvalEnd = 0L

        val frameAvailable = livenessState.onFrameAvailable()
        assertFalse(frameAvailable)
        verify(exactly = 1) { sendChallengeResponse(any<FaceTargetChallengeResponse>()) }
        verify(exactly = 1) { sendVideoEvent(any<VideoEvent>()) }
        verify(exactly = 1) { onFinalEventsSent() }
    }

    @Test
    fun `state is initial if no face found before running`() {
        livenessState.onFrameFaceCountUpdate(0)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Initial)
    }

    @Test
    fun `state is running if no face found during challenges`() {
        livenessState.initialLocalFaceFound = true
        livenessState.livenessCheckState =
            LivenessCheckState.Running.withMultipleFaceMessage()
        livenessState.onFrameFaceCountUpdate(0)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Running)
    }

    @Test
    fun `remember initial face found when face is first detected`() {
        livenessState.onFrameFaceCountUpdate(1)
        assertTrue(livenessState.initialLocalFaceFound)
    }

    @Test
    fun `state is initial if multiple faces detected before running`() {
        livenessState.onFrameFaceCountUpdate(2)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Initial)
    }

    @Test
    fun `state is running if multiple faces found during challenges`() {
        livenessState.initialLocalFaceFound = true
        livenessState.livenessCheckState = LivenessCheckState.Running.withMoveFaceMessage()
        livenessState.onFrameFaceCountUpdate(2)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Running)
    }

    @Test
    fun `state is initial if face distance check fails before running`() {
        val faceTargetChallenge = mockk<FaceTargetChallenge>(relaxed = true)
        val challenges = listOf<FaceLivenessSessionChallenge>(
            faceTargetChallenge
        )
        val faceLivenessSession = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = { }
        )
        livenessState.onLivenessSessionReady(faceLivenessSession)

        val faceRect = RectF(20f, 20f, 100f, 100f)
        val leftEye = FaceDetector.Landmark(25f, 40f)
        val rightEye = FaceDetector.Landmark(75f, 40f)
        val mouth = FaceDetector.Landmark(40f, 80f)
        livenessState.onFrameFaceUpdate(faceRect, leftEye, rightEye, mouth)
        assertTrue(livenessState.livenessCheckState is LivenessCheckState.Running)
    }

    @Test
    fun `face distance check passes when face is far enough away before running`() {
        val faceTargetChallenge = mockk<FaceTargetChallenge>(relaxed = true)
        val challenges = listOf<FaceLivenessSessionChallenge>(
            faceTargetChallenge
        )

        every { faceTargetChallenge.faceTargetMatching.faceDistanceThresholdMin } returns 1f

        val faceLivenessSession = FaceLivenessSession(
            challengeId = "12345",
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            challenges = challenges,
            onVideoEvent = { },
            onChallengeResponseEvent = { },
            stopLivenessSession = { }
        )
        livenessState.onLivenessSessionReady(faceLivenessSession)

        val faceRect = RectF(20f, 20f, 100f, 100f)
        val leftEye = FaceDetector.Landmark(25f, 40f)
        val rightEye = FaceDetector.Landmark(75f, 40f)
        val mouth = FaceDetector.Landmark(40f, 80f)
        livenessState.onFrameFaceUpdate(faceRect, leftEye, rightEye, mouth)
        assertTrue(livenessState.initialFaceDistanceCheckPassed)
    }

    @Test
    fun `start streaming video when ready to show oval`() {
        livenessState.initialFaceDistanceCheckPassed = true
        livenessState.readyForOval = true
        livenessState.livenessSessionInfo = mockk(relaxed = true)
        livenessState.colorChallenge = mockk(relaxed = true)
        livenessState.faceTargetChallenge = mockk(relaxed = true)
        val faceRect = mockk<RectF>(relaxed = true)
        val landmark = mockk<FaceDetector.Landmark>(relaxed = true)
        livenessState.onFrameFaceUpdate(faceRect, landmark, landmark, landmark)
        verify(exactly = 1) { onCaptureReady() }
    }

    @Test
    fun `send initial face detected event when detecting face after checks pass`() {
        livenessState.initialFaceDistanceCheckPassed = true
        livenessState.readyForOval = true
        val challenges = mockk<List<FaceLivenessSessionChallenge>>(relaxed = true)
        val sendVideoEvent = mockk<(VideoEvent) -> Unit>(relaxed = true)
        val sendChallengeResponse = mockk<(ChallengeResponseEvent) -> Unit>(relaxed = true)
        livenessState.livenessSessionInfo =
            FaceLivenessSession(
                challengeId = "12345",
                challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
                challenges = challenges,
                onVideoEvent = sendVideoEvent,
                onChallengeResponseEvent = sendChallengeResponse,
                stopLivenessSession = { }
            )
        livenessState.colorChallenge = mockk(relaxed = true)
        livenessState.faceTargetChallenge = mockk(relaxed = true)

        val faceRect = mockk<RectF>(relaxed = true)
        val landmark = mockk<FaceDetector.Landmark>(relaxed = true)
        livenessState.onFrameFaceUpdate(faceRect, landmark, landmark, landmark)
        verify(exactly = 1) { sendChallengeResponse(any<InitialFaceDetected>()) }
    }
}
