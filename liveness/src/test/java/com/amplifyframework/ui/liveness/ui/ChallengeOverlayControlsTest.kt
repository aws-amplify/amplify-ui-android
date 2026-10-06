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

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.ui.liveness.R
import com.amplifyframework.ui.testing.ComposeTest
import io.kotest.matchers.shouldBe
import org.junit.Test

class ChallengeOverlayControlsTest : ComposeTest() {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cancelDescription =
        context.getString(R.string.amplify_ui_liveness_challenge_a11y_cancel_content_description)
    private val recordingLabel =
        context.getString(R.string.amplify_ui_liveness_challenge_recording_indicator_label)

    @Test
    fun `cancel button is shown by default`() {
        setControls(hideCancelButton = false)

        composeTestRule.onNodeWithContentDescription(cancelDescription).assertIsDisplayed()
    }

    @Test
    fun `clicking cancel button invokes onCancel`() {
        var cancelCount = 0
        setControls(hideCancelButton = false) { cancelCount++ }

        composeTestRule.onNodeWithContentDescription(cancelDescription).performClick()

        cancelCount shouldBe 1
    }

    @Test
    fun `cancel button is not shown when hideCancelButton is true`() {
        setControls(hideCancelButton = true)

        composeTestRule.onNodeWithContentDescription(cancelDescription).assertDoesNotExist()
    }

    @Test
    fun `recording indicator is still shown when cancel button is hidden`() {
        setControls(showRecordingIndicator = true, hideCancelButton = true)

        composeTestRule.onNodeWithText(recordingLabel).assertIsDisplayed()
    }

    @Test
    fun `recording indicator is not shown before the face guide is shown`() {
        setControls(showRecordingIndicator = false, hideCancelButton = false)

        composeTestRule.onNodeWithText(recordingLabel).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(cancelDescription).assertIsDisplayed()
    }

    private fun setControls(
        showRecordingIndicator: Boolean = true,
        hideCancelButton: Boolean,
        onCancel: () -> Unit = {}
    ) = setContent {
        Box(modifier = Modifier.fillMaxSize()) {
            ChallengeOverlayControls(
                showRecordingIndicator = showRecordingIndicator,
                hideCancelButton = hideCancelButton,
                onCancel = onCancel
            )
        }
    }
}
