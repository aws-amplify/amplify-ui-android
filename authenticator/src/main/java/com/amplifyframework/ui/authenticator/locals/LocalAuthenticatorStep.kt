package com.amplifyframework.ui.authenticator.locals

import androidx.compose.runtime.compositionLocalOf
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep

/**
 * This composition local supplies the current AuthenticatorStep. This allows descendant composables to tailor
 * their content to specific steps.
 */
internal val LocalAuthenticatorStep = compositionLocalOf<AuthenticatorStep> { AuthenticatorStep.Loading }
