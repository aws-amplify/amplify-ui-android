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
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.SignUpFormBuilder

internal fun mockAuthConfiguration(
    initialStep: AuthenticatorInitialStep = AuthenticatorStep.SignIn,
    signUpForm: SignUpFormBuilder.() -> Unit = {}
) = AuthenticatorConfiguration(
    initialStep = initialStep,
    signUpForm = signUpForm
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

internal fun mockAuthSession(
    isSignedIn: Boolean = false
) = AuthSession(isSignedIn)

internal fun mockAuthUser(
    userId: String = "userId",
    username: String = "username"
) = AuthUser(userId, username)

internal fun mockSignInResult(
    isSignedIn: Boolean = true,
    nextSignInStep: AuthNextSignInStep = mockNextSignInStep()
) = AuthSignInResult(isSignedIn, nextSignInStep)

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
    allowedMFATypes: Set<MFAType>? = null
) = AuthNextSignInStep(signInStep, additionalInfo, codeDeliveryDetails, totpSetupDetails, allowedMFATypes)
