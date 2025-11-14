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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthWebAuthnCredential
import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.MutableFormState

/**
 * A state holder for the UI for a specific [AuthenticatorStep].
 */
@Stable
interface AuthenticatorStepState {
    /**
     * The [AuthenticatorStep] that this state holder represents.
     */
    val step: AuthenticatorStep
}

/**
 * A state holder for the UI that has multiple possible actions that may be in progress.
 */
@Stable
interface AuthenticatorActionState<T> {
    /**
     * The action in progress, or null if state is idle
     */
    val action: T?
}

/**
 * The Authenticator is loading the current state of the user's Auth session.
 */
@Immutable
object LoadingState : AuthenticatorStepState {
    override val step = AuthenticatorStep.Loading
}

/**
 * The Authenticator has encountered an unrecoverable error.
 * @param error The error that occurred.
 */
@Immutable
data class ErrorState(val error: AuthException) : AuthenticatorStepState {
    override val step = AuthenticatorStep.Error
}

/**
 * The user has completed the sign in process.
 */
@Immutable
interface SignedInState : AuthenticatorStepState {
    /**
     * The [AuthUser] instance for the signed in user.
     */
    val user: AuthUser

    /**
     * Sign out the current user. This does a local sign out and returns the [AuthSignOutResult] that
     * may be inspected to determine if any parts of the sign out were unsuccessful.
     */
    suspend fun signOut(): AuthSignOutResult
}

/**
 * The user is on the Sign In step. They can enter their Sign In information to authenticate with Amplify.
 */
@Stable
interface SignInState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Initiate a sign in with the information entered into the [form].
     */
    suspend fun signIn()
}

/**
 * The user has entered their username and must select the authentication factor they'd like to use to sign in
 */
@Stable
interface SignInSelectAuthFactorState :
    AuthenticatorStepState,
    AuthenticatorActionState<SignInSelectAuthFactorState.Action> {

    sealed interface Action {
        /**
         * User has selected an auth factor
         */
        data class SelectFactor(val factor: AuthFactor) : Action
    }

    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The username entered in the SignIn step
     */
    val username: String

    /**
     * The available types to select how to sign in.
     */
    val availableAuthFactors: Set<AuthFactor>

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Initiate a sign in with one of the available sign in types
     */
    suspend fun select(authFactor: AuthFactor)
}

/**
 * A user has entered their username and must enter their password to continue signing in
 */
@Stable
interface SignInConfirmPasswordState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The username entered in the SignIn step
     */
    val username: String

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Initiate a sign in with the information entered into the [form].
     */
    suspend fun signIn()
}

/**
 * The user has completed the initial Sign In step, and needs to enter the confirmation code from an MFA
 * message to complete the sign in process.
 */
@Stable
interface SignInConfirmMfaState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The [AuthCodeDeliveryDetails] for the confirmation code that was sent to the user when entering this state.
     */
    val deliveryDetails: AuthCodeDeliveryDetails?

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Confirm the user's sign in using the information entered into the [form].
     */
    suspend fun confirmSignIn()
}

/**
 * The user has completed the initial Sign In step, and needs to enter the confirmation code from a custom
 * challenge to complete the sign in process.
 */
@Stable
interface SignInConfirmCustomState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The additional info that is configured with your custom challenge. For more information, please see
     * how to [sign in with a custom flow](https://docs.amplify.aws/lib/auth/signin_with_custom_flow/q/platform/android)
     * in the Amplify documentation.
     */
    val additionalInfo: Map<String, String>

    /**
     * The [AuthCodeDeliveryDetails] for the confirmation code that was sent to the user when entering this state.
     */
    val deliveryDetails: AuthCodeDeliveryDetails?

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Confirm the user's sign in using the information entered into the [form].
     */
    suspend fun confirmSignIn()
}

/**
 * The user has completed the initial Sign In step, and is required to change their password in order to complete
 * the sign in process.
 */
@Stable
interface SignInConfirmNewPasswordState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Confirm the user's sign in using the information entered into the [form].
     */
    suspend fun confirmSignIn()
}

/**
 * The user has completed the initial Sign In step, and needs to enter a TOTP code from a registered authenticator
 * app.
 */
@Stable
interface SignInConfirmTotpCodeState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Confirm the user's sign in using the information entered into the [form].
     */
    suspend fun confirmSignIn()
}

/**
 * The user has completed the initial Sign In step, and must register a TOTP authenticator app to continue.
 */
@Stable
interface SignInContinueWithTotpSetupState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The code that can be used to manually register with the authenticator application.
     */
    val sharedSecret: String

    /**
     * The URI that can be used to automatically register with the authenticator application.
     */
    val setupUri: String

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Continue the user's sign in using the information entered into the [form].
     */
    suspend fun continueSignIn()
}

