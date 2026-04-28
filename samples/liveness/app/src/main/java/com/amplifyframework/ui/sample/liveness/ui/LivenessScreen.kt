package com.amplifyframework.ui.sample.liveness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.amplifyframework.ui.liveness.media.VideoCodec
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.CtaButtonData
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import com.amplifyframework.ui.liveness.ui.LivenessColorScheme
import com.amplifyframework.ui.liveness.ui.VideoOptions
import com.amplifyframework.ui.sample.liveness.MainViewModel
import com.amplifyframework.ui.sample.liveness.R

@Composable
fun LivenessScreen(
    viewModel: MainViewModel,
    videoCodec: VideoCodec,
    onChallengeComplete: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val sessionId = viewModel.sessionId.collectAsState().value ?: return

    MaterialTheme(colorScheme = LivenessColorScheme.default()) {
        FaceLivenessDetector(
            sessionId = sessionId,
            region = "us-east-1",
            initialMessageResId = R.string.initial_message,
            ctaButtonData = CtaButtonData(
                text = "I'm ready",
                textStyle = LocalTextStyle.current,
                buttonColors = ButtonDefaults.buttonColors(),
            ),
            disableStartView = false,
            videoOptions = VideoOptions(codec = videoCodec),
            // When STUB_SESSION_FOR_UI_DEV is on we have no real Amplify session,
            // so tell FaceLivenessDetector to skip the AWS call and park on the
            // start view (camera preview + FaceGuide oval) for UI iteration.
            previewOnly = MainViewModel.STUB_SESSION_FOR_UI_DEV,
            onComplete = {
                viewModel.fetchSessionResult(sessionId)
                onChallengeComplete()
            },
            onError = {
                if (it is FaceLivenessDetectionException.UserCancelledException) {
                    onBack()
                } else {
                    viewModel.reportErrorResult(it)
                    onChallengeComplete()
                }
            }
        )
    }
}