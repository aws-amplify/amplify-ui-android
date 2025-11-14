package com.amplifyframework.ui.authenticator.util

import com.amplifyframework.ui.authenticator.AuthenticatorConfiguration
import com.amplifyframework.ui.authenticator.data.AuthenticationFlow
import com.amplifyframework.ui.authenticator.data.PasskeyPrompt
import com.amplifyframework.ui.authenticator.data.UserInfo
import com.amplifyframework.ui.authenticator.enums.SignInSource

// Utility class for checking whether a user should be shown a passkey prompt
internal class PasskeyPromptCheck(private val authProvider: AuthProvider, private val osBuild: OsBuild = OsBuild()) {
    suspend fun shouldPromptForPasskey(userInfo: UserInfo, config: AuthenticatorConfiguration): Boolean {
        // Ensure that userHasPasskey is the last check so that the network request can be short-circuited by
        // the local-only checks.
        val authFlow = config.authenticationFlow
        return authFlow is AuthenticationFlow.UserChoice &&
            deviceSupportsPasskeyCreation() &&
            passkeyPromptsEnabled(userInfo, authFlow) &&
            !userHasPasskey()
    }

    // Passkey creation supported starting with API 28
    private fun deviceSupportsPasskeyCreation() = osBuild.sdkInt >= 28

    // Check whether passkey prompts are enabled by configuration
    private fun passkeyPromptsEnabled(userInfo: UserInfo, authFlow: AuthenticationFlow.UserChoice): Boolean =
        when (userInfo.signInSource) {
            SignInSource.SignIn -> authFlow.passkeyPrompts.afterSignIn == PasskeyPrompt.Always
            SignInSource.AutoSignIn -> authFlow.passkeyPrompts.afterSignUp == PasskeyPrompt.Always
        }

    // Check if the user already has a passkey registered
    private suspend fun userHasPasskey() = when (val result = authProvider.getPasskeys()) {
        is AmplifyResult.Error -> true // Assume user already has passkey on error so we don't incorrectly prompt them
        is AmplifyResult.Success -> result.data.isNotEmpty()
    }
}
