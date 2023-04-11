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

package com.amplifyframework.ui.authenticator.enums

/**
 * Denotes an [AuthenticatorStep] that can be an initial step in Authenticator
 */
abstract class AuthenticatorInitialStep internal constructor() : AuthenticatorStep()

/**
 * The [AuthenticatorStep] represents the user's current status in the authentication flow.
 */
abstract class AuthenticatorStep internal constructor() {

    /**
     * The Authenticator is in a loading state. We are waiting to check
     * the current authentication status.
     */
    object Loading : AuthenticatorStep()

    /**
     * The Authenticator has encountered an unrecoverable error.
     */
    object Error : AuthenticatorStep()

    /**
     * The user has successfully authenticated and is now signed in.
     */
    object SignedIn : AuthenticatorStep()

    /**
     * The user is on the Sign In step
     */
    object SignIn : AuthenticatorInitialStep()

    /**
     * The user has completed the initial Sign In step, and needs to enter the confirmation code from a custom
     * challenge to complete the sign in process.
     */
    object SignInConfirmCustomAuth : AuthenticatorStep()

    /**
     * The user has completed the initial Sign In step, and needs to enter the confirmation code from an MFA
     * message to complete the sign in process.
     */
    object SignInConfirmMfa : AuthenticatorStep()

    /**
     * The user has completed the initial Sign In step, and is required to change their password in order to complete
     * the sign in process.
     */
    object SignInConfirmNewPassword : AuthenticatorStep()

    /**
     * The user is on the Sign Up step
     */
    object SignUp : AuthenticatorInitialStep()

    /**
     * The user has signed up, but needs to enter a confirmation code sent to them.
     */
    object SignUpConfirm : AuthenticatorStep()

    /**
     * The user is on the Password Reset step. They can enter their username to begin the password reset.
     */
    object PasswordReset : AuthenticatorInitialStep()

    /**
     * The user has entered their username and been sent a confirmation code. The need to enter the code and their new
     * password to complete the password reset.
     */
    object PasswordResetConfirm : AuthenticatorStep()

    /**
     * The user has successfully signed in and their account is confirmed, however they do not have any means of account recovery (email, phone) that is confirmed.
     */
    object VerifyUser : AuthenticatorStep()

    /**
     * The user has initiated verification of an account recovery mechanism (email, phone) and needs to provide a confirmation code.
     */
    object VerifyUserConfirm : AuthenticatorStep()
}
