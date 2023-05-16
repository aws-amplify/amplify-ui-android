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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInConfirmMfaState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.launch

@Composable
fun SignInConfirmMfa(
    state: SignInConfirmMfaState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInConfirmMfaState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_confirm_mfa))
    },
    footerContent: @Composable (state: SignInConfirmMfaState) -> Unit = { SignInConfirmMfaFooter(it) },
    deliveryNoticeContent: @Composable (details: AuthCodeDeliveryDetails?) -> Unit = { DeliveryDetails(details = it) }
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        deliveryNoticeContent(state.deliveryDetails)
        AuthenticatorForm(
            state = state.form
        )
        AuthenticatorButton(
            onClick = { scope.launch { state.confirmSignIn() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
fun SignInConfirmMfaFooter(
    state: SignInConfirmMfaState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = { state.moveTo(AuthenticatorStep.SignIn) }) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_back_to_signin))
        }
    }
}
