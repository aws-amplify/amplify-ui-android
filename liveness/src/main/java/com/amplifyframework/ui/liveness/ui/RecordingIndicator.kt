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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amplifyframework.ui.liveness.R

@Composable
internal fun RecordingIndicator(modifier: Modifier = Modifier) {
    RecordingIndicator(
        label = stringResource(R.string.amplify_ui_liveness_challenge_recording_indicator_label),
        modifier = modifier
    )
}
@Composable
private fun RecordingIndicator(
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
        modifier = modifier
            .wrapContentSize(Alignment.Center)
            .defaultMinSize(42.dp, 63.dp)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(17.dp)
                .clip(CircleShape)
                .background(Color(0xFFF92626))
        )
        Text(
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            text = label
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecordingIndicatorPreview() {
    LivenessPreviewContainer {
        RecordingIndicator("REC")
    }
}

@Preview
@Composable
private fun RecordingIndicatorLongLabelPreview() {
    LivenessPreviewContainer {
        RecordingIndicator("RECORDING")
    }
}

@Preview
@Composable
private fun RecordingIndicatorCustomThemePreview() {
    LivenessPreviewContainer(
        colorScheme = lightColorScheme(
            background = Color.Blue,
            onBackground = Color.Yellow,
        ),
        typography = Typography(
            labelMedium = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive
            )
        ),
        shapes = Shapes(
            small = RoundedCornerShape(0.dp)
        )
    ) {
        RecordingIndicator("REC")
    }
}
