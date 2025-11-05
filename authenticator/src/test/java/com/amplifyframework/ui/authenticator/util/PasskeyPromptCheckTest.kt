package com.amplifyframework.ui.authenticator.util

import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.ui.authenticator.AuthenticatorConfiguration
import com.amplifyframework.ui.authenticator.data.AuthenticationFlow
import com.amplifyframework.ui.authenticator.data.PasskeyPrompt
import com.amplifyframework.ui.authenticator.data.PasskeyPrompts
import com.amplifyframework.ui.authenticator.data.UserInfo
import com.amplifyframework.ui.authenticator.enums.SignInSource
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PasskeyPromptCheckTest {

    private val authProvider = mockk<AuthProvider> {
        coEvery { getAvailableAuthFactors() } returns
            AmplifyResult.Success(listOf(AuthFactorType.PASSWORD_SRP, AuthFactorType.SMS_OTP))
    }
    private val osBuild = mockk<OsBuild> {
        every { sdkInt } returns 30
    }
    private val passkeyPromptCheck = PasskeyPromptCheck(authProvider, osBuild)

    @Test
    fun `shouldPromptForPasskey returns false when auth flow is not UserChoice`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration(authFlow = AuthenticationFlow.Password)

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeFalse()
    }

    @Test
    fun `shouldPromptForPasskey returns false when passkey prompts are disabled for SignIn`() = runTest {
        val userInfo = mockUserInfo(source = SignInSource.SignIn)
        val config = mockAuthenticatorConfiguration(
            authFlow = AuthenticationFlow.UserChoice(
                passkeyPrompts = PasskeyPrompts(afterSignIn = PasskeyPrompt.Never)
            )
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeFalse()
    }

    @Test
    fun `shouldPromptForPasskey returns false when passkey prompts are disabled for AutoSignIn`() = runTest {
        val userInfo = mockUserInfo(source = SignInSource.AutoSignIn)
        val config = mockAuthenticatorConfiguration(
            authFlow = AuthenticationFlow.UserChoice(
                passkeyPrompts = PasskeyPrompts(afterSignUp = PasskeyPrompt.Never)
            )
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeFalse()
    }

    @Test
    fun `shouldPromptForPasskey returns false when user already has passkey`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration()

        coEvery { authProvider.getAvailableAuthFactors() } returns AmplifyResult.Success(
            listOf(AuthFactorType.PASSWORD, AuthFactorType.WEB_AUTHN)
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeFalse()
    }

    @Test
    fun `shouldPromptForPasskey returns false when getAvailableAuthFactors returns error`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration()

        coEvery { authProvider.getAvailableAuthFactors() } returns AmplifyResult.Error(
            UnknownException("Network error")
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeFalse()
    }

    @Test
    fun `shouldPromptForPasskey returns false when os version is below 28`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration()

        every { osBuild.sdkInt } returns 27

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeFalse()
    }

    @Test
    fun `shouldPromptForPasskey returns true when os version is 28`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration()

        every { osBuild.sdkInt } returns 28

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeTrue()
    }

    @Test
    fun `shouldPromptForPasskey returns true when auth factor list is empty`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration()

        coEvery { authProvider.getAvailableAuthFactors() } returns AmplifyResult.Success(emptyList())

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeTrue()
    }

    @Test
    fun `shouldPromptForPasskey returns true when auth factors don't have webAuthn`() = runTest {
        val userInfo = mockUserInfo()
        val config = mockAuthenticatorConfiguration()

        coEvery { authProvider.getAvailableAuthFactors() } returns AmplifyResult.Success(
            AuthFactorType.entries - AuthFactorType.WEB_AUTHN
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeTrue()
    }

    @Test
    fun `shouldPromptForPasskey returns true for autoSignIn`() = runTest {
        val userInfo = mockUserInfo(source = SignInSource.AutoSignIn)
        val config = mockAuthenticatorConfiguration(
            authFlow = AuthenticationFlow.UserChoice(
                passkeyPrompts = PasskeyPrompts(
                    afterSignIn = PasskeyPrompt.Never,
                    afterSignUp = PasskeyPrompt.Always
                )
            )
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeTrue()
    }

    @Test
    fun `shouldPromptForPasskey returns true for normal signIn`() = runTest {
        val userInfo = mockUserInfo(source = SignInSource.SignIn)
        val config = mockAuthenticatorConfiguration(
            authFlow = AuthenticationFlow.UserChoice(
                passkeyPrompts = PasskeyPrompts(
                    afterSignIn = PasskeyPrompt.Always,
                    afterSignUp = PasskeyPrompt.Never
                )
            )
        )

        val result = passkeyPromptCheck.shouldPromptForPasskey(userInfo, config)
        result.shouldBeTrue()
    }

    private fun mockUserInfo(source: SignInSource = SignInSource.SignIn) = mockk<UserInfo> {
        every { signInSource } returns source
    }

    private fun mockAuthenticatorConfiguration(authFlow: AuthenticationFlow = AuthenticationFlow.UserChoice()) =
        mockk<AuthenticatorConfiguration> {
            every { authenticationFlow } returns authFlow
        }
}
