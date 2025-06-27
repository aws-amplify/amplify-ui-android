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

package com.amplifyframework.ui.liveness.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSCredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin
import com.amplifyframework.predictions.aws.exceptions.AccessDeniedException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessSessionNotFoundException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessSessionTimeoutException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessUnsupportedChallengeTypeException
import com.amplifyframework.predictions.aws.models.ColorChallengeResponse
import com.amplifyframework.predictions.aws.models.RgbColor
import com.amplifyframework.predictions.aws.options.AWSFaceLivenessSessionOptions
import com.amplifyframework.predictions.models.Challenge
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import com.amplifyframework.predictions.models.VideoEvent
import com.amplifyframework.ui.liveness.BuildConfig
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.model.LivenessCheckState
import com.amplifyframework.ui.liveness.state.AttemptCounter
import com.amplifyframework.ui.liveness.state.LivenessState
import com.amplifyframework.ui.liveness.ui.Camera
import com.amplifyframework.ui.liveness.ui.ChallengeOptions
import com.amplifyframework.ui.liveness.util.WebSocketCloseCode
import java.util.Date
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal typealias OnMuxedSegment = (bytes: ByteArray, timestamp: Long) -> Unit
internal typealias OnChallengeComplete = () -> Unit
internal typealias OnFreshnessColorDisplayed = (
    currentColor: RgbColor,
    previousColor: RgbColor,
    sequenceNumber: Int,
    colorStartTime: Long
) -> Unit

