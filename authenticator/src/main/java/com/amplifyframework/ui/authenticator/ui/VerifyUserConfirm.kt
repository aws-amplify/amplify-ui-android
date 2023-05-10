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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.VerifyUserConfirmState
import kotlinx.coroutines.launch

@Composable
fun VerifyUserConfirm(
    state: VerifyUserConfirmState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (VerifyUserConfirmState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_verify_user))
    },
    footerContent: @Composable (VerifyUserConfirmState) -> Unit = {
        VerifyUserConfirmFooter(it)
    },
    deliveryNoticeContent: @Composable (AuthCodeDeliveryDetails?) -> Unit = { DeliveryDetails(it) }
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
            onClick = { scope.launch { state.confirmVerifyUser() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
fun VerifyUserConfirmFooter(
    state: VerifyUserConfirmState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_lost_code))
            TextButton(onClick = { scope.launch { state.resendCode() } }) {
                Text(stringResource(R.string.amplify_ui_authenticator_button_resend_code))
            }
        }
        TextButton(onClick = { state.skip() }) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_skip))
        }
    }
}