/**
 * The user has completed the initial Sign In step,  and must setup their desired MFA method to continue.
 */
@Stable
interface SignInContinueWithMfaSetupSelectionState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The set of [MFAType] that could be used to continue this sign in.
     */
    val allowedMfaTypes: Set<MFAType>

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Continue the user's sign in using the information entered into the [form].
     */
    suspend fun continueSignIn()
}

/**
 * The user has completed the initial Sign In step,  and must setup email MFA method to continue.
 */
@Stable
interface SignInContinueWithEmailSetupState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Continue the user's sign in using the information entered into the [form].
     */
    suspend fun continueSignIn()
}

/**
 * The user has completed the initial Sign In step,  and must select their desired MFA method to continue.
 */
@Stable
interface SignInContinueWithMfaSelectionState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The set of [MFAType] that could be used to continue this sign in.
     */
    val allowedMfaTypes: Set<MFAType>

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Continue the user's sign in using the information entered into the [form].
     */
    suspend fun continueSignIn()
}

/**
 * The user is on the Sign Up step, and can fill out the account creation form to Sign Up.
 */
@Stable
interface SignUpState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Initiate the sign up using the information entered into the [form].
     */
    suspend fun signUp()
}

/**
 * The user has signed up, but needs to enter a confirmation code sent to them.
 */
@Stable
interface SignUpConfirmState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The [AuthCodeDeliveryDetails] for the confirmation code that was sent to the user when entering this state.
     */
    val deliveryDetails: AuthCodeDeliveryDetails?

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Confirm the sign up using the information entered into the [form].
     */
    suspend fun confirmSignUp()

    /**
     * Re-send the confirmation code to the user.
     */
    suspend fun resendCode()
}

/**
 * The user is on the Password Reset step. They can enter their username to begin the password reset.
 */
@Stable
interface PasswordResetState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Initiate the password reset using the information entered into the [form].
     */
    suspend fun submitPasswordReset()
}

/**
 * The user has entered their username and been sent a confirmation code. The need to enter the code and their new
 * password to complete the password reset.
 */
@Stable
interface PasswordResetConfirmState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The [AuthCodeDeliveryDetails] for the confirmation code that was sent to the user when entering this state.
     */
    val deliveryDetails: AuthCodeDeliveryDetails?

    /**
     * Move the user to a different [AuthenticatorInitialStep].
     */
    fun moveTo(step: AuthenticatorInitialStep)

    /**
     * Confirm the password reset using the information entered into the [form].
     */
    suspend fun submitPasswordResetConfirm()
}

/**
 * The user has successfully signed in and their account is confirmed, however they do not have any means of account
 * recovery (email, phone) that is confirmed.
 */
@Stable
interface VerifyUserState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The list of unverified attributes.
     */
    val attributes: List<AuthUserAttribute>

    /**
     * Submit the selected attribute to initiate the attribute verification.
     */
    suspend fun verifyUser()

    /**
     * Skip verification and move to the Signed In state.
     */
    fun skip()
}

/**
 * The user has initiated verification of an account recovery mechanism (email, phone) and needs to provide a
 * confirmation code.
 */
@Stable
interface VerifyUserConfirmState : AuthenticatorStepState {
    /**
     * The input form state holder for this step.
     */
    val form: MutableFormState

    /**
     * The details of where the verification code was sent.
     */
    val deliveryDetails: AuthCodeDeliveryDetails?

    /**
     * Submit the entered confirmation code to confirm the verification.
     */
    suspend fun confirmVerifyUser()

    /**
     * Re-send the verification code.
     */
    suspend fun resendCode()

    /**
     * Skip verification and move to the Signed In state.
     */
    fun skip()
}

/**
 * The user is being shown a prompt to create a passkey, encouraging them to use this as a way to sign in quickly
 * via biometrics
 */
@Stable
interface PromptToCreatePasskeyState :
    AuthenticatorStepState,
    AuthenticatorActionState<PromptToCreatePasskeyState.Action> {
    sealed interface Action {
        /**
         * User is creating a passkey
         */
        class CreatePasskey : Action

        /**
         * User has selected the Skip button
         */
        class Skip : Action
    }

    /**
     * Create a passkey
     */
    suspend fun createPasskey()

    /**
     * Skip passkey creation and continue to the next step
     */
    suspend fun skip()
}

/**
 * The user is being shown a confirmation screen after creating a passkey
 */
@Stable
interface PasskeyCreatedState :
    AuthenticatorStepState,
    AuthenticatorActionState<PasskeyCreatedState.Action> {
    sealed interface Action {
        /**
         * User has selected the Done button
         */
        class ContinueSignIn : Action
    }

    /**
     * A list of existing passkeys for this user, including the one they've just created
     */
    val passkeys: List<AuthWebAuthnCredential>

    /**
     * Continue to the next step
     */
    suspend fun continueSignIn()
}
