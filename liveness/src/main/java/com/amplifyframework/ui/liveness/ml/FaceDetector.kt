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

import android.content.Context
import android.graphics.RectF
import com.amplifyframework.predictions.aws.models.FaceTargetMatchingParameters
import com.amplifyframework.ui.liveness.R
import com.amplifyframework.ui.liveness.camera.LivenessCoordinator.Companion.TARGET_HEIGHT
import com.amplifyframework.ui.liveness.camera.LivenessCoordinator.Companion.TARGET_WIDTH
import com.amplifyframework.ui.liveness.state.LivenessState
import java.io.FileInputStream
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import org.tensorflow.lite.Interpreter

internal class FaceDetector(private val livenessState: LivenessState) {
    private val anchors = generateAnchors()

    fun getBoundingBoxes(
        outputBoxes: Array<Array<FloatArray>>,
        outputScores: Array<Array<FloatArray>>
    ): List<Detection> {
        val detections = mutableListOf<Detection>()
        for (i in 0 until NUM_BOXES) {
            var score = outputScores[0][i][0]
            score = computeSigmoid(score)
            if (score < MIN_SCORE_THRESHOLD) {
                continue
            }

            var xCenter = outputBoxes[0][i][0]
            var yCenter = outputBoxes[0][i][1]
            var w = outputBoxes[0][i][2]
            var h = outputBoxes[0][i][3]

            var leftEyeX = outputBoxes[0][i][4]
            var leftEyeY = outputBoxes[0][i][5]
            var rightEyeX = outputBoxes[0][i][6]
            var rightEyeY = outputBoxes[0][i][7]

            var noseX = outputBoxes[0][i][8]
            var noseY = outputBoxes[0][i][9]

            var mouthX = outputBoxes[0][i][10]
            var mouthY = outputBoxes[0][i][11]

            xCenter = xCenter / X_SCALE * anchors[i].w + anchors[i].xCenter
            yCenter = yCenter / Y_SCALE * anchors[i].h + anchors[i].yCenter
            h = h / H_SCALE * anchors[i].h
            w = w / W_SCALE * anchors[i].w

            val yMin = yCenter - h / 2.0f
            val xMin = xCenter - w / 2.0f
            val yMax = yCenter + h / 2.0f
            val xMax = xCenter + w / 2.0f

            leftEyeX = leftEyeX / X_SCALE * anchors[i].w + anchors[i].xCenter
            leftEyeY = leftEyeY / Y_SCALE * anchors[i].h + anchors[i].yCenter
            rightEyeX = rightEyeX / X_SCALE * anchors[i].w + anchors[i].xCenter
            rightEyeY = rightEyeY / Y_SCALE * anchors[i].h + anchors[i].yCenter

            noseX = noseX / X_SCALE * anchors[i].w + anchors[i].xCenter
            noseY = noseY / Y_SCALE * anchors[i].h + anchors[i].yCenter

            mouthX = mouthX / X_SCALE * anchors[i].w + anchors[i].xCenter
            mouthY = mouthY / Y_SCALE * anchors[i].h + anchors[i].yCenter

            detections.add(
                Detection(
                    RectF(xMin, yMin, xMax, yMax),
                    Landmark(leftEyeX, leftEyeY),
                    Landmark(rightEyeX, rightEyeY),
                    Landmark(noseX, noseY),
                    Landmark(mouthX, mouthY),
                    score
                )
            )
        }
        // Check if there are any detections
        if (detections.isEmpty()) {
            return emptyList()
        }
        // Sort the detections according by score
        val indexedScores = mutableListOf<IndexedScore>()
        for (index in 0 until detections.size) {
            indexedScores.add(IndexedScore(index, detections[index].score))
        }
        val sortedScores = indexedScores.sortedBy { it.score }

        val weightedDetections = weightedNonMaxSuppression(sortedScores, detections)
        val renormalizedDetections = mutableListOf<Detection>()
        weightedDetections.forEach { detection ->
            // Change landmark coordinates to be for actual image size instead of model input size
            val scaledLeftEyeX = (detection.leftEye.x / X_SCALE) * TARGET_WIDTH
            val scaledLeftEyeY = (detection.leftEye.y / Y_SCALE) * TARGET_HEIGHT
            val scaledRightEyeX = (detection.rightEye.x / X_SCALE) * TARGET_WIDTH
            val scaledRightEyeY = (detection.rightEye.y / Y_SCALE) * TARGET_HEIGHT
            val scaledNoseX = (detection.nose.x / X_SCALE) * TARGET_WIDTH
            val scaledNoseY = (detection.nose.y / Y_SCALE) * TARGET_HEIGHT
            val scaledMouthX = (detection.mouth.x / X_SCALE) * TARGET_WIDTH
            val scaledMouthY = (detection.mouth.y / Y_SCALE) * TARGET_HEIGHT

            val scaledLeftEye = Landmark(scaledLeftEyeX, scaledLeftEyeY)
            val scaledRightEye = Landmark(scaledRightEyeX, scaledRightEyeY)
            val scaledNose = Landmark(scaledNoseX, scaledNoseY)
            val scaledMouth = Landmark(scaledMouthX, scaledMouthY)

            // Generate the face bounding box from the landmarks
            val renormalizedBoundingBox =
                generateBoundingBoxFromLandmarks(
                    scaledLeftEye,
                    scaledRightEye,
                    scaledNose,
                    scaledMouth
                )
            renormalizedDetections.add(
                Detection(
                    renormalizedBoundingBox,
                    scaledLeftEye,
                    scaledRightEye,
                    scaledNose,
                    scaledMouth,
                    detection.score
                )
            )
        }
        return renormalizedDetections
    }

