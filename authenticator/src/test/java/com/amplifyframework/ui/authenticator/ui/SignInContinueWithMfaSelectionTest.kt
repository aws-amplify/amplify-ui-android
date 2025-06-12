package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.challengeResponse
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignInContinueWithMfaSelectionState
import com.amplifyframework.ui.authenticator.ui.robots.signInContinueWithMfaSelection
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInContinueWithMfaSelectionTest : AuthenticatorUiTest() {

    @Test
    fun `title is Choose your two-factor authentication method`() {
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState())
        }
        signInContinueWithMfaSelection {
            hasTitle("Choose your two-factor authentication method")
        }
    }

    @Test
    fun `Continue button label is Continue`() {
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState())
        }
        signInContinueWithMfaSelection {
            hasSubmitButton("Continue")
        }
    }

    @Test
    fun `defaults to first MFA type`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState(onSubmit = onSubmit))
        }
        signInContinueWithMfaSelection {
            hasMfaTypeSelected(MFAType.SMS)
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
            hasMfaTypeSelected(MFAType.SMS)
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
            hasMfaTypeSelected(MFAType.TOTP)
            clickSubmitButton()
        }
        verify {
            onSubmit(MFAType.TOTP.challengeResponse)
        }
    }

    @Test
    fun `Submits Email MFA type`() {
        val onSubmit = mockk<(String) -> Unit>(relaxed = true)
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState(onSubmit = onSubmit))
        }
        signInContinueWithMfaSelection {
            selectMfaType(MFAType.EMAIL)
            hasMfaTypeSelected(MFAType.EMAIL)
            clickSubmitButton()
        }
        verify {
            onSubmit(MFAType.EMAIL.challengeResponse)
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

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState())
        }
    }
}
