package com.amplifyframework.ui.sample.liveness.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.liveness.media.VideoCodec
import com.amplifyframework.ui.sample.liveness.AuthState
import com.amplifyframework.ui.sample.liveness.MainViewModel
import com.amplifyframework.ui.sample.liveness.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, onStartChallenge: (sessionId: String, videoCodec: VideoCodec) -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val authState = viewModel.authState.collectAsState().value
    val fetchingSession = viewModel.fetchingSession.collectAsState().value
    val activity = LocalContext.current.findActivity()

    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            authState is AuthState.Fetching || authState is AuthState.SigningIn -> {
                CircularProgressIndicator()
            }

            authState is AuthState.SignedOut -> {
                Button(
                    onClick = {
                        activity?.let { viewModel.launchSignIn(it) }
                    }
                ) {
                    Text(stringResource(R.string.sign_in))
                }
            }

            cameraPermissionState.status.shouldShowRationale -> {
                Text(stringResource(R.string.open_settings_camera_permissions))
            }

            !cameraPermissionState.status.isGranted -> {
                SideEffect {
                    cameraPermissionState.launchPermissionRequest()
                }
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text(stringResource(R.string.grant_camera_permission))
                }
            }

            fetchingSession -> {
                CircularProgressIndicator()
            }

            else -> {
                var videoCodec by remember { mutableStateOf<VideoCodec>(VideoCodec.VP8) }

                Column {
                    FormatSelector(
                        modifier = Modifier.heightIn(min = 48.dp),
                        videoCodec = VideoCodec.VP8,
                        selectedFormat = videoCodec,
                        onSelect = { videoCodec = it }
                    )
                    FormatSelector(
                        modifier = Modifier.heightIn(min = 48.dp),
                        videoCodec = VideoCodec.VP9,
                        selectedFormat = videoCodec,
                        onSelect = { videoCodec = it }
                    )
                    FormatSelector(
                        modifier = Modifier.heightIn(min = 48.dp),
                        videoCodec = VideoCodec.H264,
                        selectedFormat = videoCodec,
                        onSelect = { videoCodec = it }
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val sessionId = viewModel.createLivenessSession()
                                sessionId?.let { onStartChallenge(it, videoCodec) }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.create_liveness_session))
                    }
                }
            }
        }
    }
}

@Composable
fun FormatSelector(
    videoCodec: VideoCodec,
    selectedFormat: VideoCodec,
    onSelect: (VideoCodec) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.clickable { onSelect(videoCodec) }) {
        RadioButton(
            selected = videoCodec == selectedFormat,
            onClick = null
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = videoCodec::class.simpleName!!,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