    private fun generateBoundingBoxFromLandmarks(
        leftEye: Landmark,
        rightEye: Landmark,
        nose: Landmark,
        mouth: Landmark
    ): RectF {
        val pupilDistance = calculatePupilDistance(leftEye, rightEye)
        val faceHeight = calculateFaceHeight(leftEye, rightEye, mouth)

        val ow = (ALPHA * pupilDistance + GAMMA * faceHeight) / 2
        val oh = GOLDEN_RATIO * ow

        val eyeCenterX = (leftEye.x + rightEye.x) / 2
        val eyeCenterY = (leftEye.y + rightEye.y) / 2

        var cx = eyeCenterX
        var cy = eyeCenterY
        val ovalInfo = livenessState.faceTargetChallenge
        if (ovalInfo != null && eyeCenterY <= ovalInfo.targetCenterY / 2) {
            cx = (eyeCenterX + nose.x) / 2
            cy = (eyeCenterY + nose.y) / 2
        }

        val left = cx - ow / 2
        val top = cy - oh / 2
        val right = left + ow
        val bottom = top + oh
        return RectF(left, top, right, bottom)
    }

    private fun generateAnchors(): List<Anchor> {
        val newAnchors = mutableListOf<Anchor>()
        var layerId = 0
        while (layerId < strides.size) {
            val anchorHeight = mutableListOf<Float>()
            val anchorWidth = mutableListOf<Float>()
            val aspectRatios = mutableListOf<Float>()
            val scales = mutableListOf<Float>()

            // For same strides, we merge the anchors in the same order.
            var lastSameStrideLayer = layerId
            while (lastSameStrideLayer < strides.size &&
                strides[lastSameStrideLayer] == strides[layerId]
            ) {
                val scale = calculateScale(
                    MIN_SCALE,
                    MAX_SCALE, lastSameStrideLayer, strides.size
                )
                for (aspectRatioId in 0 until ASPECT_RATIOS_SIZE) {
                    aspectRatios.add(1.0f)
                    scales.add(scale)
                }
                val scaleNext = if (lastSameStrideLayer == strides.size - 1) {
                    1.0f
                } else {
                    calculateScale(
                        MIN_SCALE,
                        MAX_SCALE, lastSameStrideLayer + 1, strides.size
                    )
                }
                scales.add(sqrt(scale * scaleNext))
                aspectRatios.add(1.0f)
                lastSameStrideLayer += 1
            }
            for (i in 0 until aspectRatios.size) {
                val ratioSqrts = sqrt(aspectRatios[i])
                anchorHeight.add(scales[i] / ratioSqrts)
                anchorWidth.add(scales[i] * ratioSqrts)
            }
            val stride = strides[layerId]
            val featureMapHeight = ceil(1.0f * INPUT_SIZE_HEIGHT / stride).toInt()
            val featureMapWidth = ceil(1.0f * INPUT_SIZE_WIDTH / stride).toInt()

            for (y in 0 until featureMapHeight) {
                for (x in 0 until featureMapWidth) {
                    for (anchorId in 0 until anchorHeight.size) {
                        val xCenter = (x + ANCHOR_OFFSET_X) * 1.0f / featureMapWidth
                        val yCenter = (y + ANCHOR_OFFSET_Y) * 1.0f / featureMapHeight

                        val currentAnchor = Anchor(xCenter, yCenter, 1.0f, 1.0f)
                        newAnchors.add(currentAnchor)
                    }
                }
            }
            layerId = lastSameStrideLayer
        }
        return newAnchors
    }

