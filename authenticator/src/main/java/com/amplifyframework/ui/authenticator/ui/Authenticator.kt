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

package com.amplifyframework.ui.authenticator.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.amplifyframework.ui.authenticator.AuthenticatorState
import com.amplifyframework.ui.authenticator.AuthenticatorStepState
import com.amplifyframework.ui.authenticator.ErrorState
import com.amplifyframework.ui.authenticator.LoadingState
import com.amplifyframework.ui.authenticator.PasswordResetConfirmState
import com.amplifyframework.ui.authenticator.PasswordResetState
import com.amplifyframework.ui.authenticator.SignInConfirmCustomState
import com.amplifyframework.ui.authenticator.SignInConfirmMfaState
import com.amplifyframework.ui.authenticator.SignInConfirmNewPasswordState
import com.amplifyframework.ui.authenticator.SignInState
import com.amplifyframework.ui.authenticator.SignUpConfirmState
import com.amplifyframework.ui.authenticator.SignUpState
import com.amplifyframework.ui.authenticator.SignedInState
import com.amplifyframework.ui.authenticator.VerifyUserConfirmState
import com.amplifyframework.ui.authenticator.VerifyUserState
import com.amplifyframework.ui.authenticator.rememberAuthenticatorState
import com.amplifyframework.ui.authenticator.util.AuthenticatorMessage

/**
 * The Composable Authenticator UI.
 * @param modifier The [Modifier] instance.
 * @param state The state holder for the Authenticator.
 * @param loadingContent The content to show for the [LoadingState].
 * @param signInContent The content to show for the [SignInState].
 * @param signInConfirmMfaContent The content to show for the [SignInConfirmMfaState].
 * @param signInConfirmCustomContent The content to show for the [SignInConfirmCustomState].
 * @param signInConfirmNewPasswordContent The content to show for the [SignInConfirmNewPasswordState].
 * @param signUpContent The content to show for the [SignUpState].
 * @param signUpConfirmContent The content to show for the [SignUpConfirmState].
 * @param passwordResetContent The content to show for the [PasswordResetState].
 * @param passwordResetConfirmContent The content to show for the [PasswordResetConfirmState].
 * @param verifyUserContent The content to show for the [VerifyUserState].
 * @param verifyUserConfirmContent The content to show for the [VerifyUserConfirmState].
 * @param errorContent The content to show for the [ErrorState].
 * @param headerContent The header content to show throughout the Authenticator UI.
 * @param footerContent The footer content to show throughout the Authenticator UI.
 * @param onDisplayMessage Override the default handling for displaying an [AuthenticatorMessage].
 * @param content The content shown when the Authenticator reaches the [SignedInState].
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Authenticator(
    modifier: Modifier = Modifier,
    state: AuthenticatorState = rememberAuthenticatorState(),
    loadingContent: @Composable () -> Unit = { AuthenticatorLoading() },
    signInContent: @Composable (state: SignInState) -> Unit = { SignIn(it) },
    signInConfirmMfaContent: @Composable (state: SignInConfirmMfaState) -> Unit = { SignInConfirmMfa(it) },
    signInConfirmCustomContent: @Composable (state: SignInConfirmCustomState) -> Unit = { SignInConfirmCustom(it) },
    signInConfirmNewPasswordContent: @Composable (state: SignInConfirmNewPasswordState) -> Unit = {
        SignInConfirmNewPassword(it)
    },
    signUpContent: @Composable (state: SignUpState) -> Unit = { SignUp(it) },
    signUpConfirmContent: @Composable (state: SignUpConfirmState) -> Unit = { SignUpConfirm(it) },
    passwordResetContent: @Composable (state: PasswordResetState) -> Unit = { PasswordReset(it) },
    passwordResetConfirmContent: @Composable (state: PasswordResetConfirmState) -> Unit = { PasswordResetConfirm(it) },
    verifyUserContent: @Composable (state: VerifyUserState) -> Unit = { VerifyUser(it) },
    verifyUserConfirmContent: @Composable (state: VerifyUserConfirmState) -> Unit = { VerifyUserConfirm(it) },
    errorContent: @Composable (state: ErrorState) -> Unit = { AuthenticatorError(it) },
    headerContent: @Composable () -> Unit = {},
    footerContent: @Composable () -> Unit = {},
    onDisplayMessage: ((AuthenticatorMessage) -> Unit)? = null,
    content: @Composable (state: SignedInState) -> Unit
) {
    val snackbarState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val stepState = state.stepState

    if (stepState is SignedInState) {
        content(stepState)
    } else {
        Box(modifier = modifier) {
            AnimatedContent(
                targetState = stepState,
                transitionSpec = { defaultTransition() }
            ) { targetState ->
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    headerContent()
                    when (targetState) {
                        is LoadingState -> loadingContent()
                        is SignInState -> signInContent(targetState)
                        is SignInConfirmMfaState -> signInConfirmMfaContent(targetState)
                        is SignInConfirmCustomState -> signInConfirmCustomContent(targetState)
                        is SignInConfirmNewPasswordState -> signInConfirmNewPasswordContent(
                            targetState
                        )
                        is SignUpState -> signUpContent(targetState)
                        is PasswordResetState -> passwordResetContent(targetState)
                        is PasswordResetConfirmState -> passwordResetConfirmContent(targetState)
                        is ErrorState -> errorContent(targetState)
                        is SignUpConfirmState -> signUpConfirmContent(targetState)
                        is VerifyUserState -> verifyUserContent(targetState)
                        is VerifyUserConfirmState -> verifyUserConfirmContent(targetState)
                        else -> Unit
                    }
                    footerContent()
                }
            }
            SnackbarHost(hostState = snackbarState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    LaunchedEffect(Unit) {
        state.messages.collect { event ->
            if (onDisplayMessage != null) {
                onDisplayMessage(event)
            } else {
                snackbarState.showSnackbar(event.message(context))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
internal fun AnimatedContentScope<AuthenticatorStepState>.defaultTransition(): ContentTransform {
    // Show reverse transition when going back to signIn
    if (targetState is SignInState && initialState != LoadingState) {
        return fadeIn(animationSpec = tween(220, delayMillis = 90)) with
            scaleOut(targetScale = 0.92f, animationSpec = tween(90)) +
                fadeOut(animationSpec = tween(90))
    }
    // Show forward transition for all others
    return fadeIn(animationSpec = tween(220, delayMillis = 90)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) with
        fadeOut(animationSpec = tween(90))
}
