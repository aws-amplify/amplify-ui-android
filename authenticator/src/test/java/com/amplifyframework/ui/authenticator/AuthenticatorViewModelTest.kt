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
import androidx.lifecycle.viewmodel.compose.viewModel
import aws.smithy.kotlin.runtime.http.HttpException
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthUserAttributeKey.email
import com.amplifyframework.auth.AuthUserAttributeKey.emailVerified
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.exceptions.service.LimitExceededException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.ui.authenticator.auth.VerificationMechanism
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.util.AmplifyResult.Error
import com.amplifyframework.ui.authenticator.util.AmplifyResult.Success
import com.amplifyframework.ui.authenticator.util.AuthConfigurationResult
import com.amplifyframework.ui.authenticator.util.AuthProvider
import com.amplifyframework.ui.authenticator.util.LimitExceededMessage
import com.amplifyframework.ui.authenticator.util.NetworkErrorMessage
import com.amplifyframework.ui.testing.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.net.UnknownHostException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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

    private val hubFlow = MutableSharedFlow<HubEvent<*>>(replay = 0)

    @Before
    fun setup() {
        coEvery { authProvider.getConfiguration() } returns mockAmplifyAuthConfiguration()
        coEvery { authProvider.getCurrentUser() } returns Success(mockUser())
        coEvery { authProvider.authStatusEvents() } returns hubFlow
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
        coEvery { authProvider.fetchAuthSession() } returns Error(mockAuthException())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) { authProvider.fetchAuthSession() }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `fetchAuthSession error can be retried`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns
            Error(mockAuthException()) andThen Success(mockAuthSession())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        val state = viewModel.stepState.value.shouldBeInstanceOf<ErrorState>()
        state.retry()
        advanceUntilIdle()

        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
        coVerify(exactly = 2) {
            authProvider.fetchAuthSession()
        }
    }

    @Test
    fun `getCurrentUser error during start results in an error`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns Error(mockAuthException())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
            authProvider.getCurrentUser()
        }
        viewModel.currentStep shouldBe AuthenticatorStep.Error
    }

    @Test
    fun `getCurrentUser error with session expired exception during start results in being signed out`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns Error(SessionExpiredException())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
            authProvider.getCurrentUser()
            authProvider.signOut()
        }
    }

    @Test
    fun `getCurrentUser error can be retried`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = true))
        coEvery { authProvider.getCurrentUser() } returns Error(mockAuthException()) andThen Success(mockAuthUser())

        viewModel.start(mockAuthenticatorConfiguration())
        advanceUntilIdle()

        val state = viewModel.stepState.value.shouldBeInstanceOf<ErrorState>()
        state.retry()
        advanceUntilIdle()

        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
        coVerify(exactly = 1) {
            authProvider.fetchAuthSession()
        }
        coVerify(exactly = 2) {
            authProvider.getCurrentUser()
        }
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
    fun `SMS MFA Code next step shows the SignInConfirmMfa screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE)
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignInConfirmMfa
    }

    @Test
    fun `Custom Challenge next step shows the SignInConfirmCustomAuth screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE)
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignInConfirmCustomAuth
    }

    @Test
    fun `New Password next step shows the SignInConfirmNewPassword screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD)
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignInConfirmNewPassword
    }

    @Test
    fun `Confirm SignUp next step, get error from resendSignUpCode, stays in SignIn screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_UP)
        )
        coEvery { authProvider.resendSignUpCode(any()) } returns Error(mockAuthException())

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
    }

    @Test
    fun `Confirm SignUp next step shows the SignUpConfirm screen`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(signInStep = AuthSignInStep.CONFIRM_SIGN_UP)
        )
        coEvery { authProvider.resendSignUpCode(any()) } returns Success(mockk())

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.SignUpConfirm
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
        coEvery { authProvider.fetchUserAttributes() } returns Error(mockk(relaxed = true))

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

    @Test
    fun `signing in with no internet results in network error message`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns
            Error(
                mockk<UnknownException> {
                    every { cause } returns
                        mockk<HttpException> {
                            every { cause } returns mockk<UnknownHostException>()
                        }
                }
            )

        viewModel.start(mockAuthenticatorConfiguration())

        viewModel.shouldEmitMessage<NetworkErrorMessage> {
            viewModel.signIn("username", "password")
        }

        // Assert step does not change
        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
    }

    @Test
    fun `moves to SignedInState when receiving SignedIn event`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))

        viewModel.start(mockAuthenticatorConfiguration())
        runCurrent()

        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
        hubFlow.emit(HubEvent.create(AuthChannelEventName.SIGNED_IN.name))
        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `does not advance to signed in if sign in is in progress when SignedIn event is received`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } coAnswers {
            delay(1000) // delay so that the sign in does not complete until the clock is advanced
            Success(mockSignInResult())
        }

        viewModel.start(mockAuthenticatorConfiguration())
        runCurrent()

        viewModel.currentStep shouldBe AuthenticatorStep.SignIn

        backgroundScope.launch { viewModel.signIn("username", "password") }

        hubFlow.emit(HubEvent.create(AuthChannelEventName.SIGNED_IN.name))

        // Since sign in is in progress we should not move to SignedIn until after it completes
        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
        advanceUntilIdle() // advance the clock to complete sign in
        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `does not advance to SignedIn when SignedIn event is received in a post-sign-in state`() = runTest {
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
        hubFlow.emit(HubEvent.create(AuthChannelEventName.SIGNED_IN.name))
        viewModel.currentStep shouldBe AuthenticatorStep.VerifyUser // stay in current state
    }

