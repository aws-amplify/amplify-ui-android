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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.VerifyUserState
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import kotlinx.coroutines.launch

@Composable
fun VerifyUser(
    state: VerifyUserState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (VerifyUserState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_verify_user))
    },
    footerContent: @Composable (VerifyUserState) -> Unit = { VerifyUserFooter(it) }
) {
    val scope = rememberCoroutineScope()
    val fieldState = state.form.fields[FieldKey.VerificationAttribute]?.state!!

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        VerifyUserAttributeSelect(
            attributes = state.attributes,
            fieldState = fieldState,
            enabled = state.form.enabled,
            modifier = Modifier.fillMaxWidth()
        )
        AuthenticatorButton(
            onClick = { scope.launch { state.verifyUser() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
internal fun VerifyUserAttributeSelect(
    attributes: List<AuthUserAttribute>,
    fieldState: MutableFieldState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        attributes.forEach { attribute ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { fieldState.content = attribute.key.keyString },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = attribute.key.keyString == fieldState.content,
                    onClick = { fieldState.content = attribute.key.keyString },
                    enabled = enabled
                )

                val label = when (attribute.key) {
                    AuthUserAttributeKey.email() -> R.string.amplify_ui_authenticator_field_label_email
                    else -> R.string.amplify_ui_authenticator_field_label_phone_number
                }

                Text(stringResource(label))
            }
        }
    }
}

@Composable
fun VerifyUserFooter(
    state: VerifyUserState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = { state.skip() }) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_skip))
        }
    }
}
