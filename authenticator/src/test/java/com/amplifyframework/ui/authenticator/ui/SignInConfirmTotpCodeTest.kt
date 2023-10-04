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
import com.amplifyframework.ui.authenticator.states.SignInConfirmTotpCodeStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.signInConfirmTotpCode
import com.amplifyframework.ui.testing.ComposeTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInConfirmTotpCodeTest : ComposeTest() {
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

    private fun mockSignInConfirmTotpCodeState(
        onSubmit: (String) -> Unit = { },
        onMoveTo: (AuthenticatorInitialStep) -> Unit = { }
    ) = SignInConfirmTotpCodeStateImpl(
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )
}
