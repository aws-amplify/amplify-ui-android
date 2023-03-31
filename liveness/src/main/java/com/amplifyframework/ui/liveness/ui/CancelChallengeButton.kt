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

package com.amplifyframework.ui.liveness.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.liveness.R

@Composable
internal fun CancelChallengeButton(
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    CancelChallengeButton(
        modifier = modifier,
        closeContentDescription = stringResource(
            R.string.amplify_ui_liveness_challenge_a11y_cancel_content_description
        ),
        action
    )
}

@Composable
private fun CancelChallengeButton(
    modifier: Modifier = Modifier,
    closeContentDescription: String,
    action: () -> Unit,
) {
    IconButton(
        onClick = action,
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.background, shape = CircleShape)
            .size(44.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = closeContentDescription,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CancelChallengeButtonPreview() {
    LivenessPreviewContainer {
        CancelChallengeButton(closeContentDescription = "") {}
    }
}

@Preview
@Composable
private fun CancelChallengeButtonCustomThemePreview() {
    LivenessPreviewContainer(
        colorScheme = lightColorScheme(
            background = Color.Blue,
            onBackground = Color.Yellow
        )
    ) {
        CancelChallengeButton(closeContentDescription = "") {}
    }
}
