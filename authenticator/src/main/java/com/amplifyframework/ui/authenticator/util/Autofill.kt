package com.amplifyframework.ui.authenticator.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.locals.LocalAuthenticatorStep

@Composable
@ReadOnlyComposable
internal fun Modifier.contentTypeForKey(key: FieldKey): Modifier {
    val derivedContentType = key.deriveContentType()
    return this.semantics { if (derivedContentType != null) contentType = derivedContentType }
}

@Composable
@ReadOnlyComposable
internal fun FieldKey.deriveContentType(): ContentType? {
    val step = LocalAuthenticatorStep.current
    return when (this) {
        is FieldKey.Username -> if (step.isNewUsername()) ContentType.NewUsername else ContentType.Username
        is FieldKey.Password -> if (step.isNewPassword()) ContentType.NewPassword else ContentType.Password
        is FieldKey.ConfirmPassword -> ContentType.NewPassword
        is FieldKey.ConfirmationCode -> ContentType.SmsOtpCode
        is FieldKey.Email -> ContentType.EmailAddress
        is FieldKey.PhoneNumber -> ContentType.PhoneNumber
        else -> null
    }
}

private fun AuthenticatorStep.isNewUsername() = this is AuthenticatorStep.SignUp
private fun AuthenticatorStep.isNewPassword() =
    this is AuthenticatorStep.SignUp || this is AuthenticatorStep.PasswordResetConfirm
