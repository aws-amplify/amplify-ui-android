package com.amplifyframework.ui.sample.liveness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import com.amplifyframework.ui.liveness.ui.LivenessColorScheme
import com.amplifyframework.ui.sample.liveness.MainViewModel

@Composable
fun LivenessScreen(
    viewModel: MainViewModel,
    onChallengeComplete: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val sessionId = viewModel.sessionId.collectAsState().value ?: return

    MaterialTheme(colorScheme = LivenessColorScheme.default()) {
        FaceLivenessDetector(
            sessionId = sessionId,
            region = "us-east-1",
            disableStartView = false,
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