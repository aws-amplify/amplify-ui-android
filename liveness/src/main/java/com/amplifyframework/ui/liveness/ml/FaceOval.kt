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

package com.amplifyframework.ui.liveness.ml

import android.graphics.RectF
import com.amplifyframework.predictions.aws.models.FaceTargetChallenge
import kotlin.math.max

internal object FaceOval {

    fun createBoundingRect(
        ovalInfo: FaceTargetChallenge
    ): RectF {
        val left = ovalInfo.targetCenterX - (ovalInfo.targetWidth / 2)
        val top = ovalInfo.targetCenterY - (ovalInfo.targetHeight / 2)
        val right = left + ovalInfo.targetWidth
        val bottom = top + ovalInfo.targetHeight
        return RectF(left, top, right, bottom)
    }

    // Creates a new rectangle that is a mirror of the given rectangle
    fun convertMirroredRectangle(rectangle: RectF, fullViewWidth: Int): RectF {
        val newLeft = max(0f, fullViewWidth - 1 - rectangle.right)
        val newRight = fullViewWidth - 1 - rectangle.left
        val newTop = rectangle.top
        val newBottom = rectangle.bottom
        return RectF(newLeft, newTop, newRight, newBottom)
    }

    fun convertMirroredLandmark(
        landmark: FaceDetector.Landmark,
        fullViewWidth: Int
    ): FaceDetector.Landmark {
        val newX = max(0f, fullViewWidth - 1 - landmark.x)
        val newY = landmark.y
        return FaceDetector.Landmark(newX, newY)
    }
}
