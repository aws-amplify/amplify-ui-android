package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.ui.authenticator.ScreenshotTestBase
import com.amplifyframework.ui.authenticator.SignInContinueWithTotpSetupState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.mockFieldData
import com.amplifyframework.ui.authenticator.mockFieldState
import com.amplifyframework.ui.authenticator.mockForm
import org.junit.Test

class SignInContinueWithTotpSetupScreenshots : ScreenshotTestBase() {

    @Test
    fun default_state() {
        screenshot {
            SignInContinueWithTotpSetup(state = mockSignInContinueWithTotpSetupState())
        }
    }

    @Test
    fun invalid_code() {
        screenshot {
            SignInContinueWithTotpSetup(
                state = mockSignInContinueWithTotpSetupState(
                    confirmationCode = "123456",
                    fieldError = FieldError.ConfirmationCodeIncorrect
                )
            )
        }
    }

    private fun mockSignInContinueWithTotpSetupState(
        confirmationCode: String = "",
        sharedSecret: String = "secret",
        setupUri: String = "https://docs.amplify.aws/android/tools/libraries/",
        fieldError: FieldError? = null
    ) = object : SignInContinueWithTotpSetupState {
        override val form = mockForm(
            mockFieldData(
                config = FieldConfig.Text(FieldKey.ConfirmationCode),
                state = mockFieldState(content = confirmationCode, error = fieldError)
            )
        )
        override val sharedSecret = sharedSecret
        override val setupUri = setupUri
        override fun moveTo(step: AuthenticatorInitialStep) {}
        override suspend fun continueSignIn() {}
        override val step = AuthenticatorStep.SignInContinueWithTotpSetup
    }
}
