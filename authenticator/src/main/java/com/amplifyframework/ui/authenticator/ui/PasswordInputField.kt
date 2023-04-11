/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.strings.StringResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordInputField(
    fieldConfig: FieldConfig.Password,
    fieldState: MutableFieldState,
    enabled: Boolean,
    hidden: Boolean,
    onClickHideShow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val label = StringResolver.label(fieldConfig)
    val hint = StringResolver.hint(fieldConfig)
    val transformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None
    val trailingIcon = getTrailingIcon(hidden = hidden, onClick = onClickHideShow)

    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        value = fieldState.content,
        onValueChange = { fieldState.content = it },
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        visualTransformation = transformation,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = fieldConfig.keyboardType,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) }
        ),
        trailingIcon = trailingIcon,
        isError = fieldState.error != null,
        supportingText = {
            AuthenticatorFieldError(
                modifier = Modifier.fillMaxWidth(),
                fieldConfig = fieldConfig,
                error = fieldState.error
            )
        }
    )
}

private fun getTrailingIcon(hidden: Boolean, onClick: () -> Unit): @Composable (() -> Unit) {
    return {
        val icon = when (hidden) {
            true -> R.drawable.ic_authenticator_visible
            false -> R.drawable.ic_authenticator_invisible
        }
        val contentDescription = when (hidden) {
            true -> R.string.authenticator_field_a11y_password_hide
            false -> R.string.authenticator_field_a11y_password_show
        }
        IconButton(onClick = onClick) {
            Icon(painter = painterResource(icon), contentDescription = stringResource(contentDescription))
        }
    }
}