    private fun calculateScale(
        minScale: Float,
        maxScale: Float,
        strideIndex: Int,
        numStrides: Int
    ): Float {
        return minScale + (maxScale - minScale) * 1.0f * strideIndex / (numStrides - 1.0f)
    }

    private fun computeSigmoid(inputValue: Float): Float {
        var finalInputValue = max(inputValue, -100f)
        finalInputValue = min(finalInputValue, 100f)
        finalInputValue *= -1
        return 1.0f / (1.0f + exp(finalInputValue))
    }

    private fun weightedNonMaxSuppression(
        indexedScores: List<IndexedScore>,
        detections: List<Detection>
    ): List<Detection> {
        val remainedIndexedScores = indexedScores.toMutableList()
        val remained = mutableListOf<IndexedScore>()
        val candidates = mutableListOf<IndexedScore>()
        val outputLocations = mutableListOf<Detection>()

        while (remainedIndexedScores.isNotEmpty()) {
            val detection = detections[remainedIndexedScores[0].index]
            if (detection.score < -1.0f) {
                break
            }
            remained.clear()
            candidates.clear()
            val location = RectF(detection.location)
            // This includes the first box
            remainedIndexedScores.forEach { indexedScore ->
                val restLocation = RectF(detections[indexedScore.index].location)
                val similarity = overlapSimilarity(restLocation, location)
                if (similarity > MIN_SUPPRESSION_THRESHOLD) {
                    candidates.add(indexedScore)
                } else {
                    remained.add(indexedScore)
                }
            }
            val weightedLocation = RectF(detection.location)
            var weightedLeftEye = detection.leftEye
            var weightedRightEye = detection.rightEye
            var weightedNose = detection.nose
            var weightedMouth = detection.mouth
            if (candidates.isNotEmpty()) {
                var wXMin = 0.0f
                var wYMin = 0.0f
                var wXMax = 0.0f
                var wYMax = 0.0f
                var wLeftEyeX = 0.0f
                var wLeftEyeY = 0.0f
                var wRightEyeX = 0.0f
                var wRightEyeY = 0.0f
                var wNoseX = 0.0f
                var wNoseY = 0.0f
                var wMouthX = 0.0f
                var wMouthY = 0.0f
                var totalScore = 0.0f
                candidates.forEach { candidate ->
                    totalScore += candidate.score
                    val bbox = detections[candidate.index].location
                    val leftEye = detections[candidate.index].leftEye
                    val rightEye = detections[candidate.index].rightEye
                    val nose = detections[candidate.index].nose
                    val mouth = detections[candidate.index].mouth

                    wXMin += bbox.left * candidate.score
                    wYMin += bbox.top * candidate.score
                    wXMax += bbox.right * candidate.score
                    wYMax += bbox.bottom * candidate.score

                    wLeftEyeX += leftEye.x * candidate.score
                    wLeftEyeY += leftEye.y * candidate.score
                    wRightEyeX += rightEye.x * candidate.score
                    wRightEyeY += rightEye.y * candidate.score

                    wNoseX += nose.x * candidate.score
                    wNoseY += nose.y * candidate.score

                    wMouthX += mouth.x * candidate.score
                    wMouthY += mouth.y * candidate.score
                }
                weightedLocation.left = wXMin / totalScore * INPUT_SIZE_WIDTH
                weightedLocation.top = wYMin / totalScore * INPUT_SIZE_HEIGHT
                weightedLocation.right = wXMax / totalScore * INPUT_SIZE_WIDTH
                weightedLocation.bottom = wYMax / totalScore * INPUT_SIZE_HEIGHT

                val weightedLeftEyeX = wLeftEyeX / totalScore * INPUT_SIZE_WIDTH
                val weightedLeftEyeY = wLeftEyeY / totalScore * INPUT_SIZE_HEIGHT
                weightedLeftEye = Landmark(weightedLeftEyeX, weightedLeftEyeY)

                val weightedRightEyeX = wRightEyeX / totalScore * INPUT_SIZE_WIDTH
                val weightedRightEyeY = wRightEyeY / totalScore * INPUT_SIZE_HEIGHT
                weightedRightEye = Landmark(weightedRightEyeX, weightedRightEyeY)

                val weightedNoseX = wNoseX / totalScore * INPUT_SIZE_WIDTH
                val weightedNoseY = wNoseY / totalScore * INPUT_SIZE_HEIGHT
                weightedNose = Landmark(weightedNoseX, weightedNoseY)

                val weightedMouthX = wMouthX / totalScore * INPUT_SIZE_WIDTH
                val weightedMouthY = wMouthY / totalScore * INPUT_SIZE_HEIGHT
                weightedMouth = Landmark(weightedMouthX, weightedMouthY)
            }
            remainedIndexedScores.clear()
            remainedIndexedScores.addAll(remained)
            val weightedDetection = Detection(
                weightedLocation,
                weightedLeftEye,
                weightedRightEye,
                weightedNose,
                weightedMouth,
                detection.score
            )
            outputLocations.add(weightedDetection)
        }
        return outputLocations
    }

