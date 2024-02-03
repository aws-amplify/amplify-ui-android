package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.auth.MFAType
import com.amplifyframework.ui.authenticator.ScreenshotTestBase
import com.amplifyframework.ui.authenticator.SignInContinueWithMfaSelectionState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.mockFieldData
import com.amplifyframework.ui.authenticator.mockFieldState
import com.amplifyframework.ui.authenticator.mockForm
import org.junit.Test

class SignInContinueWithMfaSelectionScreenshots : ScreenshotTestBase() {

    @Test
    fun default_state() {
        screenshot {
            SignInContinueWithMfaSelection(mockSignInContinueWithMfaSelectionState())
        }
    }

    private fun mockSignInContinueWithMfaSelectionState() = object : SignInContinueWithMfaSelectionState {
        override val form = mockForm(
            mockFieldData(
                config = FieldConfig.Text(FieldKey.MfaSelection),
                state = mockFieldState(content = "SMS_MFA")
            )
        )
        override val allowedMfaTypes = MFAType.values().toSet()
        override fun moveTo(step: AuthenticatorInitialStep) {}
        override suspend fun continueSignIn() {}
        override val step = AuthenticatorStep.SignInContinueWithMfaSelection
    }
}
