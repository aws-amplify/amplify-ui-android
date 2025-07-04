/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.ui.authenticator.testUtil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.locals.LocalAuthenticatorStep
import com.amplifyframework.ui.authenticator.theme.AmplifyTheme
import com.amplifyframework.ui.testing.ComposeTest

abstract class AuthenticatorUiTest : ComposeTest() {
    fun setContent(
        providedStep: AuthenticatorStep? = null,
        autofillManager: AutofillManager? = null,
        content: @Composable () -> Unit
    ) = setContent {
        val step = providedStep ?: AuthenticatorStep.Loading
        CompositionLocalProvider(LocalAutofillManager provides autofillManager) {
            CompositionLocalProvider(LocalAuthenticatorStep provides step) {
                content()
            }
        }
    }

    override fun setContent(content: @Composable () -> Unit) {
        super.setContent {
            AmplifyTheme {
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}