    private fun overlapSimilarity(rect1: RectF, rect2: RectF): Float {
        if (!RectF.intersects(rect1, rect2)) {
            return 0.0f
        }
        val intersection = RectF()
        intersection.setIntersect(rect1, rect2)
        val intersectionArea = intersection.height() * intersection.width()
        val normalization =
            rect1.height() * rect1.width() + rect2.height() * rect2.width() - intersectionArea
        return if (normalization > 0.0f) {
            intersectionArea / normalization
        } else {
            0.0f
        }
    }

    private class Anchor(val xCenter: Float, val yCenter: Float, val h: Float, val w: Float)
    internal class Landmark(val x: Float, val y: Float)
    internal class Detection(
        val location: RectF,
        val leftEye: Landmark,
        val rightEye: Landmark,
        val nose: Landmark,
        val mouth: Landmark,
        val score: Float
    )
    private class IndexedScore(val index: Int, val score: Float)

    enum class FaceOvalPosition(val instructionStringRes: Int) {
        MATCHED(R.string.amplify_ui_liveness_challenge_instruction_hold_face_during_freshness),
        TOO_FAR_LEFT(R.string.amplify_ui_liveness_challenge_instruction_move_face_closer),
        TOO_FAR_RIGHT(R.string.amplify_ui_liveness_challenge_instruction_move_face_closer),
        TOO_CLOSE(R.string.amplify_ui_liveness_challenge_instruction_move_face_further),
        TOO_FAR(R.string.amplify_ui_liveness_challenge_instruction_move_face_closer)
    }

    companion object {
        private const val MIN_SUPPRESSION_THRESHOLD = 0.3f
        private const val MIN_SCORE_THRESHOLD = 0.7f
        private val strides = listOf(8, 16, 16, 16)
        private const val ASPECT_RATIOS_SIZE = 1
        private const val MIN_SCALE = 0.1484375f
        private const val MAX_SCALE = 0.75f
        private const val ANCHOR_OFFSET_X = 0.5f
        private const val ANCHOR_OFFSET_Y = 0.5f
        private const val INPUT_SIZE_HEIGHT = 128
        private const val INPUT_SIZE_WIDTH = 128
        private const val GOLDEN_RATIO = 1.618f
        private const val ALPHA = 2.0f
        private const val GAMMA = 1.8f
        const val X_SCALE = 128f
        const val Y_SCALE = 128f
        const val H_SCALE = 128f
        const val W_SCALE = 128f
        const val NUM_BOXES = 896

        /**
         * Face detection coordinates:
         * 0, 1, 2, 3 - face bounding box
         * 4, 5 - left eye
         * 6, 7 - right eye
         * 8, 9 - nose
         * 10, 11 - mouth
         * 12, 13 - left eye tragion
         * 14, 15 - right eye tragion
         */
        const val NUM_COORDS = 16
        const val INITIAL_FACE_DISTANCE_THRESHOLD = 0.32f

        fun loadModel(context: Context): Interpreter {
            val modelFileDescriptor =
                context.assets.openFd("face_detection_short_range.tflite")
            val modelInputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
            val modelByteBuffer = modelInputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                modelFileDescriptor.startOffset, modelFileDescriptor.declaredLength
            )
            return Interpreter(modelByteBuffer)
        }

