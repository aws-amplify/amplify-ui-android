package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.auth.MFAType
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.states.SignInContinueWithMfaSelectionStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.signInContinueWithMfaSelection
import com.amplifyframework.ui.authenticator.util.challengeResponse
import com.amplifyframework.ui.testing.ComposeTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInContinueWithMfaSelectionTest : ComposeTest() {

    @Test
    fun `title is Select your preferred Two-Factor Auth method`() {
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState())
        }
        signInContinueWithMfaSelection {
            hasTitle("Select your preferred Two-Factor Auth method")
        }
    }

    @Test
    fun `Submit button label is Submit`() {
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState())
        }
        signInContinueWithMfaSelection {
            hasSubmitButton("Submit")
        }
    }

    @Test
    fun `Requires MFA Type selection`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState(onSubmit = onSubmit))
        }
        signInContinueWithMfaSelection {
            clickSubmitButton()
        }
        verify(exactly = 0) {
            onSubmit(any())
        }
    }

    @Test
    fun `Submits SMS MFA type`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState(onSubmit = onSubmit))
        }
        signInContinueWithMfaSelection {
            selectMfaType(MFAType.SMS)
            clickSubmitButton()
        }
        verify {
            onSubmit(MFAType.SMS.challengeResponse)
        }
    }

    @Test
    fun `Submits TOTP MFA type`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState(onSubmit = onSubmit))
        }
        signInContinueWithMfaSelection {
            selectMfaType(MFAType.TOTP)
            clickSubmitButton()
        }
        verify {
            onSubmit(MFAType.TOTP.challengeResponse)
        }
    }

    @Test
    fun `moves back to sign in`() {
        val onMoveTo = mockk<(AuthenticatorInitialStep) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithMfaSelection(state = mockSignInContinueWithMfaSelectionState(onMoveTo = onMoveTo))
        }
        signInContinueWithMfaSelection {
            clickBackToSignInButton()
        }
        verify {
            onMoveTo(AuthenticatorStep.SignIn)
        }
    }

    private fun mockSignInContinueWithMfaSelectionState(
        allowedMfaTypes: Set<MFAType> = MFAType.values().toSet(),
        onSubmit: (String) -> Unit = {},
        onMoveTo: (AuthenticatorInitialStep) -> Unit = {}
    ) = SignInContinueWithMfaSelectionStateImpl(
        allowedMfaTypes = allowedMfaTypes,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )
}
