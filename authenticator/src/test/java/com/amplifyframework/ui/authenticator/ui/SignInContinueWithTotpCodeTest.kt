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

import android.content.ClipboardManager
import android.content.Context
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.states.SignInContinueWithTotpSetupStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.signInContinueWithTotpSetup
import com.amplifyframework.ui.testing.ComposeTest
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class SignInContinueWithTotpCodeTest : ComposeTest() {
    @Test
    fun `title is Enable Two-Factor Auth`() {
        setContent {
            SignInContinueWithTotpSetup(state = mockSignInContinueWithTotpSetupState())
        }
        signInContinueWithTotpSetup {
            hasTitle("Enable Two-Factor Auth")
        }
    }

    @Test
    fun `Submit button label is Submit`() {
        setContent {
            SignInContinueWithTotpSetup(state = mockSignInContinueWithTotpSetupState())
        }
        signInContinueWithTotpSetup {
            hasSubmitButton("Submit")
        }
    }

    @Test
    fun `passes confirmation code to onSubmit`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)

        setContent {
            SignInContinueWithTotpSetup(state = mockSignInContinueWithTotpSetupState(onSubmit = onSubmit))
        }
        signInContinueWithTotpSetup {
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
            SignInContinueWithTotpSetup(state = mockSignInContinueWithTotpSetupState(onMoveTo = onMoveTo))
        }
        signInContinueWithTotpSetup {
            clickBackToSignInButton()
        }
        verify {
            onMoveTo(AuthenticatorStep.SignIn)
        }
    }

    @Test
    fun `copies shared secret to clipboard`() {
        setContent {
            SignInContinueWithTotpSetup(state = mockSignInContinueWithTotpSetupState(sharedSecret = "secret!"))
        }
        signInContinueWithTotpSetup {
            clickCopyKeyButton()
        }

        getClipboardContent() shouldBe "secret!"
    }

    private fun mockSignInContinueWithTotpSetupState(
        sharedSecret: String = "",
        setupUri: String = "",
        onSubmit: (String) -> Unit = { },
        onMoveTo: (AuthenticatorInitialStep) -> Unit = { }
    ) = SignInContinueWithTotpSetupStateImpl(
        sharedSecret = sharedSecret,
        setupUri = setupUri,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    private fun getClipboardContent(): String? {
        val clipboardManager =
            RuntimeEnvironment.getApplication().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
    }
}
