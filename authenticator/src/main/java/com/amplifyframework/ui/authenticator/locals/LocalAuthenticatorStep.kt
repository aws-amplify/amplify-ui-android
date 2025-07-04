package com.amplifyframework.ui.authenticator.locals

import androidx.compose.runtime.compositionLocalOf
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep

internal val LocalAuthenticatorStep = compositionLocalOf<AuthenticatorStep> { AuthenticatorStep.Loading }