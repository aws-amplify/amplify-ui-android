/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.ui.authenticator.data

import com.amplifyframework.auth.cognito.options.AuthFlowType

/**
 * AuthenticationFlow represents the different styles of authentication supported by the Authenticator component.
 */
sealed interface AuthenticationFlow {
    /**
     * The standard password-based auth flow. The user will be prompted to enter a username and password on the SignIn
     * screen. You can use this with either Password or PasswordSrp sign ins.
     */
    data object Password : AuthenticationFlow

    /**
     * A choice-based auth flow, where the user may log in via a password, a passkey, or a one-time-password (OTP) sent
     * to their email or SMS. The user is first prompted to enter only their sign in attribute (username/email/phone)
     * and then may be presented with options for how to log in. You must have ALLOW_USER_AUTH enabled as an
     * authentication flow in your Cognito User Pool.
     */
    data class UserChoice(
        /**
         * Specify an [AuthFactor] to use by default, if available to the user.
         *
         * For example, if you want any user with a registered passkey to sign in with that passkey without being
         * prompted, then set this value to `AuthFactor.WebAuthn`.
         *
         * If this is null or the [AuthFactor] is not available to the user, they may go directly into a different
         * [AuthFactor] (if they only have one available) or may be prompted to choose a factor (if they have multiple
         * available).
         *
         * If this is set to [AuthFactor.Password] or [AuthFactor.PasswordSrp] then the user will be prompted for a
         * password directly when signing in. Use these values only if you're certain that no users exist who don't
         * have passwords.
         */
        val preferredAuthFactor: AuthFactor? = null,

        /**
         * Control when/if the user is prompted to create a passkey after logging in.
         */
        val passkeyPrompts: PasskeyPrompts = PasskeyPrompts()
    ) : AuthenticationFlow
}

internal val AuthenticationFlow.signUpRequiresPassword: Boolean get() = when (this) {
    is AuthenticationFlow.Password -> true
    is AuthenticationFlow.UserChoice -> false
}

internal val AuthenticationFlow.signInRequiresPassword: Boolean get() = when (this) {
    is AuthenticationFlow.Password -> true
    is AuthenticationFlow.UserChoice -> this.preferredAuthFactor is AuthFactor.Password
}

internal fun AuthenticationFlow.toAuthFlowType() = when (this) {
    is AuthenticationFlow.Password -> AuthFlowType.USER_SRP_AUTH
    is AuthenticationFlow.UserChoice -> AuthFlowType.USER_AUTH
}