//endregion
//region sign up tests

    @Test
    fun `user can autoSignIn after sign up`() = runTest {
        val result = mockSignUpResult(nextStep = mockNextSignUpStep(signUpStep = AuthSignUpStep.COMPLETE_AUTO_SIGN_IN))
        coEvery { authProvider.signUp("username", "password", any()) } returns Success(result)
        coEvery { authProvider.autoSignIn() } returns Success(mockSignInResult())

        viewModel.start(mockAuthenticatorConfiguration())
        viewModel.signUp("username", "password", emptyList())
        advanceUntilIdle()

        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

//endregion
//region password reset tests

    @Test
    fun `Sign in with temporary password requires password reset`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.signIn(any(), any()) } returns Success(
            mockSignInResult(
                signInStep = AuthSignInStep.RESET_PASSWORD
            )
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.SignIn))

        viewModel.signIn("username", "password")
        viewModel.currentStep shouldBe AuthenticatorStep.PasswordReset
    }

    @Test
    fun `Password reset returns a result of DONE, state should be sign in`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.resetPassword(any()) } returns Success(
            AuthResetPasswordResult(
                true,
                AuthNextResetPasswordStep(AuthResetPasswordStep.DONE, emptyMap(), null)
            )
        )
        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.PasswordReset))

        viewModel.resetPassword("username")
        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
    }

    @Test
    fun `Password reset fails with an error, state should stay in PasswordReset`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.resetPassword(any()) } returns Error(
            mockk<UnknownException> {
                every { cause } returns mockk<HttpException> {
                    every { cause } returns mockk<UnknownHostException>()
                }
            }
        )
        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.PasswordReset))

        viewModel.resetPassword("username")
        viewModel.currentStep shouldBe AuthenticatorStep.PasswordReset
    }

    @Test
    fun `Password reset confirmation succeeds, sign in succeeds, state should be signed in`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.resetPassword(any()) } returns Success(
            AuthResetPasswordResult(
                true,
                AuthNextResetPasswordStep(AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE, emptyMap(), null)
            )
        )

        coEvery { authProvider.confirmResetPassword(any(), any(), any()) } returns Success(Unit)
        coEvery { authProvider.signIn(any(), any()) } returns Success(mockSignInResult())

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.PasswordReset))

        viewModel.resetPassword("username")
        viewModel.confirmResetPassword("username", "password", "code")
        viewModel.currentStep shouldBe AuthenticatorStep.SignedIn
    }

    @Test
    fun `Password reset confirmation fails, state should stay in PasswordResetConfirm`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.resetPassword(any()) } returns Success(
            AuthResetPasswordResult(
                true,
                AuthNextResetPasswordStep(AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE, emptyMap(), null)
            )
        )

        coEvery { authProvider.confirmResetPassword(any(), any(), any()) } returns Error(
            mockk<UnknownException> {
                every { cause } returns mockk<HttpException> {
                    every { cause } returns mockk<UnknownHostException>()
                }
            }
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.PasswordReset))

        viewModel.resetPassword("username")
        viewModel.confirmResetPassword("username", "password", "code")
        viewModel.currentStep shouldBe AuthenticatorStep.PasswordResetConfirm
    }

    @Test
    fun `Password reset confirmation succeeds, sign in fails, state should be sign in`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.resetPassword(any()) } returns Success(
            AuthResetPasswordResult(
                true,
                AuthNextResetPasswordStep(AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE, emptyMap(), null)
            )
        )

        coEvery { authProvider.confirmResetPassword(any(), any(), any()) } returns Success(Unit)
        coEvery { authProvider.signIn(any(), any()) } returns Error(
            mockk<UnknownException> {
                every { cause } returns mockk<HttpException> {
                    every { cause } returns mockk<UnknownHostException>()
                }
            }
        )

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.PasswordReset))

        viewModel.resetPassword("username")
        viewModel.confirmResetPassword("username", "password", "code")
        viewModel.currentStep shouldBe AuthenticatorStep.SignIn
    }

    @Test
    fun `Password reset results in limit exceeded message`() = runTest {
        coEvery { authProvider.fetchAuthSession() } returns Success(mockAuthSession(isSignedIn = false))
        coEvery { authProvider.resetPassword(any()) } returns Error(LimitExceededException(null))

        viewModel.start(mockAuthenticatorConfiguration(initialStep = AuthenticatorStep.PasswordReset))

        viewModel.shouldEmitMessage<LimitExceededMessage> {
            viewModel.resetPassword("username")
        }
    }

//endregion
//region helpers
    private val AuthenticatorViewModel.currentStep: AuthenticatorStep
        get() = stepState.value.step
//endregion
}
