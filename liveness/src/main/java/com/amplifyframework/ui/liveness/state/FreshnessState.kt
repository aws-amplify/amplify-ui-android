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

import com.amplifyframework.predictions.aws.models.ColorDisplayInformation
import com.amplifyframework.predictions.aws.models.RgbColor
import com.amplifyframework.ui.liveness.model.FreshnessColorFrame
import com.amplifyframework.ui.liveness.model.FreshnessColorScene
import com.amplifyframework.ui.liveness.model.SceneType
import com.amplifyframework.ui.liveness.model.toComposeColor

internal typealias OnColorDisplayed = (
    currentColor: RgbColor,
    previousColor: RgbColor,
    sequenceNumber: Int,
    colorStartTime: Long
) -> Unit

internal data class FreshnessState(
    val freshnessColors: List<ColorDisplayInformation>,
    val onColorDisplayed: OnColorDisplayed,
    val onComplete: () -> Unit
) {

    private val freshnessColorScript: List<FreshnessColorScene>

    private var playbackStarted = -1L
    private var currentSceneIndex = 0
    private var lastDisplayedSceneIndex = -1 // used to track onColorDisplayed callback
    var playbackEnded = false

    fun nextFrame(currentTime: Long): FreshnessColorFrame? {
        if (playbackEnded) return null

        if (playbackStarted == -1L) {
            // start playback if not yet started
            playbackStarted = currentTime
        }

        // get current relative start time for player
        val playbackTime = currentTime - playbackStarted

        while (playbackTime > freshnessColorScript[currentSceneIndex].endTime) {
            // increase scene index until finding non-expired scene
            currentSceneIndex += 1

            // End Playback if all colors have expired
            if (currentSceneIndex >= freshnessColors.size) {
                playbackEnded = true
                onComplete()
                return null
            }
        }

        return freshnessColorScript[currentSceneIndex].let {
            val scenePlaybackTime = playbackTime - it.startTime
            val sceneDuration = it.endTime - it.startTime
            val sceneCompletionPercentage = scenePlaybackTime.toFloat() / sceneDuration

            val currentFreshnessColor = it.currentColor.color
            val previousFreshnessColor = it.previousColor?.color

            if (lastDisplayedSceneIndex != currentSceneIndex) {
                onColorDisplayed(
                    currentFreshnessColor,
                    previousFreshnessColor ?: currentFreshnessColor,
                    currentSceneIndex,
                    currentTime
                )
                lastDisplayedSceneIndex = currentSceneIndex
            }

            FreshnessColorFrame(
                sceneType = it.sceneType,
                currentColor = currentFreshnessColor.toComposeColor(
                    if (currentSceneIndex == 0) FIRST_SCENE_ALPHA else REMAINING_SCENE_ALPHA
                ),
                previousColor = previousFreshnessColor?.toComposeColor(
                    if (currentSceneIndex == 1) FIRST_SCENE_ALPHA else REMAINING_SCENE_ALPHA
                ),
                sceneCompletionPercentage = if (it.sceneType is SceneType.DownScroll) {
                    sceneCompletionPercentage
                } else {
                    100f
                }
            )
        }
    }

    init {
        var accumulator = 0L
        freshnessColorScript = freshnessColors.mapIndexed { index, color ->
            val sceneType = if (color.shouldScroll) SceneType.DownScroll else SceneType.Flat
            val startTime = accumulator
            val duration = color.duration.toLong()
            val endTime = startTime + duration
            FreshnessColorScene(
                startTime = startTime,
                endTime = endTime,
                currentColor = color,
                previousColor = freshnessColors.getOrNull(index - 1),
                sceneType = sceneType
            ).also {
                accumulator = endTime
            }
        }
    }

    companion object {
        const val FIRST_SCENE_ALPHA = (255 * .90).toInt()
        const val REMAINING_SCENE_ALPHA = (255f * .75).toInt()
    }
}
