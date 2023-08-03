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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSCredentialsProvider
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.ui.liveness.camera.LivenessCoordinator
import com.amplifyframework.ui.liveness.camera.OnChallengeComplete
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.helper.VideoViewportSize
import com.amplifyframework.ui.liveness.util.hasCameraPermission
import kotlinx.coroutines.launch

/**
 * @param sessionId of challenge
 * @param region AWS region to stream the video to. Current supported regions are listed in [add link here]
 * @param credentialsProvider to provide custom CredentialsProvider for authentication. Default uses initialized Amplify.Auth CredentialsProvider
 * @param disableStartView to bypass warmup screen.
 * @param onComplete callback notifying a completed challenge
 * @param onError callback containing exception for cause
 */
@Composable
fun FaceLivenessDetector(
    sessionId: String,
    region: String,
    credentialsProvider: AWSCredentialsProvider<AWSCredentials>? = null,
    disableStartView: Boolean = false,
    onComplete: Action,
    onError: Consumer<FaceLivenessDetectionException>
) {
    val scope = rememberCoroutineScope()
    val key = Triple(sessionId, region, credentialsProvider)
    var showReadyView by remember(key) { mutableStateOf(!disableStartView) }
    var isFinished by remember(key) { mutableStateOf(false) }
    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnError by rememberUpdatedState(onError)

    if (isFinished) {
        return
    }

    // fails challenge if no camera permission set
    if (!LocalContext.current.hasCameraPermission()) {
        LaunchedEffect(key) {
            isFinished = true
            currentOnError.accept(FaceLivenessDetectionException.CameraPermissionDeniedException())
        }
        return
    }

    // fails challenge if session ID is empty
    if (sessionId.isBlank()) {
        LaunchedEffect(key) {
            isFinished = true
            currentOnError.accept(
                FaceLivenessDetectionException.SessionNotFoundException("Session ID cannot be empty.")
            )
        }
        return
    }

    // Locks portrait orientation for duration of challenge and resets on complete
    LockPortraitOrientation { resetOrientation ->
        Surface(color = MaterialTheme.colorScheme.background) {
            if (showReadyView) {
                GetReadyView {
                    showReadyView = false
                }
            } else {
                AlwaysOnMaxBrightnessScreen()
                ChallengeView(
                    key = key,
                    sessionId = sessionId,
                    region,
                    credentialsProvider = credentialsProvider,
                    onChallengeComplete = {
                        scope.launch {
                            isFinished = true
                            resetOrientation()
                            currentOnComplete.call()
                        }
                    },
                    onChallengeFailed = {
                        scope.launch {
                            isFinished = true
                            resetOrientation()
                            currentOnError.accept(it)
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun ChallengeView(
    key: Any,
    sessionId: String,
    region: String,
    credentialsProvider: AWSCredentialsProvider<AWSCredentials>?,
    onChallengeComplete: OnChallengeComplete,
    onChallengeFailed: Consumer<FaceLivenessDetectionException>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var coordinator by remember { mutableStateOf<LivenessCoordinator?>(null) }
    val currentOnChallengeComplete by rememberUpdatedState(onChallengeComplete)
    val currentOnChallengeFailed by rememberUpdatedState(onChallengeFailed)

    DisposableEffect(key) {
        coordinator = LivenessCoordinator(
            context,
            lifecycleOwner,
            sessionId,
            region,
            credentialsProvider,
            onChallengeComplete = { currentOnChallengeComplete() },
            onChallengeFailed = { currentOnChallengeFailed.accept(it) }
        )

        onDispose {
            coordinator?.destroy(context)
        }
    }

    val livenessCoordinator = coordinator ?: return
    val livenessState = livenessCoordinator.livenessState

    val localDensity = LocalDensity.current
    val backgroundColor = if (livenessState.faceGuideRect != null) {
        Color.White
    } else {
        Color.Black
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onGloballyPositioned {
                livenessState.updateVideoViewportSize(
                    VideoViewportSize.create(it.size, localDensity)
                )
            }
    ) {
        val videoViewportSize = livenessState.videoViewportSize

        if (videoViewportSize != null) {
            Box(
                modifier = Modifier
                    .size(videoViewportSize.viewportDpSize)
                    .align(Alignment.Center)
            ) {
                AndroidView(
                    { livenessCoordinator.previewTextureView },
                    Modifier
                        .size(videoViewportSize.viewportDpSize)
                        .align(Alignment.Center)
                )
            }

            livenessState.faceGuideRect?.let {
                FaceGuide(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    faceGuideRect = it,
                    videoViewportSize = videoViewportSize
                )
            }

            if (livenessState.runningFreshness) {
                FreshnessChallenge(
                    key,
                    modifier = Modifier.fillMaxSize(),
                    colors = livenessState.colorChallenge!!.challengeColors,
                    onColorDisplayed = { currentColor, previousColor, sequenceNumber, colorStart ->
                        livenessCoordinator.processColorDisplayed(
                            currentColor,
                            previousColor,
                            sequenceNumber,
                            colorStart
                        )
                    },
                    onComplete = {
                        livenessCoordinator.processFreshnessChallengeComplete()
                    }
                )
            }

            livenessState.faceGuideRect?.let {
                RecordingIndicator(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }

            CancelChallengeButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                livenessCoordinator.processSessionError(
                    FaceLivenessDetectionException.UserCancelledException(),
                    true
                )
            }

            Box(
                modifier = Modifier
                    .size(videoViewportSize.viewportDpSize)
                    .align(Alignment.Center)
            ) {
                if (livenessState.faceGuideRect != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            InstructionMessage(livenessState.livenessCheckState.value, true)
                            if (livenessState.livenessCheckState.value.instructionId ==
                                FaceDetector.FaceOvalPosition.TOO_FAR.instructionStringRes
                            ) {
                                val scaledOvalRect = livenessState.faceGuideRect?.let {
                                    videoViewportSize.getScaledBoundingRect(it)
                                } ?: RectF()
                                val progressWidth = with(LocalDensity.current) {
                                    ((scaledOvalRect.right - scaledOvalRect.left) * 0.6f).toDp()
                                }
                                LinearProgressIndicator(
                                    progress = livenessState.faceMatchPercentage,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .width(progressWidth)
                                        .height(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            InstructionMessage(livenessState.livenessCheckState.value)
                        }
                    }
                }
            }
        }
    }
}
