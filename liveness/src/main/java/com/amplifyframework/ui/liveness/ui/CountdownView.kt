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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

@Composable
internal fun CountdownView(
    modifier: Modifier = Modifier,
    key: Any? = null,
    durationInSeconds: Float,
    onComplete: () -> Unit
) {
    val currentOnComplete by rememberUpdatedState(onComplete)
    val countdownCompleted = remember(key1 = key, key2 = durationInSeconds) {
        mutableStateOf(false)
    }

    if (countdownCompleted.value) {
        return
    }

    val startTime = remember(key1 = key, key2 = durationInSeconds) { System.currentTimeMillis() }
    val durationInMillis = durationInSeconds * 1000
    val millisSinceStart = nextFrameMillis().value - startTime

    if (millisSinceStart >= durationInMillis) {
        LaunchedEffect(key) {
            countdownCompleted.value = true
            currentOnComplete()
        }
    }

    CountdownView(
        modifier = modifier,
        secondsSinceStart = millisSinceStart / 1000F,
        durationInSeconds = durationInSeconds
    )
}

@Composable
private fun CountdownView(
    modifier: Modifier = Modifier,
    secondsSinceStart: Float,
    durationInSeconds: Float
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.wrapContentSize()) {
        val countdownTextLineHeight: Dp = with(LocalDensity.current) {
            val lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
            val fontSize = MaterialTheme.typography.headlineMedium.fontSize

            if (lineHeight.isSp) {
                MaterialTheme.typography.headlineMedium.lineHeight.toDp()
            } else if (fontSize.isSp) {
                // Custom typography likely provided, but fontSize was provided without a lineHeight
                fontSize.toDp() + 8.dp
            } else {
                // unable to measure provided font size. Default to standard headline2 lineHeight
                36.dp
            }
        }

        CircularProgressIndicator(
            progress = 1 - (secondsSinceStart / durationInSeconds),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = CircleShape
                )
                .padding(4.dp)
                .size(countdownTextLineHeight + 20.dp)
        )

        Text(
            text = ceil(durationInSeconds - secondsSinceStart).toInt().toString(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CountdownViewPreview() {
    LivenessPreviewContainer {
        CountdownView(secondsSinceStart = 1f, durationInSeconds = 2f)
    }
}

@Preview
@Composable
private fun CountdownCustomThemePreview() {
    LivenessPreviewContainer(
        colorScheme = lightColorScheme(
            background = Color.Blue,
            onBackground = Color.Yellow,
            primary = Color.Green
        ),
        typography = Typography(
            headlineMedium = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive
            )
        )
    ) {
        CountdownView(secondsSinceStart = 1f, durationInSeconds = 2f)
    }
}
