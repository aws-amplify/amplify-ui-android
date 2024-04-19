/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator

import android.app.Application
import com.amplifyframework.auth.AuthUserAttributeKey.email
import com.amplifyframework.auth.AuthUserAttributeKey.emailVerified
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.ui.authenticator.auth.VerificationMechanism
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.util.AmplifyResult
import com.amplifyframework.ui.authenticator.util.AmplifyResult.Success
import com.amplifyframework.ui.authenticator.util.AuthConfigurationResult
import com.amplifyframework.ui.authenticator.util.AuthProvider
import com.amplifyframework.ui.testing.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [AuthenticatorViewModel] class
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorViewModelTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val application = mockk<Application>(relaxed = true)
    private val authProvider = mockk<AuthProvider>(relaxed = true)

    private val viewModel = AuthenticatorViewModel(application, authProvider)

    @Before
    fun setup() {
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration()
        coEvery { authProvider.getCurrentUser() } returns Success(mockUser())
    }

//region start tests

    @Test
    fun `start only executes once`() = runTest {
        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        // fetchAuthSession only called by the first start
        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
        }
    }

    @Test
    fun `missing configuration results in an error`() = runTest {
        coEvery { authProvider.getConfiguration() } returns AuthConfigurationResult.Missing

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 0) { authProvider.fetchAuthSession() }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `invalid configuration results in an error`() = runTest {
        coEvery { authProvider.getConfiguration() } returns AuthConfigurationResult.Invalid("Invalid")

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 0) { authProvider.fetchAuthSession() }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `fetchAuthSession error during start results in an error`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Error(mockAuthException())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) { authProvider.fetchAuthSession() }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `getCurrentUser error during start results in an error`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns AmplifyResult.Error(mockAuthException())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
            authProvider.getCurrentUser()
        }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `when already signed in during start the initial state should be signed in`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns Success(mockAuthUser())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
            authProvider.getCurrentUser()
        }
        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `initial step is SignIn`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))
        advanceUntilIdle()

        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
    }

//endregion
//region signIn tests

    @Test
    fun `TOTPSetup next step shows error if totpSetupDetails is null`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                totpSetupDetails = null
            )
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `TOTPSetup next step shows SignInContinueWithTotpSetup screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                totpSetupDetails = mockk(relaxed = true)
            )
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignInContinueWithTotpSetup
    }

    @Test
    fun `TOTP Code next step shows the SignInConfirmTotpCode screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE)
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignInConfirmTotpCode
    }

    @Test
    fun `MFA selection next step shows error if allowedMFATypes is null`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                allowedMFATypes = null
            )
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `MFA selection next step shows error if allowedMFATypes is empty`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                allowedMFATypes = emptySet()
            )
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `MFA Selection next step shows the SignInContinueWithMfaSelection screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                allowedMFATypes = setOf(MFAType.TOTP, MFAType.SMS)
            )
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignInContinueWithMfaSelection
    }

    @Test
    fun `user attribute verification screen is shown if user has no verified attributes`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(mockSignInResult())
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration(
            verificationMechanisms = setOf(VerificationMechanism.Email)
        )
        coEvery { authProvider.fetchUserAttributes() } returns Success(
            mockUserAttributes(email() to "email", emailVerified() to "false")
        )

        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.signIn("username", "password")

        viewModel.currentStep shouldBe AuthenticatorStep.VerifyUser
    }

    @Test
    fun `user attribute verification screen is not shown if user has verified attributes`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(mockSignInResult())
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration(
            verificationMechanisms = setOf(VerificationMechanism.Email)
        )
        coEvery { authProvider.fetchUserAttributes() } returns Success(
            mockUserAttributes(email() to "email", emailVerified() to "true") // email is already verified
        )

        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.signIn("username", "password")

        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `user attribute verification screen is not shown if there are no verification mechanisms`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(mockSignInResult())
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration(
            verificationMechanisms = emptySet() // no verification mechanisms
        )
        coEvery { authProvider.fetchUserAttributes() } returns Success(
            mockUserAttributes(email() to "email", emailVerified() to "false")
        )

        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.signIn("username", "password")

        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `user attribute verification screen is not shown if cannot fetch user attributes`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(mockSignInResult())
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration(
            verificationMechanisms = setOf(VerificationMechanism.Email)
        )
        // cannot fetch user attributes
        coEvery { authProvider.fetchUserAttributes() } returns AmplifyResult.Error(mockk(relaxed = true))

        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.signIn("username", "password")

        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `user attribute verification screen is not shown if user does not have the required attributes`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(mockSignInResult())
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration(
            verificationMechanisms = setOf(VerificationMechanism.Email)
        )
        coEvery { authProvider.fetchUserAttributes() } returns Success(mockUserAttributes()) // no email attribute

        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.signIn("username", "password")

        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

//endregion
//region helpers
    private val AuthenticatorViewModel.currentStep: AuthenticatorStep
        get() = stepState.value.step
//endregion
}
