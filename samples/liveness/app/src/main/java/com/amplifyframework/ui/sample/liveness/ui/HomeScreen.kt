package com.amplifyframework.ui.sample.liveness.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.sample.liveness.AuthState
import com.amplifyframework.ui.sample.liveness.MainViewModel
import com.amplifyframework.ui.sample.liveness.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onStartChallenge: (sessionId: String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val authState = viewModel.authState.collectAsState().value
    val fetchingSession = viewModel.fetchingSession.collectAsState().value
    val activity = LocalContext.current.findActivity()

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
                Button(
                    onClick = {
                        viewModel.createLivenessSession { sessionId ->
                            sessionId?.let { onStartChallenge(it) }
                        }
                    }
                ) {
                    Text(stringResource(R.string.create_liveness_session))
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}