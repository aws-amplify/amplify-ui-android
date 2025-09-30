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

package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.forms.FieldKey

@Suppress("ConstPropertyName")
internal object TestTags {
    const val SignInConfirmButton = "SignInConfirmButton"
    const val BackToSignInButton = "BackToSignInButton"
    const val CopyKeyButton = "CopyKeyButton"
    const val SignInButton = "SignInButton"
    const val SignUpButton = "SignUpButton"
    const val ForgotPasswordButton = "ForgotPasswordButton"
    const val CreateAccountButton = "CreateAccountButton"
    const val PasswordResetButton = "PasswordResetButton"
    const val ContinueButton = "ContinueButton"
    const val CreatePasskeyButton = "CreatePasskeyButton"
    const val SkipPasskeyButton = "SkipPasskeyButton"
    const val AuthenticatorTitle = "AuthenticatorTitle"

    const val AuthFactorPassword = "AuthFactorPassword"
    const val AuthFactorSms = "AuthFactorSms"
    const val AuthFactorEmail = "AuthFactorEmail"
    const val AuthFactorPasskey = "AuthFactorPasskey"

    const val ShowPasswordIcon = "ShowPasswordIcon"
}

internal val FieldKey.testTag: String
    get() = this.toString()

internal val AuthFactor.testTag: String
    get() = when (this) {
        is AuthFactor.Password -> TestTags.AuthFactorPassword
        AuthFactor.SmsOtp -> TestTags.AuthFactorSms
        AuthFactor.EmailOtp -> TestTags.AuthFactorEmail
        AuthFactor.WebAuthn -> TestTags.AuthFactorPasskey
    }
