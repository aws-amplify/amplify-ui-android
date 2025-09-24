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

package com.amplifyframework.ui.authenticator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amplifyframework.ui.authenticator.data.AuthenticationFlow
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.SignUpFormBuilder
import com.amplifyframework.ui.authenticator.options.TotpOptions
import com.amplifyframework.ui.authenticator.util.AuthenticatorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Create the [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for the
 * Authenticator composable.
 * @param initialStep The [AuthenticatorInitialStep] that the user sees first when the Authenticator becomes visible. Default is [AuthenticatorStep.SignIn].
 * @param signUpForm The builder instance for configuring the sign up form content. A default sign up form will be inferred from your
 * Amplify configuration, but this may be used to modify that form by changing the order of the fields, adding fields for custom or optional user attributes,
 * or adding fully custom fields. Has an [SignUpFormBuilder] receiver.
 * @param totpOptions Optional information that can be used to configure the user's experience when using
 * [TOTP MFA](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-settings-mfa-totp.html).
 */
@Composable
fun rememberAuthenticatorState(
    initialStep: AuthenticatorInitialStep = AuthenticatorStep.SignIn,
    signUpForm: SignUpFormBuilder.() -> Unit = {},
    totpOptions: TotpOptions? = null,
    authenticationFlow: AuthenticationFlow = AuthenticationFlow.Password
): AuthenticatorState {
    val viewModel = viewModel<AuthenticatorViewModel>()
    val scope = rememberCoroutineScope()
    return remember {
        val configuration = AuthenticatorConfiguration(
            initialStep = initialStep,
            signUpForm = signUpForm,
            totpOptions = totpOptions,
            authenticationFlow = authenticationFlow
        )

        viewModel.start(configuration)
        AuthenticatorStateImpl(viewModel).also { state ->
            viewModel.stepState.onEach { state.stepState = it }.launchIn(scope)
        }
    }
}

/**
 * Create the [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for the
 * Authenticator composable.
 * @param initialStep The [AuthenticatorInitialStep] that the user sees first when the Authenticator becomes visible. Default is [AuthenticatorStep.SignIn].
 * @param signUpForm The builder instance for configuring the sign up form content. A default sign up form will be inferred from your
 * Amplify configuration, but this may be used to modify that form by changing the order of the fields, adding fields for custom or optional user attributes,
 * or adding fully custom fields. Has an [SignUpFormBuilder] receiver.
 */
@Composable
fun rememberAuthenticatorState(
    initialStep: AuthenticatorInitialStep = AuthenticatorStep.SignIn,
    signUpForm: SignUpFormBuilder.() -> Unit = {}
): AuthenticatorState = rememberAuthenticatorState(
    initialStep = initialStep,
    totpOptions = null,
    signUpForm = signUpForm
)

/**
 * The [state holder](https://developer.android.com/jetpack/compose/state#managing-state) instance for
 * the Authenticator composable.
 */
@Stable
interface AuthenticatorState {
    /**
     * The state holder instance for the current [AuthenticatorStep] being shown to the user.
     */
    val stepState: AuthenticatorStepState

    /**
     * A flow of [AuthenticatorMessage] that may be presented to the user, such as messages indicating a
     * password was successfully reset or certain error responses were received from the Auth backend. By
     * default these messages will be shown in a Snackbar by the Authenticator, but this can be overridden
     * by supplying an onDisplayMessage argument to the Authenticator composable.
     */
    val messages: Flow<AuthenticatorMessage>
}

internal class AuthenticatorStateImpl constructor(private val viewModel: AuthenticatorViewModel) : AuthenticatorState {
    override var stepState by mutableStateOf<AuthenticatorStepState>(LoadingState)

    override val messages: Flow<AuthenticatorMessage>
        get() = viewModel.events
}
