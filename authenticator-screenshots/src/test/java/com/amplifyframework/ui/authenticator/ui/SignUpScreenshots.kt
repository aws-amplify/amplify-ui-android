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
import com.amplifyframework.ui.authenticator.SignUpState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.PasswordError
import com.amplifyframework.ui.authenticator.mockFieldData
import com.amplifyframework.ui.authenticator.mockFieldState
import com.amplifyframework.ui.authenticator.mockForm
import com.amplifyframework.ui.authenticator.mockPasswordFieldState
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class SignUpScreenshots : ScreenshotTestBase() {

    @Test
    fun default_state() {
        screenshot {
            SignUp(state = mockSignUpState())
        }
    }

    @Test
    fun ready_to_submit() {
        screenshot {
            SignUp(
                state = mockSignUpState(
                    username = "username",
                    password = "password",
                    confirmPassword = "password",
                    email = "email@email.com"
                )
            )
        }
    }

    @Test
    fun password_visible() {
        screenshot {
            SignUp(
                state = mockSignUpState(
                    username = "username",
                    password = "password",
                    confirmPassword = "password",
                    email = "email@email.com",
                    passwordVisible = true
                )
            )
        }
    }

    @Test
    fun username_exists() {
        screenshot {
            SignUp(
                state = mockSignUpState(
                    username = "username",
                    password = "password",
                    confirmPassword = "password",
                    email = "email@email.com",
                    usernameError = FieldError.FieldValueExists
                )
            )
        }
    }

    @Test
    fun invalid_password() {
        val error = mockk<FieldError.InvalidPassword> {
            every { errors } returns listOf(
                PasswordError.InvalidPasswordLength(10),
                PasswordError.InvalidPasswordMissingUpper,
                PasswordError.InvalidPasswordMissingSpecial,
                PasswordError.InvalidPasswordMissingNumber
            )
        }

        screenshot {
            SignUp(
                state = mockSignUpState(
                    username = "username",
                    password = "password",
                    confirmPassword = "password",
                    email = "email@email.com",
                    passwordError = error
                )
            )
        }
    }

    @Test
    fun passwords_do_not_match() {
        screenshot {
            SignUp(
                state = mockSignUpState(
                    username = "username",
                    password = "password",
                    confirmPassword = "password2",
                    email = "email@email.com",
                    confirmPasswordError = FieldError.PasswordsDoNotMatch
                )
            )
        }
    }

    @Test
    fun invalid_email() {
        screenshot {
            SignUp(
                state = mockSignUpState(
                    username = "username",
                    password = "password",
                    confirmPassword = "password2",
                    email = "email@email.com",
                    emailError = FieldError.InvalidFormat
                )
            )
        }
    }

    private fun mockSignUpState(
        username: String = "",
        password: String = "",
        confirmPassword: String = "",
        email: String = "",
        usernameError: FieldError? = null,
        passwordError: FieldError? = null,
        confirmPasswordError: FieldError? = null,
        emailError: FieldError? = null,
        passwordVisible: Boolean = false
    ) = object : SignUpState {
        override fun moveTo(step: AuthenticatorInitialStep) {}
        override suspend fun signUp() {}
        override val form = mockForm(
            mockFieldData(
                config = FieldConfig.Text(FieldKey.Username),
                state = mockFieldState(content = username, error = usernameError)
            ),
            mockFieldData(
                config = FieldConfig.Password(FieldKey.Password),
                state = mockPasswordFieldState(content = password, error = passwordError, visible = passwordVisible)
            ),
            mockFieldData(
                config = FieldConfig.Password(FieldKey.ConfirmPassword),
                state = mockPasswordFieldState(
                    content = confirmPassword,
                    error = confirmPasswordError,
                    visible = passwordVisible
                )
            ),
            mockFieldData(
                config = FieldConfig.Text(FieldKey.Email),
                state = mockFieldState(content = email, error = emailError)
            )
        )
        override val step = AuthenticatorStep.SignUp
    }
}
