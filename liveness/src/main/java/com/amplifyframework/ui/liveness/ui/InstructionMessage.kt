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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.model.LivenessCheckState

@Composable internal fun InstructionMessage(
    livenessCheckState: LivenessCheckState,
    isFaceOvalInstruction: Boolean = false
) {
    val instructionText = livenessCheckState.instructionId?.let { stringResource(it) } ?: return
    val showProgress = livenessCheckState is LivenessCheckState.Success
    if (isFaceOvalInstruction) {
        FaceOvalInstructionMessage(message = instructionText)
    } else {
        InstructionMessage(message = instructionText, showProgress = showProgress)
    }
}
@Composable
private fun InstructionMessage(
    message: String,
    showProgress: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            message,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FaceOvalInstructionMessage(
    message: String
) {
    val backgroundColor = if (
        message == stringResource(FaceDetector.FaceOvalPosition.TOO_CLOSE.instructionStringRes)
    ) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val textColor = if (
        message == stringResource(FaceDetector.FaceOvalPosition.TOO_CLOSE.instructionStringRes)
    ) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            message,
            color = textColor,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun InstructionMessagePreview() {
    LivenessPreviewContainer {
        InstructionMessage("Success", true)
    }
}
@Preview
@Composable
private fun InstructionMessageProgressPreview() {
    LivenessPreviewContainer {
        InstructionMessage("Success", true)
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun FaceOvalInstructionMessagePreview() {
    LivenessPreviewContainer {
        FaceOvalInstructionMessage("Move closer")
    }
}

@Preview
@Composable
private fun InstructionMessageMultiLinePreview() {
    LivenessPreviewContainer {
        InstructionMessage(
            message = "Instruction message \n multiline",
            showProgress = false
        )
    }
}

@Preview
@Composable
private fun InstructionMessageCustomThemePreview() {
    LivenessPreviewContainer(
        colorScheme = lightColorScheme(
            primary = Color.White,
            background = Color.Blue,
            onBackground = Color.Yellow,
        ),
        typography = Typography(
            bodyMedium = TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Cursive
            )
        )
    ) {
        InstructionMessage("Success", true)
    }
}
