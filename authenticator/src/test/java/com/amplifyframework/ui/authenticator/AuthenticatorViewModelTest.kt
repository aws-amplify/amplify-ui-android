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
import app.cash.turbine.test
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.util.AmplifyResult
import com.amplifyframework.ui.authenticator.util.AuthProvider
import com.amplifyframework.ui.testing.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

//region start tests

    @Test
    fun `start only executes once`() = runTest {
        viewModel.start(mockAuthConfiguration())
        viewModel.start(mockAuthConfiguration())
        advanceUntilIdle()

        // fetchAuthSession only called by the first start
        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
        }
    }

    @Test
    fun `missing configuration results in an error`() = runTest {
        coEvery { authProvider.getConfiguration() } returns null

        viewModel.start(mockAuthConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 0) { authProvider.fetchAuthSession() }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `fetchAuthSession error during start results in an error`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Error(mockAuthException())

        viewModel.start(mockAuthConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) { authProvider.fetchAuthSession() }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `getCurrentUser error during start results in an error`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns AmplifyResult.Error(mockAuthException())

        viewModel.start(mockAuthConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
            authProvider.getCurrentUser()
        }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `when already signed in during start the initial state should be signed in`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns AmplifyResult.Success(mockAuthUser())

        viewModel.start(mockAuthConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
            authProvider.getCurrentUser()
        }
        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `initial step is SignIn`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Success(mockAuthSession(isSignedIn = false))

        viewModel.start(mockAuthConfiguration(initialStep = AuthenticatorStep.SignIn))
        advanceUntilIdle()

        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
    }

//endregion
//region signIn tests

    @Test
    fun `TOTPSetup next step is unsupported`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns AmplifyResult.Success(
            mockSignInResult(signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP)
        )

        viewModel.start(mockAuthConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.events.test {
            viewModel.signIn("username", "password")
            awaitItem().shouldBeError(causeMessage = "Authenticator does not yet support TOTP workflows.")
        }
    }

    @Test
    fun `TOTP Code next step is unsupported`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns AmplifyResult.Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE)
        )

        viewModel.start(mockAuthConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.events.test {
            viewModel.signIn("username", "password")
            awaitItem().shouldBeError(causeMessage = "Authenticator does not yet support TOTP workflows.")
        }
    }

    @Test
    fun `MFA Selection next step is unsupported`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns AmplifyResult.Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns AmplifyResult.Success(
            mockSignInResult(signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION)
        )

        viewModel.start(mockAuthConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.events.test {
            viewModel.signIn("username", "password")
            awaitItem().shouldBeError(causeMessage = "Authenticator does not yet support TOTP workflows.")
        }
    }

//endregion
//region helpers
    private val AuthenticatorViewModel.currentStep: AuthenticatorStep
        get() = stepState.value.step
//endregion
}
