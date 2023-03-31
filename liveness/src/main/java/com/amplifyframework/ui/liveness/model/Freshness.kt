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

package com.amplifyframework.ui.liveness.model

import androidx.compose.ui.graphics.Color
import com.amplifyframework.predictions.aws.models.ColorDisplayInformation
import com.amplifyframework.predictions.aws.models.RgbColor

internal fun RgbColor.toComposeColor(alpha: Int) =
    Color(
        red = red,
        green = green,
        blue = blue,
        alpha = alpha
    )

internal sealed class SceneType {
    object DownScroll : SceneType()
    object Flat : SceneType()
}

internal data class FreshnessColorScene(
    val startTime: Long,
    val endTime: Long,
    val currentColor: ColorDisplayInformation,
    val previousColor: ColorDisplayInformation?,
    val sceneType: SceneType
)

internal data class FreshnessColorFrame(
    val sceneType: SceneType,
    val currentColor: Color,
    val previousColor: Color?,
    val sceneCompletionPercentage: Float
)
