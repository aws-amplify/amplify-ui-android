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

// we need access to internal classes; this cannot be in the same package unfortunately.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.amplifyframework.ui.liveness.ui

import android.content.Context
import android.graphics.RectF
import androidx.compose.ui.platform.LocalContext
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin
import com.amplifyframework.predictions.models.FaceLivenessSession
import com.amplifyframework.ui.liveness.ScreenshotTestBase
import com.amplifyframework.ui.liveness.camera.FrameAnalyzer
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.region
import com.amplifyframework.ui.liveness.sessionId
import com.amplifyframework.ui.liveness.state.LivenessState
import io.mockk.CapturingSlot
import io.mockk.InvokeMatcher
import io.mockk.OfTypeMatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import org.tensorflow.lite.Interpreter

@OptIn(InternalAmplifyApi::class)
class FaceLivenessDetectorScreenshot : ScreenshotTestBase() {
    private var livenessState: LivenessState? = null
//    private lateinit var onSessionStarted: CapturingSlot<Consumer<FaceLivenessSession>>
    @Before
    fun setup() {
        mockkConstructor(FrameAnalyzer::class)
        every {
            constructedWith<FrameAnalyzer>(
                OfTypeMatcher<Context>(Context::class),
                InvokeMatcher<LivenessState> {
                    livenessState = it
                },
            ).analyze(any())
        } answers {
            livenessState?.onFrameFaceCountUpdate(1)

            // Features too far apart, this face must be too close to the camera
            livenessState?.onFrameFaceUpdate(
                RectF(0f, 0f, 400f, 400f),
                FaceDetector.Landmark(120f, 120f),
                FaceDetector.Landmark(280f, 120f),
                FaceDetector.Landmark(200f, 320f),
            )
        }

        spyk
        mockkObject(FaceDetector)
        every { FaceDetector.loadModel(any()) } returns mockk()

//        mockkStatic(AWSPredictionsPlugin::class)
//        every {
//            AWSPredictionsPlugin.startFaceLivenessSession(
//                any(), // sessionId
//                any(),
////                capture(livenessSessionInformation), // sessionInformation
//                any(),
////                capture(livenessSessionOptions), // options
////                capture(onSessionStarted), // onSessionStarted
//                any(),
////                capture(onLivenessComplete), // onComplete
//                any(),
//                any(), // onError
//            )
//        } just Runs
    }

    @Test
    fun default_state() {
        screenshot {
            val context = LocalContext.current
//            context.assets.openFd
            context
            ChallengeView(key = "key", sessionId, region, credentialsProvider = null, onChallengeComplete = {}, onChallengeFailed = {})
//            FaceLivenessDetector(sessionId, region, onComplete = {}, onError = {})
//            onSessionStarted.captured.accept(FaceLivenessSession(emptyList(), {}, {}, {}))

            // 1. mock out frameanalyzer.analyze
            // 2. facedetector.loadModel returns mock
        }
    }
}
