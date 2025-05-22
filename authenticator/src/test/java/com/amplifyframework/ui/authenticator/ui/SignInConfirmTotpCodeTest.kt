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

import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignInConfirmTotpCodeState
import com.amplifyframework.ui.authenticator.ui.robots.signInConfirmTotpCode
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInConfirmTotpCodeTest : AuthenticatorUiTest() {
    @Test
    fun `title is Enter your one-time passcode`() {
        setContent {
            SignInConfirmTotpCode(state = mockSignInConfirmTotpCodeState())
        }
        signInConfirmTotpCode {
            hasTitle("Enter your one-time passcode")
        }
    }

    @Test
    fun `Submit button label is Submit`() {
        setContent {
            SignInConfirmTotpCode(state = mockSignInConfirmTotpCodeState())
        }
        signInConfirmTotpCode {
            hasSubmitButton("Submit")
        }
    }

    @Test
    fun `passes confirmation code to onSubmit`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)

        setContent {
            SignInConfirmTotpCode(state = mockSignInConfirmTotpCodeState(onSubmit = onSubmit))
        }
        signInConfirmTotpCode {
            setConfirmationCode("123123")
            clickSubmitButton()
        }

        verify {
            onSubmit("123123")
        }
    }

    @Test
    fun `moves back to sign in`() {
        val onMoveTo = mockk<(AuthenticatorInitialStep) -> Unit>(relaxed = true)
        setContent {
            SignInConfirmTotpCode(state = mockSignInConfirmTotpCodeState(onMoveTo = onMoveTo))
        }
        signInConfirmTotpCode {
            clickBackToSignInButton()
        }
        verify {
            onMoveTo(AuthenticatorStep.SignIn)
        }
    }

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            SignInConfirmTotpCode(state = mockSignInConfirmTotpCodeState())
        }
    }

    @Test
    @ScreenshotTest
    fun `invalid code`() {
        val state = mockSignInConfirmTotpCodeState()
        setContent {
            SignInConfirmTotpCode(state = state)
        }
        signInConfirmTotpCode {
            setConfirmationCode("123456")
        }
        state.form.setFieldError(FieldKey.ConfirmationCode, FieldError.ConfirmationCodeIncorrect)
    }
}
