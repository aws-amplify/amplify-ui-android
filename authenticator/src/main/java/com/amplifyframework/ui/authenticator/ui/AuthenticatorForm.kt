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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.forms.MutableFormState

/**
 * Displays the input fields for a [MutableFormState].
 * @param state The [MutableFormState] holding the form's state.
 * @param modifier The Modifier for the composable.
 */
@Composable
internal fun AuthenticatorForm(
    state: MutableFormState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.fields.values.forEach { field ->
            AuthenticatorField(
                modifier = Modifier.fillMaxWidth().testTag(field.config.key.toString()),
                fieldConfig = field.config,
                fieldState = field.state,
                formState = state
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
    }
}

/**
 * The button displayed in Authenticator.
 * @param onClick The click handler for the button
 * @param loading True to show the [loadingIndicator] content, false to show the button label.
 * @param modifier The [Modifier] for the composable.
 * @param label The label for the button
 * @param loadingIndicator The content to show when loading.
 */
@Composable
internal fun AuthenticatorButton(
    onClick: () -> Unit,
    loading: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.amplify_ui_authenticator_button_submit),
    loadingIndicator: @Composable () -> Unit = { LoadingIndicator() }
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = !loading
    ) {
        if (loading) {
            loadingIndicator()
        } else {
            Text(label)
        }
    }
}
