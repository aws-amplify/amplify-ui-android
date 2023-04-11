/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.MutableFormState

/**
 * A Screen State is a state holder for the UI for a specific [AuthenticatorStep].
 */
@Stable
interface AuthenticatorScreenState {
    val step: AuthenticatorStep
}

/**
 * The Authenticator is loading the current state of the user's Auth session.
 */
@Immutable
object LoadingState : AuthenticatorScreenState {
    override val step = AuthenticatorStep.Loading
}

/**
 * The Authenticator has encountered an unrecoverable error.
 * @param error The error that occurred.
 */
@Immutable
data class ErrorState(val error: AuthException) : AuthenticatorScreenState {
    override val step = AuthenticatorStep.Error
}

/**
 * The user has completed the sign in process.
 */
@Immutable
interface SignedInState : AuthenticatorScreenState {
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
 * An [AuthenticatorScreenState] for a part of UI that contains an input form.
 */
@Stable
interface FormHolderState : AuthenticatorScreenState {
    val form: MutableFormState
}

/**
 * The user is on the Sign In step. They can enter their Sign In information to authenticate with Amplify.
 */
@Stable
interface SignInState : FormHolderState {
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun signIn()
}

/**
 * The user has completed the initial Sign In step, and needs to enter the confirmation code from an MFA
 * message to complete the sign in process.
 */
@Stable
interface SignInConfirmMfaState : FormHolderState {
    val deliveryDetails: AuthCodeDeliveryDetails?
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun confirmSignIn()
}

/**
 * The user has completed the initial Sign In step, and needs to enter the confirmation code from a custom
 * challenge to complete the sign in process.
 */
@Stable
interface SignInConfirmCustomState : FormHolderState {
    val additionalInfo: Map<String, String>
    val deliveryDetails: AuthCodeDeliveryDetails?
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun confirmSignIn()
}

/**
 * The user has completed the initial Sign In step, and is required to change their password in order to complete
 * the sign in process.
 */
@Stable
interface SignInConfirmNewPasswordState : FormHolderState {
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun confirmSignIn()
}

/**
 * The user is on the Sign Up step, and can fill out the account creation form to Sign Up.
 */
@Stable
interface SignUpState : FormHolderState {
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun signUp()
}

/**
 * The user has signed up, but needs to enter a confirmation code sent to them.
 */
@Stable
interface SignUpConfirmState : FormHolderState {
    val deliveryDetails: AuthCodeDeliveryDetails?
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun confirmSignUp()
    suspend fun resendCode()
}

/**
 * The user is on the Password Reset step. They can enter their username to begin the password reset.
 */
@Stable
interface PasswordResetState : FormHolderState {
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun submitPasswordReset()
}

/**
 * The user has entered their username and been sent a confirmation code. The need to enter the code and their new
 * password to complete the password reset.
 */
@Stable
interface PasswordResetConfirmState : FormHolderState {
    val deliveryDetails: AuthCodeDeliveryDetails?
    fun moveTo(step: AuthenticatorInitialStep)
    suspend fun submitPasswordResetConfirm()
}

/**
 * The user has successfully signed in and their account is confirmed, however they do not have any means of account recovery (email, phone) that is confirmed.
 */
@Stable
interface VerifyUserState : FormHolderState {
    /**
     * The list of unverified attributes.
     */
    val attributes: List<AuthUserAttribute>

    /**
     * Submit the selected attribute to initiate the attribute verification.
     */
    suspend fun verifyUser()

    /**
     * Skip verification and move to the Signed In state
     */
    fun skip()
}

/**
 * The user has initiated verification of an account recovery mechanism (email, phone) and needs to provide a confirmation code.
 */
@Stable
interface VerifyUserConfirmState : FormHolderState {
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
     * Skip verification and move to the Signed In state
     */
    fun skip()
}
