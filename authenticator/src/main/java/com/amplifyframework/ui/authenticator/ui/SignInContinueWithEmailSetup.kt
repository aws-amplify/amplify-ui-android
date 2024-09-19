/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInContinueWithEmailSetupState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.launch

@Composable
fun SignInContinueWithEmailSetup(
    state: SignInContinueWithEmailSetupState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInContinueWithEmailSetupState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_continue_email_setup))
    },
    footerContent: @Composable (state: SignInContinueWithEmailSetupState) -> Unit = {
        SignInContinueWithEmailSetupFooter(state = it)
    }
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(R.string.amplify_ui_authenticator_email_setup_description)
        )
        AuthenticatorForm(state = state.form)
        AuthenticatorButton(
            modifier = modifier.testTag(TestTags.SignInConfirmButton),
            onClick = { scope.launch { state.continueSignIn() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
fun SignInContinueWithEmailSetupFooter(
    state: SignInContinueWithEmailSetupState,
    modifier: Modifier = Modifier
) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)