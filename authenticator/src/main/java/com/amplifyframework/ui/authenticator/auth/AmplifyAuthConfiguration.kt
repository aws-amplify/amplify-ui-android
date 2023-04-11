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

package com.amplifyframework.ui.authenticator.auth

import androidx.annotation.IntRange
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal enum class SignInMethod {
    Username, Email, PhoneNumber
}

internal enum class VerificationMechanism {
    PhoneNumber, Email
}

internal data class PasswordCriteria(
    @IntRange(from = 6, to = 99) val length: Int,
    val requiresNumber: Boolean,
    val requiresSpecial: Boolean,
    val requiresUpper: Boolean,
    val requiresLower: Boolean
)

/**
 * This is a temporary mock of the information we'll need to get from Amplify during start up
 */
internal class AmplifyAuthConfiguration(
    val signInMethod: SignInMethod,
    val signUpAttributes: List<AuthUserAttributeKey>,
    val passwordCriteria: PasswordCriteria,
    val verificationMechanisms: Set<VerificationMechanism>
)

internal fun SignInMethod.toFieldKey() = when (this) {
    SignInMethod.Username -> FieldKey.Username
    SignInMethod.Email -> FieldKey.Email
    SignInMethod.PhoneNumber -> FieldKey.PhoneNumber
}

internal fun VerificationMechanism.toAttributeKey() = when (this) {
    VerificationMechanism.PhoneNumber -> AuthUserAttributeKey.phoneNumber()
    VerificationMechanism.Email -> AuthUserAttributeKey.email()
}

internal fun VerificationMechanism.toVerifiedAttributeKey() = when (this) {
    VerificationMechanism.PhoneNumber -> AuthUserAttributeKey.phoneNumberVerified()
    VerificationMechanism.Email -> AuthUserAttributeKey.emailVerified()
}
