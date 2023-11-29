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

package com.amplifyframework.ui.liveness.ui.helper

import android.graphics.RectF
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import com.amplifyframework.ui.liveness.camera.LivenessCoordinator

/**
 * @param containerSize The size of the view container that will hold the video preview
 * @param viewportPixelSize The size (in pixels) of the video, once scaled to "fitCenter" within the container
 * @param viewportDpSize The size (in dp) of the video, once scaled to "fitCenter" within the container.
 */
internal data class VideoViewportSize(
    val containerSize: IntSize,
    val viewportPixelSize: IntSize,
    val viewportDpSize: DpSize
) {

    /*
    This method helps creating coordinates for Rects to draw on Canvas (ex: face oval)
    If our video is 360x640, but the viewport is 1080x1920, we must scale the Rect up by 3 (1080/360)
     */
    fun getScaledBoundingRect(boundingRectF: RectF): RectF {
        val scaleRatio = viewportPixelSize.width.toFloat() / LivenessCoordinator.TARGET_WIDTH
        return RectF(
            boundingRectF.left * scaleRatio,
            boundingRectF.top * scaleRatio,
            boundingRectF.right * scaleRatio,
            boundingRectF.bottom * scaleRatio
        )
    }

    companion object {
        fun create(containerSize: IntSize, density: Density): VideoViewportSize {
            val videoContainerAspectRatio = containerSize.width.toFloat() / containerSize.height

            val viewportPixelSize = if (
                videoContainerAspectRatio == LivenessCoordinator.TARGET_ASPECT_RATIO
            ) {
                containerSize
            } else if (videoContainerAspectRatio < LivenessCoordinator.TARGET_ASPECT_RATIO) {
                val adjustedHeight =
                    (containerSize.width * (1 / LivenessCoordinator.TARGET_ASPECT_RATIO)).toInt()
                IntSize(containerSize.width, adjustedHeight)
            } else {
                val adjustedWidth =
                    (containerSize.height * LivenessCoordinator.TARGET_ASPECT_RATIO).toInt()
                IntSize(adjustedWidth, containerSize.height)
            }

            val viewportDpSize = DpSize(
                with(density) { viewportPixelSize.width.toDp() },
                with(density) { viewportPixelSize.height.toDp() }
            )

            return VideoViewportSize(containerSize, viewportPixelSize, viewportDpSize)
        }
    }
}
