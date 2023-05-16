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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.launch

@Composable
fun SignIn(
    state: SignInState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (SignInState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin))
    },
    footerContent: @Composable (SignInState) -> Unit = { SignInFooter(it) }
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        AuthenticatorForm(
            state = state.form
        )
        AuthenticatorButton(
            onClick = { scope.launch { state.signIn() } },
            loading = !state.form.enabled,
            label = stringResource(R.string.amplify_ui_authenticator_button_signin),
            modifier = Modifier.testTag("SignInButton")
        )
        footerContent(state)
    }
}

@Composable
fun SignInFooter(
    state: SignInState,
    modifier: Modifier = Modifier,
    hideSignUp: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextButton(onClick = { state.moveTo(AuthenticatorStep.PasswordReset) }) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_forgot_password))
        }
        if (!hideSignUp) {
            TextButton(onClick = { state.moveTo(AuthenticatorStep.SignUp) }) {
                Text(stringResource(R.string.amplify_ui_authenticator_button_signup))
            }
        }
    }
}
