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

import androidx.compose.ui.graphics.Color
import com.amplifyframework.predictions.aws.models.ColorDisplayInformation
import com.amplifyframework.predictions.aws.models.RgbColor
import com.amplifyframework.ui.liveness.model.FreshnessColorFrame
import com.amplifyframework.ui.liveness.model.SceneType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FreshnessStateTest {

    @Test
    fun `test playback frame transitions`() {
        // Given
        val expectedFrame1 = FreshnessColorFrame(
            sceneType = SceneType.Flat,
            currentColor = Color(5, 10, 15, FreshnessState.FIRST_SCENE_ALPHA),
            previousColor = null,
            sceneCompletionPercentage = 100f
        )
        val expectedFrame2 = FreshnessColorFrame(
            sceneType = SceneType.DownScroll,
            currentColor = Color(20, 25, 30, FreshnessState.REMAINING_SCENE_ALPHA),
            previousColor = Color(5, 10, 15, FreshnessState.FIRST_SCENE_ALPHA),
            sceneCompletionPercentage = 5.0E-4f
        )
        val expectedFrame3 = FreshnessColorFrame(
            sceneType = SceneType.DownScroll,
            currentColor = Color(20, 25, 30, FreshnessState.REMAINING_SCENE_ALPHA),
            previousColor = Color(5, 10, 15, FreshnessState.FIRST_SCENE_ALPHA),
            sceneCompletionPercentage = .5005f
        )
        val expectedFrame4 = FreshnessColorFrame(
            sceneType = SceneType.Flat,
            currentColor = Color(35, 40, 45, FreshnessState.REMAINING_SCENE_ALPHA),
            previousColor = Color(20, 25, 30, FreshnessState.REMAINING_SCENE_ALPHA),
            sceneCompletionPercentage = 100f
        )
        val expectedFrame5 = null

        val rgbColor1 = RgbColor(5, 10, 15)
        val color1 = mockk<ColorDisplayInformation> {
            every { color } returns rgbColor1
            every { duration } returns 1000.0f
            every { shouldScroll } returns false
        }
        val rgbColor2 = RgbColor(20, 25, 30)
        val color2 = mockk<ColorDisplayInformation> {
            every { color } returns rgbColor2
            every { duration } returns 2000.0f
            every { shouldScroll } returns true
        }
        val rgbColor3 = RgbColor(35, 40, 45)
        val color3 = mockk<ColorDisplayInformation> {
            every { color } returns rgbColor3
            every { duration } returns 500f
            every { shouldScroll } returns false
        }
        val freshnessColors = listOf(color1, color2, color3)
        val onColorDisplayed = mockk<OnColorDisplayed>(relaxed = true)
        val onComplete = mockk<() -> Unit>(relaxed = true)
        val state = FreshnessState(freshnessColors, onColorDisplayed, onComplete)

        // When
        val frame0 = state.nextFrame(0)

        // Then
        assertEquals(expectedFrame1, frame0)
        verify { onColorDisplayed(rgbColor1, rgbColor1, 0, 0) }
        verify(exactly = 1) { onColorDisplayed(any(), any(), any(), any()) }
        verify(exactly = 0) { onComplete() }

        // When
        val frame1 = state.nextFrame(100)

        // Then
        assertEquals(expectedFrame1, frame1)
        verify(exactly = 1) { onColorDisplayed(any(), any(), any(), any()) }
        verify(exactly = 0) { onComplete() }

        // When
        val frame2 = state.nextFrame(1001)

        // Then
        assertEquals(expectedFrame2, frame2)
        verify { onColorDisplayed(rgbColor2, rgbColor1, 1, 1001) }
        verify(exactly = 2) { onColorDisplayed(any(), any(), any(), any()) }
        verify(exactly = 0) { onComplete() }

        // When
        val frame3 = state.nextFrame(2001)

        // Then
        assertEquals(expectedFrame3, frame3)
        verify(exactly = 2) { onColorDisplayed(any(), any(), any(), any()) }
        verify(exactly = 0) { onComplete() }

        // When
        val frame4 = state.nextFrame(3001)

        // Then
        assertEquals(expectedFrame4, frame4)
        verify { onColorDisplayed(rgbColor3, rgbColor2, 2, 3001) }
        verify(exactly = 3) { onColorDisplayed(any(), any(), any(), any()) }
        verify(exactly = 0) { onComplete() }

        // When
        val frame5 = state.nextFrame(3501)

        // Then
        assertEquals(expectedFrame5, frame5)
        verify(exactly = 3) { onColorDisplayed(any(), any(), any(), any()) }
        verify(exactly = 1) { onComplete() }
    }
}
