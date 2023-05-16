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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldScope
import com.amplifyframework.ui.authenticator.forms.FormState
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.forms.MutableFormState
import com.amplifyframework.ui.authenticator.forms.MutablePasswordFieldState
import com.amplifyframework.ui.authenticator.strings.StringResolver

@Composable
internal fun AuthenticatorField(
    fieldConfig: FieldConfig,
    fieldState: MutableFieldState,
    formState: MutableFormState,
    modifier: Modifier = Modifier
) {
    when (fieldConfig) {
        is FieldConfig.Text -> TextInputField(
            modifier = modifier,
            fieldConfig = fieldConfig,
            fieldState = fieldState,
            enabled = formState.enabled
        )
        is FieldConfig.Password -> PasswordInputField(
            modifier = modifier,
            fieldConfig = fieldConfig,
            fieldState = fieldState as MutablePasswordFieldState,
            enabled = formState.enabled
        )
        is FieldConfig.Date -> DateInputField(
            modifier = modifier,
            fieldConfig = fieldConfig,
            fieldState = fieldState,
            enabled = formState.enabled
        )
        is FieldConfig.PhoneNumber -> PhoneInputField(
            modifier = modifier,
            fieldConfig = fieldConfig,
            fieldState = fieldState,
            enabled = formState.enabled
        )
        is FieldConfig.Custom -> {
            val context = FieldScopeImpl(fieldState, formState)
            fieldConfig.content(context)
        }
    }
}

@Composable
internal fun AuthenticatorFieldError(
    fieldConfig: FieldConfig,
    error: FieldError?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val text = error?.let { StringResolver.error(config = fieldConfig, error = it) } ?: ""
        Text(text = text)
    }
}

private class FieldScopeImpl(
    override val fieldState: MutableFieldState,
    override val formState: FormState
) : FieldScope