@SuppressLint("UnsafeOptInUsageError")
internal class LivenessCoordinator(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val sessionId: String,
    private val region: String,
    private val credentialsProvider: AWSCredentialsProvider<AWSCredentials>?,
    private val disableStartView: Boolean,
    private val challengeOptions: ChallengeOptions,
    private val onChallengeComplete: OnChallengeComplete,
    val onChallengeFailed: Consumer<FaceLivenessDetectionException>
) {

    private val attemptCounter = AttemptCounter()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    val livenessState = LivenessState(
        sessionId = sessionId,
        context = context,
        disableStartView = disableStartView,
        onCaptureReady = this::processCaptureReady,
        onSessionError = this::processSessionError,
        onFinalEventsSent = this::processFinalEventsSent
    )

    private val preview = Preview.Builder().apply {
        Camera2Interop.Extender(this).apply {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(TARGET_FPS_MIN, TARGET_FPS_MAX)
            )
        }
        setTargetResolution(TARGET_RESOLUTION_SIZE)
    }.build()

    private val analyzer = FrameAnalyzer(context, livenessState)

    private val analysis = ImageAnalysis.Builder().apply {
        Camera2Interop.Extender(this).apply {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(TARGET_FPS_MIN, TARGET_FPS_MAX)
            )
        }
        setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        setTargetResolution(TARGET_RESOLUTION_SIZE)
    }.build().apply {
        setAnalyzer(analysisExecutor, analyzer)
    }

    private val encoder = LivenessVideoEncoder.create(
        context = context,
        width = TARGET_WIDTH,
        height = TARGET_HEIGHT,
        bitrate = TARGET_ENCODE_BITRATE,
        framerate = TARGET_FPS_MAX,
        keyframeInterval = TARGET_ENCODE_KEYFRAME_INTERVAL,
        onMuxedSegment = { bytes, time ->
            livenessState.livenessSessionInfo?.sendVideoEvent(VideoEvent(bytes, Date(time)))
        }
    ) ?: throw IllegalStateException("Failed to start the encoder.")

    private val renderer = OpenGLRenderer()
        .apply {
            attachInputPreview(preview)
            attachOutputSurface(
                encoder.inputSurface,
                Size(TARGET_WIDTH, TARGET_HEIGHT),
                0
            )
        }

    val previewTextureView = PreviewTextureView(context, renderer)

    private var disconnectEventReceived = false

    init {
        startLivenessSession()
        if (challengeOptions.hasOneCameraConfigured()) {
            launchCamera(challengeOptions.faceMovementAndLight.camera)
        } else {
            livenessState.loadingCameraPreview = true
        }
    }

    private fun launchCamera(camera: Camera) {
        MainScope().launch {
            delay(5_000)
            if (!previewTextureView.hasReceivedUpdate) {
                val faceLivenessException = FaceLivenessDetectionException(
                    "The camera failed to open within the allowed time limit.",
                    "Ensure the camera is available to use and that no other apps are using it."
                )
                processSessionError(faceLivenessException, true)
            }
        }
        MainScope().launch {
            getCameraProvider(context).apply {
                if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    unbindAll()

                    val (chosenCamera, orientation) = when (camera) {
                        Camera.Front -> Pair(CameraSelector.DEFAULT_FRONT_CAMERA, "front")
                        Camera.Back -> Pair(CameraSelector.DEFAULT_BACK_CAMERA, "back")
                    }

                    if (this.hasCamera(chosenCamera)) {
                        bindToLifecycle(
                            lifecycleOwner,
                            chosenCamera,
                            preview,
                            analysis
                        )
                    } else {
                        livenessState.loadingCameraPreview = false
                        val faceLivenessException = FaceLivenessDetectionException(
                            "A $orientation facing camera is required but no $orientation facing camera detected.",
                            "Enable a $orientation facing camera."
                        )
                        processSessionError(faceLivenessException, true)
                    }
                }
            }
        }
    }

    private fun startLivenessSession() {
        livenessState.livenessCheckState = LivenessCheckState.Initial.withConnectingMessage()
        attemptCounter.countAttempt()

        val faceLivenessSessionInformation = FaceLivenessSessionInformation(
            videoWidth = TARGET_WIDTH.toFloat(),
            videoHeight = TARGET_HEIGHT.toFloat(),
            challengeVersions = listOf(
                Challenge.FaceMovementAndLightChallenge("2.0.0"),
                Challenge.FaceMovementChallenge("1.0.0")
            ),
            region = region,
            preCheckViewEnabled = !disableStartView,
            attemptCount = attemptCounter.getCount()
        )

        val faceLivenessSessionOptions = AWSFaceLivenessSessionOptions.builder().apply {
            this@LivenessCoordinator.credentialsProvider?.let { credentialsProvider(it) }
        }.build()

        AWSPredictionsPlugin.startFaceLivenessSession(
            sessionId,
            faceLivenessSessionInformation,
            faceLivenessSessionOptions,
            BuildConfig.LIVENESS_VERSION_NAME,
            {
                livenessState.onLivenessSessionReady(it)
                if (!challengeOptions.hasOneCameraConfigured()) {
                    val foundChallenge = challengeOptions.getLivenessChallenge(it.challengeType)
                    launchCamera(foundChallenge.camera)
                }
            },
            {
                disconnectEventReceived = true
                onChallengeComplete()
            },
            { error ->
                val (faceLivenessException, shouldStopLivenessSession) = when (error) {
                    is AccessDeniedException ->
                        FaceLivenessDetectionException.AccessDeniedException(throwable = error) to false
                    is FaceLivenessSessionNotFoundException ->
                        FaceLivenessDetectionException.SessionNotFoundException(throwable = error) to false
                    is FaceLivenessSessionTimeoutException ->
                        FaceLivenessDetectionException.SessionTimedOutException(throwable = error) to false
                    is FaceLivenessUnsupportedChallengeTypeException ->
                        FaceLivenessDetectionException.UnsupportedChallengeTypeException(throwable = error) to true
                    else -> FaceLivenessDetectionException(
                        error.message ?: "Unknown error.",
                        error.recoverySuggestion, error
                    ) to false
                }
                processSessionError(faceLivenessException, shouldStopLivenessSession)
            }
        )
    }

    private fun unbindCamera(context: Context) {
        MainScope().launch {
            getCameraProvider(context).apply {
                unbindAll()
            }
        }
    }

    private fun processCaptureReady() {
        encoder.start()
    }

    internal fun processSessionError(
        faceLivenessException: FaceLivenessDetectionException,
        stopLivenessSession: Boolean
    ) {
        val webSocketCloseCode = when (faceLivenessException) {
            is FaceLivenessDetectionException.UserCancelledException -> WebSocketCloseCode.CANCELED
            is FaceLivenessDetectionException.FaceInOvalMatchExceededTimeLimitException -> WebSocketCloseCode.TIMEOUT
            else -> WebSocketCloseCode.RUNTIME_ERROR
        }
        livenessState.onError(stopLivenessSession, webSocketCloseCode)
        unbindCamera(context)
        onChallengeFailed.accept(faceLivenessException)
    }

    fun processColorDisplayed(
        currentColor: RgbColor,
        previousColor: RgbColor,
        sequenceNumber: Int,
        colorStartTime: Long
    ) {
        livenessState.livenessSessionInfo!!.sendChallengeResponseEvent(
            ColorChallengeResponse(
                livenessState.colorChallenge!!.challengeId,
                currentColor,
                previousColor,
                Date(colorStartTime),
                sequenceNumber
            )
        )
    }

    fun processLivenessCheckComplete() {
        livenessState.onLivenessChallengeComplete()
        stopEncoder { livenessState.onFullChallengeComplete() }
    }

    private fun processFinalEventsSent() {
        unbindCamera(context)
    }

    private fun stopEncoder(onComplete: () -> Unit) {
        encoder.stop {
            MainScope().launch {
                onComplete()
            }
        }
    }

    /**
     * This is only called when onDispose is triggered from FaceLivenessDetector view.
     * If we begin calling destroy in other places, we should ensure we are still tracking the proper error code.
     */
    fun destroy(context: Context) {
        // Destroy all resources so a new coordinator can safely be created
        encoder.stop {
            encoder.destroy()
        }
        val webSocketCloseCode = if (!disconnectEventReceived) WebSocketCloseCode.DISPOSED else null
        livenessState.onDestroy(true, webSocketCloseCode)
        unbindCamera(context)
        analysisExecutor.shutdown()
    }

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(context).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(context))
            }
        }

    companion object {
        const val TARGET_FPS_MIN = 24
        const val TARGET_FPS_MAX = 24
        const val TARGET_WIDTH = 480
        const val TARGET_HEIGHT = 640
        const val TARGET_ASPECT_RATIO = TARGET_WIDTH.toFloat() / TARGET_HEIGHT
        const val TARGET_ENCODE_BITRATE = (1024 * 1024 * .6).toInt()
        const val TARGET_ENCODE_KEYFRAME_INTERVAL = 1 // webm muxer only flushes to file on keyframe
        val TARGET_RESOLUTION_SIZE = Size(TARGET_WIDTH, TARGET_HEIGHT)
    }
}
