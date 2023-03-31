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

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.liveness.ui.helper.VideoViewportSize

@Composable
internal fun FaceGuide(
    modifier: Modifier,
    faceGuideRect: RectF?,
    videoViewportSize: VideoViewportSize
) {

    val scaledBoundingRect = faceGuideRect?.let {
        videoViewportSize.getScaledBoundingRect(it)
    } ?: return

    Canvas(modifier.graphicsLayer(alpha = 0.99f)) {

        drawRect(
            color = Color.White,
            size = size
        )

        // Calculate topLeft of viewport for centering inside full sized canvas
        val viewportTopLeft = Offset(
            x = (size.width - videoViewportSize.viewportPixelSize.width) / 2,
            y = (size.height - videoViewportSize.viewportPixelSize.height) / 2
        )

        // Calculate oval topLeft, taking into viewport topLeft
        val ovalTopLeft = Offset(
            x = viewportTopLeft.x + scaledBoundingRect.left,
            y = viewportTopLeft.y + scaledBoundingRect.top
        )

        val ovalSize = Size(
            width = scaledBoundingRect.right - scaledBoundingRect.left,
            height = scaledBoundingRect.bottom - scaledBoundingRect.top
        )

        // Draw oval stroke
        drawOval(
            color = Color(0xFFAEB3B7),
            style = Stroke(4.dp.toPx()),
            topLeft = ovalTopLeft,
            size = ovalSize
        )

        // Cut out oval, removing the drawn freshness and overlay color
        drawOval(
            color = Color.Transparent,
            style = Fill,
            topLeft = ovalTopLeft,
            size = ovalSize,
            blendMode = BlendMode.SrcOut
        )
    }
}

@Preview
@Composable
internal fun FaceGuidePreview() {
    Box(Modifier.size(1080.dp, 1920.dp).background(color = Color.Red)) {
        FaceGuide(
            modifier = Modifier.fillMaxSize(),
            faceGuideRect = RectF(50f, 50f, 200f, 400f),
            videoViewportSize = VideoViewportSize(
                IntSize(1080, 2100),
                IntSize(1080, 1920),
                DpSize(1080.dp, 1920.dp)
            )
        )
    }
}
