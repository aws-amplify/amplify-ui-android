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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.challengeResponse
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInContinueWithMfaSetupSelectionState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import kotlinx.coroutines.launch

@Composable
fun SignInContinueWithMfaSetupSelection(
    state: SignInContinueWithMfaSetupSelectionState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInContinueWithMfaSetupSelectionState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_continue_setup_mfa_select))
    },
    footerContent: @Composable (state: SignInContinueWithMfaSetupSelectionState) -> Unit = {
        SignInContinueWithMfaSetupSelectionFooter(state = it)
    }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fieldState = state.form.fields[FieldKey.MfaSelection]?.state!!

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(R.string.amplify_ui_authenticator_mfa_setup_description)
        )
        val items = remember { state.allowedMfaTypes.map { it.challengeResponse } }
        RadioGroup(
            items = items,
            selected = fieldState.content,
            onSelect = { fieldState.content = it },
            label = {
                when (it) {
                    MFAType.SMS.challengeResponse -> context.getString(R.string.amplify_ui_authenticator_mfa_sms)
                    MFAType.EMAIL.challengeResponse -> context.getString(R.string.amplify_ui_authenticator_mfa_email)
                    else -> context.getString(R.string.amplify_ui_authenticator_mfa_totp)
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        AuthenticatorButton(
            modifier = modifier.testTag(TestTags.SignInConfirmButton),
            label = context.getString(R.string.amplify_ui_authenticator_button_continue),
            onClick = { scope.launch { state.continueSignIn() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
fun SignInContinueWithMfaSetupSelectionFooter(
    state: SignInContinueWithMfaSetupSelectionState,
    modifier: Modifier = Modifier
) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)
