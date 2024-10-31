/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.ui.authenticator.states.SignInContinueWithEmailSetupStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.signInContinueWithEmailSetup
import com.amplifyframework.ui.testing.ComposeTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInContinueWithEmailSetupTest : ComposeTest() {

    @Test
    fun `title is Setup Two-Factor Auth Method`() {
        setContent {
            SignInContinueWithEmailSetup(mockSignInContinueWithEmailSetupState())
        }

        signInContinueWithEmailSetup {
            hasTitle("Add Email for Two-Factor Authentication")
        }
    }

    @Test
    fun `Submit button label is Submit`() {
        setContent {
            SignInContinueWithEmailSetup(mockSignInContinueWithEmailSetupState())
        }

        signInContinueWithEmailSetup {
            hasSubmitButton("Submit")
        }
    }

    @Test
    fun `Submit email address`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithEmailSetup(mockSignInContinueWithEmailSetupState(onSubmit = onSubmit))
        }

        signInContinueWithEmailSetup {
            setEmail("SuperCool@EmailAddress.com")
            clickSubmitButton()
        }

        verify {
            onSubmit("SuperCool@EmailAddress.com")
        }
    }

    @Test
    fun `Go back to sign in`() {
        val onMoveTo = mockk<(AuthenticatorInitialStep) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithEmailSetup(mockSignInContinueWithEmailSetupState(onMoveTo = onMoveTo))
        }

        signInContinueWithEmailSetup {
            clickBackToSignInButton()
        }

        verify {
            onMoveTo(AuthenticatorStep.SignIn)
        }
    }

    private fun mockSignInContinueWithEmailSetupState(
        onSubmit: suspend (email: String) -> Unit = {},
        onMoveTo: (step: AuthenticatorInitialStep) -> Unit = {}
    ) = SignInContinueWithEmailSetupStateImpl(
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )
}
