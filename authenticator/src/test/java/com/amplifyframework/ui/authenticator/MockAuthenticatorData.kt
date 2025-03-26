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

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.ui.authenticator.auth.AmplifyAuthConfiguration
import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.auth.VerificationMechanism
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.SignUpFormBuilder
import com.amplifyframework.ui.authenticator.options.TotpOptions
import com.amplifyframework.ui.authenticator.util.AuthConfigurationResult
import io.mockk.mockk

internal fun mockAuthenticatorConfiguration(
    initialStep: AuthenticatorInitialStep = AuthenticatorStep.SignIn,
    signUpForm: SignUpFormBuilder.() -> Unit = {},
    totpOptions: TotpOptions? = null
) = AuthenticatorConfiguration(
    initialStep = initialStep,
    signUpForm = signUpForm,
    totpOptions = totpOptions
)

internal fun mockAmplifyAuthConfiguration(
    signInMethod: SignInMethod = SignInMethod.Username,
    signUpAttributes: List<AuthUserAttributeKey> = emptyList(),
    passwordCriteria: PasswordCriteria = mockk(relaxed = true),
    verificationMechanisms: Set<VerificationMechanism> = emptySet()
) = AuthConfigurationResult.Valid(
    AmplifyAuthConfiguration(
        signInMethod = signInMethod,
        signUpAttributes = signUpAttributes,
        passwordCriteria = passwordCriteria,
        verificationMechanisms = verificationMechanisms
    )
)

internal fun mockAuthException(
    message: String = "A test exception",
    recoverySuggestion: String = "A test suggestion",
    cause: Throwable? = null
) = AuthException(
    message = message,
    recoverySuggestion = recoverySuggestion,
    cause = cause
)

internal fun mockAuthSession(isSignedIn: Boolean = false) = AuthSession(isSignedIn)

internal fun mockAuthUser(userId: String = "userId", username: String = "username") = AuthUser(userId, username)

internal fun mockSignInResult(isSignedIn: Boolean = true, nextSignInStep: AuthNextSignInStep = mockNextSignInStep()) =
    AuthSignInResult(isSignedIn, nextSignInStep)

internal fun mockSignInResult(
    signInStep: AuthSignInStep = AuthSignInStep.DONE,
    additionalInfo: Map<String, String> = emptyMap(),
    codeDeliveryDetails: AuthCodeDeliveryDetails? = null,
    totpSetupDetails: TOTPSetupDetails? = null,
    allowedMFATypes: Set<MFAType>? = null
) = AuthSignInResult(
    signInStep == AuthSignInStep.DONE,
    mockNextSignInStep(
        signInStep = signInStep,
        additionalInfo = additionalInfo,
        codeDeliveryDetails = codeDeliveryDetails,
        totpSetupDetails = totpSetupDetails,
        allowedMFATypes = allowedMFATypes
    )
)

internal fun mockNextSignInStep(
    signInStep: AuthSignInStep = AuthSignInStep.DONE,
    additionalInfo: Map<String, String> = emptyMap(),
    codeDeliveryDetails: AuthCodeDeliveryDetails? = null,
    totpSetupDetails: TOTPSetupDetails? = null,
    allowedMFATypes: Set<MFAType>? = null,
    availableFactors: Set<AuthFactorType>? = null
) = AuthNextSignInStep(
    signInStep,
    additionalInfo,
    codeDeliveryDetails,
    totpSetupDetails,
    allowedMFATypes,
    availableFactors
)

internal fun mockSignUpResult(
    nextStep: AuthNextSignUpStep,
    userId: String = "userId"
) = AuthSignUpResult(
    nextStep.signUpStep != AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
    nextStep,
    userId
)

internal fun mockNextSignUpStep(
    signUpStep: AuthSignUpStep = AuthSignUpStep.DONE,
    additionalInfo: Map<String, String> = emptyMap(),
    codeDeliveryDetails: AuthCodeDeliveryDetails? = null
) = AuthNextSignUpStep(
    signUpStep,
    additionalInfo,
    codeDeliveryDetails
)

internal fun mockUserAttributes(vararg attribute: Pair<AuthUserAttributeKey, String>) =
    attribute.map { AuthUserAttribute(it.first, it.second) }

internal fun mockUser(userId: String = "userId", username: String = "username") = AuthUser(userId, username)
