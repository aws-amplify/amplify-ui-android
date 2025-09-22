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
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignInState
import com.amplifyframework.ui.authenticator.ui.robots.signIn
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInTest : AuthenticatorUiTest() {

    @Test
    fun `title is Sign In`() {
        setContent {
            SignIn(state = mockSignInState())
        }
        signIn {
            hasTitle("Sign In")
        }
    }

    @Test
    fun `button is Sign In`() {
        setContent {
            SignIn(state = mockSignInState())
        }
        signIn {
            hasSubmitButton("Sign In")
        }
    }

    @Test
    fun `has expected content types set`() {
        setContent(providedStep = AuthenticatorStep.SignIn) {
            SignIn(state = mockSignInState())
        }
        signIn {
            hasUsernameContentType(ContentType.Username)
            hasPasswordContentType(ContentType.Password)
        }
    }

    @Test
    fun `cancels autofill values on create account`() {
        val autofillManager = mockk<AutofillManager>(relaxed = true)
        setContent(autofillManager = autofillManager) {
            SignIn(state = mockSignInState())
        }
        signIn {
            setUsername("foo")
            setPassword("bar")
            clickCreateAccount()
        }
        verify {
            autofillManager.cancel()
        }
    }

    @Test
    fun `cancels autofill values on forgot password`() {
        val autofillManager = mockk<AutofillManager>(relaxed = true)
        setContent(autofillManager = autofillManager) {
            SignIn(state = mockSignInState())
        }
        signIn {
            setUsername("foo")
            setPassword("bar")
            clickForgotPassword()
        }
        verify {
            autofillManager.cancel()
        }
    }

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            SignIn(state = mockSignInState())
        }
    }

    @Test
    @ScreenshotTest
    fun `ready to submit`() {
        setContent {
            SignIn(state = mockSignInState())
        }
        signIn {
            setUsername("username")
            setPassword("password")
        }
    }

    @Test
    @ScreenshotTest
    fun `password visible`() {
        setContent {
            SignIn(state = mockSignInState())
        }
        signIn {
            setUsername("username")
            setPassword("password")
            clickShowPassword()
        }
    }

    @Test
    @ScreenshotTest
    fun `username not found`() {
        val state = mockSignInState()
        setContent {
            SignIn(state = state)
        }
        signIn {
            setUsername("username")
        }
        state.form.setFieldError(FieldKey.Username, FieldError.NotFound)
    }
}
