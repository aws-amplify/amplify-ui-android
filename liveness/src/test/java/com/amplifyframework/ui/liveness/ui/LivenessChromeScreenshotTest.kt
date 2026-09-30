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

package com.amplifyframework.ui.liveness.ui

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.model.LivenessCheckState
import com.amplifyframework.ui.testing.ComposeTest
import com.amplifyframework.ui.testing.ScreenshotTest
import org.junit.Test

/**
 * Renders the overlay chrome of the liveness component against the backdrops it actually appears
 * over, in both the light and dark default color schemes.
 *
 * The existing `@Preview`s render each element on the preview's own surface, which is why the
 * contrast problems in these elements are invisible there. Each test below stacks the light scheme
 * above the dark scheme so the two can be compared directly.
 */
class LivenessChromeScreenshotTest : ComposeTest() {

    /**
     * Stand-in for the live camera feed: a bright, blown-out highlight through to deep shadow,
     * which is the range the chrome has to stay legible against.
     */
    private val cameraFeed = Brush.verticalGradient(
        listOf(
            Color(0xFFFDFCFA),
            Color(0xFFE8D9CB),
            Color(0xFF8C7B6E),
            Color(0xFF2E2620)
        )
    )

    @Test
    @ScreenshotTest
    fun `connecting and verifying over camera feed`() {
        capture { scheme ->
            // faceGuideRect is null in these states, so FaceGuide draws no scrim and the camera
            // feed is fully visible behind the chip.
            Box(modifier = Modifier.fillMaxSize().background(cameraFeed)) {
                CancelChallengeButton(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {}
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InstructionMessage(LivenessCheckState.Initial.withConnectingMessage())
                    InstructionMessage(LivenessCheckState.Success(RectF()))
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun `check running over white scrim`() {
        capture { scheme ->
            // FaceGuide fills the screen with Color.White and punches out the oval, so all chrome
            // outside the oval sits on white regardless of theme.
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp, 260.dp)
                        .clip(RoundedCornerShape(50))
                        .background(cameraFeed)
                )
                RecordingIndicator(modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
                CancelChallengeButton(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {}
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    InstructionMessage(LivenessCheckState.Running.withMoveFaceMessage())
                    FaceMatchProgressBar(progress = 0.6f, width = 160.dp)
                    InstructionMessage(
                        LivenessCheckState.Running.withFaceOvalPosition(
                            FaceDetector.FaceOvalPosition.TOO_CLOSE
                        )
                    )
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun `start view over themed backdrop`() {
        capture { scheme ->
            // The start view backdrop and the FaceGuide scrim are both colorScheme.background here,
            // so the chip is deliberately flat against it.
            Box(modifier = Modifier.fillMaxSize().background(scheme.background)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp, 260.dp)
                        .clip(RoundedCornerShape(50))
                        .background(cameraFeed)
                )
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InstructionMessage(LivenessCheckState.Initial.withStartViewMessage())
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun `photosensitivity warning banner`() {
        capture { scheme ->
            Box(modifier = Modifier.fillMaxSize().background(scheme.background)) {
                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    PhotosensitivityView {}
                }
            }
        }
    }

    /**
     * [PhotosensitivityAlert] uses a bare [androidx.compose.material3.AlertDialog], so its title and
     * body take their colors from `onSurface` and `onSurfaceVariant` implicitly. This guards against
     * a chrome role reassignment silently changing them. Captured light-only because the dialog
     * renders into its own window and would overlay a paired gallery.
     */
    @Test
    @ScreenshotTest
    fun `photosensitivity dialog in light theme`() {
        setContent {
            MaterialTheme(colorScheme = LivenessColorScheme.Defaults.lightColorScheme) {
                PhotosensitivityAlert {}
            }
        }
    }

    /**
     * The instruction chip, the recording indicator and the cancel button all read `background` and
     * `onBackground`, so restyling the chip restyles the other two as well. This records that
     * coupling: only the chip was meant to change here.
     */
    @Test
    @ScreenshotTest
    fun `restyling the instruction chip also restyles other chrome`() {
        val customised = LivenessColorScheme.Defaults.lightColorScheme.copy(
            background = Color(0xFF303030),
            onBackground = Color(0xFFFFD400)
        )
        captureSchemes(
            "DEFAULT" to LivenessColorScheme.Defaults.lightColorScheme,
            "CHIP RESTYLED TO #303030 / #FFD400" to customised
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                RecordingIndicator(modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
                CancelChallengeButton(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {}
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionMessage(LivenessCheckState.Initial.withConnectingMessage())
                    FaceMatchProgressBar(progress = 0.6f, width = 160.dp)
                }
            }
        }
    }

    /**
     * Renders [content] twice, light scheme above dark, each labelled.
     */
    private fun capture(content: @Composable BoxScope.(ColorScheme) -> Unit) = captureSchemes(
        "LIGHT" to LivenessColorScheme.Defaults.lightColorScheme,
        "DARK" to LivenessColorScheme.Defaults.darkColorScheme,
        content = content
    )

    private fun captureSchemes(
        vararg schemes: Pair<String, ColorScheme>,
        content: @Composable BoxScope.(ColorScheme) -> Unit
    ) = setContent {
        Column(modifier = Modifier.fillMaxWidth()) {
            schemes.forEach { (label, scheme) -> ThemedHalf(label, scheme, content) }
        }
    }

    @Composable
    private fun ThemedHalf(
        label: String,
        scheme: ColorScheme,
        content: @Composable BoxScope.(ColorScheme) -> Unit
    ) {
        Column {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6A1B9A))
                    .padding(4.dp)
            )
            MaterialTheme(colorScheme = scheme) {
                Box(modifier = Modifier.fillMaxWidth().size(420.dp)) {
                    content(scheme)
                }
            }
        }
    }
}