        fun calculateFaceOvalPosition(
            face: RectF,
            ovalRect: RectF,
            faceOvalMatching: FaceTargetMatchingParameters
        ): FaceOvalPosition {
            val intersection = intersectionOverUnion(face, ovalRect)
            val ovalMatchWidthThreshold =
                ovalRect.width() * faceOvalMatching.targetIouWidthThreshold
            val ovalMatchHeightThreshold =
                ovalRect.height() * faceOvalMatching.targetIouHeightThreshold
            val faceDetectionWidthThreshold =
                ovalRect.width() * faceOvalMatching.faceIouWidthThreshold
            val faceDetectionHeightThreshold =
                ovalRect.height() * faceOvalMatching.faceIouHeightThreshold

            return if (intersection > faceOvalMatching.targetIouThreshold &&
                abs(ovalRect.left - face.left) < ovalMatchWidthThreshold &&
                abs(ovalRect.right - face.right) < ovalMatchWidthThreshold &&
                abs(ovalRect.bottom - face.bottom) < ovalMatchHeightThreshold
            ) {
                FaceOvalPosition.MATCHED
            } else if (ovalRect.left > face.left && ovalRect.right > face.right) {
                FaceOvalPosition.TOO_FAR_LEFT
            } else if (face.left > ovalRect.left && face.right > ovalRect.right) {
                FaceOvalPosition.TOO_FAR_RIGHT
            } else if (ovalRect.top - face.top > faceDetectionHeightThreshold ||
                face.bottom - ovalRect.bottom > faceDetectionHeightThreshold ||
                (
                    ovalRect.left - face.left > faceDetectionWidthThreshold &&
                        face.right - ovalRect.right > faceDetectionWidthThreshold
                    )
            ) {
                FaceOvalPosition.TOO_CLOSE
            } else {
                FaceOvalPosition.TOO_FAR
            }
        }

        fun calculateFaceMatchPercentage(
            face: RectF,
            ovalRect: RectF,
            faceOvalMatching: FaceTargetMatchingParameters,
            initialIou: Float
        ): Float {
            val currentIou = intersectionOverUnion(face, ovalRect)
            return max(
                min(
                    1f,
                    (0.75f * (currentIou - initialIou)) /
                        (faceOvalMatching.targetIouThreshold - initialIou) + 0.25f
                ),
                0f
            )
        }

        fun calculateFaceDistance(
            leftEye: Landmark,
            rightEye: Landmark,
            mouth: Landmark,
            width: Int,
            height: Int
        ): Float {
            val pupilDistance = calculatePupilDistance(leftEye, rightEye)
            val faceHeight = calculateFaceHeight(leftEye, rightEye, mouth)

            val calibratedPupilDistance = (ALPHA * pupilDistance + GAMMA * faceHeight) / 2f / ALPHA
            val ovalWidth = getStaticOvalWidth(width.toFloat(), height.toFloat())
            return calibratedPupilDistance / ovalWidth
        }

        private fun calculatePupilDistance(leftEye: Landmark, rightEye: Landmark): Float {
            return sqrt((leftEye.x - rightEye.x).pow(2) + (leftEye.y - rightEye.y).pow(2))
        }

        private fun calculateFaceHeight(leftEye: Landmark, rightEye: Landmark, mouth: Landmark):
            Float {
            val eyeCenterX = (leftEye.x + rightEye.x) / 2
            val eyeCenterY = (leftEye.y + rightEye.y) / 2
            return sqrt((eyeCenterX - mouth.x).pow(2) + (eyeCenterY - mouth.y).pow(2))
        }

        private fun getStaticOvalWidth(width: Float, height: Float, enlargeFactor: Int = 1): Float {
            val r = 0.8f * enlargeFactor
            var newWidth = width
            if (width > height) {
                newWidth = 3f / 4f * height
            }
            return r * newWidth
        }

        fun intersectionOverUnion(boxOne: RectF, boxTwo: RectF): Float {
            val xA = max(boxOne.left, boxTwo.left)
            val yA = max(boxOne.top, boxTwo.top)
            val xB = min(boxOne.right, boxTwo.right)
            val yB = min(boxOne.bottom, boxTwo.bottom)

            val intersectionArea = max(0f, xB - xA) * max(0f, yB - yA)
            if (intersectionArea == 0f) {
                return 0f
            }

            val boxAArea = abs((boxOne.bottom - boxOne.top) * (boxOne.right - boxOne.left))
            val boxBArea = abs((boxTwo.bottom - boxTwo.top) * (boxTwo.right - boxTwo.left))

            return intersectionArea / (boxAArea + boxBArea - intersectionArea)
        }
    }
}
