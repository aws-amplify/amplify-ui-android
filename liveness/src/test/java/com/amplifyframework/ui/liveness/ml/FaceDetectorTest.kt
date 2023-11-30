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

package com.amplifyframework.ui.liveness.ml

import com.amplifyframework.ui.liveness.camera.LivenessCoordinator
import com.amplifyframework.ui.liveness.ml.FaceDetector.Landmark
import com.amplifyframework.ui.liveness.state.LivenessState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FaceDetectorTest {
    private lateinit var detector: FaceDetector

    // given
    private val faceBottom = 0.88746625f
    private val leftEye = Landmark(0.668633f, 0.48738188f)
    private val rightEye = Landmark(0.35714725f, 0.46644497f)
    private val nose = Landmark(0.52836484f, 0.53194016f)
    private val mouth = Landmark(0.5062596f, 0.68926525f)
    private val leftEar = Landmark(0.78989476f, 0.5973732f)
    private val rightEar = Landmark(0.16585289f, 0.5668279f)
    private val width = 0.65490234f
    private val height = 0.49117205f

    @Before
    fun setup() {
        val state = mockk<LivenessState> {
            every { faceTargetChallenge } returns null
        }
        detector = FaceDetector(state)
    }

    @Test
    fun `test detected face`() {
        val faceDistance = FaceDetector.calculateFaceDistance(leftEye, rightEye, mouth, 1, 1)
        assertApprox(0.31462398f, faceDistance)
        val pupilDistance = FaceDetector.calculatePupilDistance(leftEye, rightEye)
        assertApprox(0.3121886f, pupilDistance)
        val faceHeight = FaceDetector.calculateFaceHeight(leftEye, rightEye, mouth)
        assertApprox(0.21245532f, faceHeight)
    }

    @Test
    fun `test detected bounding box`() {
        // when
        val boundingBox = detector.generateBoundingBoxFromLandmarks(
            faceBottom,
            leftEye,
            rightEye,
            nose,
            mouth,
            leftEar,
            rightEar
        )

        // then
        assertApprox(0.16585289f, boundingBox.left)
        assertApprox(0.07296771f, boundingBox.top)
        assertApprox(0.16585289f + 0.62404186f, boundingBox.right)
        assertApprox(0.07296771f + 0.8144985f, boundingBox.bottom)
    }

    private fun assertApprox(expected: Float, actual: Float, delta: Float = 0.000001f) {
        assertEquals(expected, actual, delta)
    }
}
