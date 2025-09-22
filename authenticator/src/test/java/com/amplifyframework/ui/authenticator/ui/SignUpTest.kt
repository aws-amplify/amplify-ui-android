/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.ContentType
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.PasswordError
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignUpState
import com.amplifyframework.ui.authenticator.ui.robots.signUp
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignUpTest : AuthenticatorUiTest() {

    @Test
    fun `title is Create Account`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
        signUp {
            hasTitle("Create Account")
        }
    }

    @Test
    fun `button is Create Account`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
        signUp {
            hasSubmitButton("Create Account")
        }
    }

    @Test
    fun `expected content types are set`() {
        setContent(providedStep = AuthenticatorStep.SignUp) {
            SignUp(state = mockSignUpState())
        }
        signUp {
            hasUsernameContentType(ContentType.NewUsername)
            hasPasswordContentType(ContentType.NewPassword)
            hasConfirmPasswordContentType(ContentType.NewPassword)
        }
    }

    @Test
    fun `cancels autofill values on back to sign in`() {
        val autofillManager = mockk<AutofillManager>(relaxed = true)
        setContent(autofillManager = autofillManager) {
            SignUp(state = mockSignUpState())
        }
        signUp {
            setUsername("foo")
            setPassword("bar")
            clickBackToSignIn()
        }
        verify {
            autofillManager.cancel()
        }
    }

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
    }

    @Test
    @ScreenshotTest
    fun `ready to submit`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
        signUp {
            setUsername("username")
            setPassword("password")
            setConfirmPassword("password")
            setEmail("email@email.com")
        }
    }

    @Test
    @ScreenshotTest
    fun `password visible`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
        signUp {
            setUsername("username")
            setPassword("password")
            setConfirmPassword("password")
            setEmail("email@email.com")

            clickShowPassword(FieldKey.Password)
            clickShowPassword(FieldKey.ConfirmPassword)
        }
    }

    @Test
    @ScreenshotTest
    fun `username exists`() {
        val state = mockSignUpState()
        setContent {
            SignUp(state = state)
        }
        signUp {
            setUsername("username")
            setPassword("password")
            setConfirmPassword("password")
            setEmail("email@email.com")
        }

        state.form.setFieldError(FieldKey.Username, FieldError.FieldValueExists)
    }

    @Test
    @ScreenshotTest
    fun `invalid password`() {
        val state = mockSignUpState()
        setContent {
            SignUp(state = state)
        }
        signUp {
            setUsername("username")
            setPassword("password")
            setConfirmPassword("password")
            setEmail("email@email.com")
        }

        val error = FieldError.InvalidPassword(
            listOf(
                PasswordError.InvalidPasswordLength(10),
                PasswordError.InvalidPasswordMissingUpper,
                PasswordError.InvalidPasswordMissingSpecial,
                PasswordError.InvalidPasswordMissingNumber
            )
        )

        state.form.setFieldError(FieldKey.Password, error)
    }

    @Test
    @ScreenshotTest
    fun `passwords do not match`() {
        val state = mockSignUpState()
        setContent {
            SignUp(state = state)
        }
        signUp {
            setUsername("username")
            setPassword("password")
            setConfirmPassword("password")
            setEmail("email@email.com")
        }
        state.form.setFieldError(FieldKey.ConfirmPassword, FieldError.PasswordsDoNotMatch)
    }

    @Test
    @ScreenshotTest
    fun `invalid email`() {
        val state = mockSignUpState()
        setContent {
            SignUp(state = state)
        }
        signUp {
            setUsername("username")
            setPassword("password")
            setConfirmPassword("password")
            setEmail("email@email.com")
        }
        state.form.setFieldError(FieldKey.Email, FieldError.InvalidFormat)
    }
}
