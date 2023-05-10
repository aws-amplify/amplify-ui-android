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

import com.amplifyframework.ui.authenticator.ScreenshotTestBase
import com.amplifyframework.ui.authenticator.SignInState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.mockFieldData
import com.amplifyframework.ui.authenticator.mockFieldState
import com.amplifyframework.ui.authenticator.mockForm
import com.amplifyframework.ui.authenticator.mockPasswordFieldState
import org.junit.Test

class SignInScreenshots : ScreenshotTestBase() {

    @Test
    fun default_state() {
        screenshot {
            SignIn(state = mockSignInState())
        }
    }

    @Test
    fun ready_to_submit() {
        screenshot {
            SignIn(state = mockSignInState(username = "username", password = "password"))
        }
    }

    @Test
    fun password_visible() {
        screenshot {
            SignIn(state = mockSignInState(username = "username", password = "password", passwordVisible = true))
        }
    }

    @Test
    fun username_not_found() {
        screenshot {
            SignIn(state = mockSignInState(username = "username", usernameError = FieldError.NotFound))
        }
    }

    private fun mockSignInState(
        username: String = "",
        password: String = "",
        usernameError: FieldError? = null,
        passwordVisible: Boolean = false
    ) = object : SignInState {
        override fun moveTo(step: AuthenticatorInitialStep) {}
        override suspend fun signIn() {}
        override val form = mockForm(
            mockFieldData(
                config = FieldConfig.Text(FieldKey.Username),
                state = mockFieldState(content = username, error = usernameError)
            ),
            mockFieldData(
                FieldConfig.Password(FieldKey.Password),
                state = mockPasswordFieldState(content = password, visible = passwordVisible)
            )
        )
        override val step = AuthenticatorStep.SignIn
    }
}
