package com.amplifyframework.ui.sample.liveness.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.amplifyframework.ui.liveness.media.VideoCodec
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import com.amplifyframework.ui.liveness.ui.LivenessColorScheme
import com.amplifyframework.ui.liveness.ui.VideoOptions
import com.amplifyframework.ui.sample.liveness.MainViewModel

@Composable
fun LivenessScreen(
    viewModel: MainViewModel,
    videoCodec: VideoCodec,
    onChallengeComplete: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val sessionId = viewModel.sessionId.collectAsState().value ?: return

    MaterialTheme(colorScheme = LivenessColorScheme.default()) {
        FaceLivenessDetector(
            sessionId = sessionId,
            region = "us-east-1",
            disableStartView = false,
            videoOptions = VideoOptions(codec = videoCodec),
            onComplete = {
                viewModel.fetchSessionResult(sessionId)
                onChallengeComplete()
            },
            onError = { error ->
                when (error) {
                    is FaceLivenessDetectionException.UserCancelledException -> onBack()
                    // An interruption is transient, so prompt for a retry instead of reporting a failed check.
                    is FaceLivenessDetectionException.SessionInterruptedException -> {
                        Toast.makeText(
                            context,
                            "Your check was interrupted. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        onBack()
                    }
                    else -> {
                        viewModel.reportErrorResult(error)
                        onChallengeComplete()
                    }
                }
            }
        )
    }
}