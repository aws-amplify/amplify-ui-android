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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.ErrorState
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.strings.StringResolver
import kotlinx.coroutines.launch

/**
 * The content displayed when Authenticator is in the ErrorState
 */
@Composable
fun AuthenticatorError(state: ErrorState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val message = StringResolver.error(state.error)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        AnimatedVisibility(state.canRetry) {
            var retrying by remember { mutableStateOf(false) }
            TextButton(
                onClick = {
                    scope.launch {
                        retrying = true
                        state.retry()
                        retrying = false
                    }
                },
                enabled = !retrying
            ) {
                Text(stringResource(R.string.amplify_ui_authenticator_button_retry))
            }
        }
    }
}
