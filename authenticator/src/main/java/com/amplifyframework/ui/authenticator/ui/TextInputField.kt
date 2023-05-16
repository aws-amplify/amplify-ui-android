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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.strings.StringResolver

@Composable
internal fun TextInputField(
    fieldConfig: FieldConfig.Text,
    fieldState: MutableFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current

    val label = StringResolver.label(fieldConfig)
    val hint = StringResolver.hint(fieldConfig)
    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        value = fieldState.content,
        onValueChange = { fieldState.content = it.take(fieldConfig.maxLength) },
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        isError = fieldState.error != null,
        maxLines = fieldConfig.maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = fieldConfig.keyboardType,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) }
        ),
        supportingText = {
            AuthenticatorFieldError(
                modifier = Modifier.fillMaxWidth(),
                fieldConfig = fieldConfig,
                error = fieldState.error
            )
        }
    )
}
