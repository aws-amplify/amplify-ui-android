package com.amplifyframework.ui.authenticator.ui

import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.ContentType
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockPasswordResetConfirmState
import com.amplifyframework.ui.authenticator.ui.robots.passwordResetConfirm
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PasswordResetConfirmTest : AuthenticatorUiTest() {
    @Test
    fun `title is reset password`() {
        setContent {
            PasswordResetConfirm(state = mockPasswordResetConfirmState())
        }
        passwordResetConfirm {
            hasTitle("Reset Password")
        }
    }

    @Test
    fun `has expected content types set`() {
        setContent(providedStep = AuthenticatorStep.PasswordResetConfirm) {
            PasswordResetConfirm(state = mockPasswordResetConfirmState())
        }
        passwordResetConfirm {
            hasPasswordContentType(ContentType.NewPassword)
            hasConfirmPasswordContentType(ContentType.NewPassword)
        }
    }

    @Test
    fun `cancels autofill values on back to sign in`() {
        val autofillManager = mockk<AutofillManager>(relaxed = true)
        setContent(autofillManager = autofillManager) {
            PasswordResetConfirm(state = mockPasswordResetConfirmState())
        }
        passwordResetConfirm {
            setConfirmationCode("123456")
            setPassword("newPassword")
            setConfirmPassword("newPassword")
            clickBackToSignIn()
        }
        verify {
            autofillManager.cancel()
        }
    }

    @Test
    fun `moves back to sign in`() {
        val onMoveTo = mockk<(AuthenticatorInitialStep) -> Unit>(relaxed = true)
        setContent {
            PasswordResetConfirm(state = mockPasswordResetConfirmState(onMoveTo = onMoveTo))
        }
        passwordResetConfirm {
            clickBackToSignIn()
        }
        verify {
            onMoveTo(AuthenticatorStep.SignIn)
        }
    }

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            PasswordResetConfirm(state = mockPasswordResetConfirmState())
        }
    }

    @Test
    @ScreenshotTest
    fun `incorrect confirmation code`() {
        val state = mockPasswordResetConfirmState()
        setContent {
            PasswordResetConfirm(state = state)
        }
        state.form.setFieldError(FieldKey.ConfirmationCode, FieldError.ConfirmationCodeIncorrect)
    }

    @Test
    @ScreenshotTest
    fun `passwords do not match`() {
        val state = mockPasswordResetConfirmState()
        setContent {
            PasswordResetConfirm(state = state)
        }
        state.form.setFieldError(FieldKey.ConfirmPassword, FieldError.PasswordsDoNotMatch)
    }
}
