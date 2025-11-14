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

import com.amplifyframework.auth.AuthFactorType

sealed interface AuthFactor {
    data class Password(val srp: Boolean = true) : AuthFactor
    data object EmailOtp : AuthFactor
    data object SmsOtp : AuthFactor
    data object WebAuthn : AuthFactor
}

internal fun AuthFactor.toAuthFactorType() = when (this) {
    AuthFactor.EmailOtp -> AuthFactorType.EMAIL_OTP
    AuthFactor.SmsOtp -> AuthFactorType.SMS_OTP
    AuthFactor.WebAuthn -> AuthFactorType.WEB_AUTHN
    is AuthFactor.Password -> if (srp) AuthFactorType.PASSWORD_SRP else AuthFactorType.PASSWORD
}

internal fun AuthFactorType.toAuthFactor() = when (this) {
    AuthFactorType.PASSWORD -> AuthFactor.Password(srp = false)
    AuthFactorType.PASSWORD_SRP -> AuthFactor.Password(srp = true)
    AuthFactorType.EMAIL_OTP -> AuthFactor.EmailOtp
    AuthFactorType.SMS_OTP -> AuthFactor.SmsOtp
    AuthFactorType.WEB_AUTHN -> AuthFactor.WebAuthn
}

internal val AuthFactor.challengeResponse: String
    get() = this.toAuthFactorType().challengeResponse

internal fun Collection<AuthFactorType>.toAuthFactors(): Set<AuthFactor> {
    // If both SRP and password are available then use SRP to sign in
    var factors = this
    if (this.contains(AuthFactorType.PASSWORD) && this.contains(AuthFactorType.PASSWORD_SRP)) {
        factors = this - AuthFactorType.PASSWORD // remove password
    }
    return factors.map { it.toAuthFactor() }.toSet()
}
internal fun Collection<AuthFactor>.containsPassword() = any { it is AuthFactor.Password }
