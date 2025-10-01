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

package com.amplifyframework.ui.authenticator.testUtil

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.result.AuthWebAuthnCredential
import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.forms.FormData
import com.amplifyframework.ui.authenticator.mockAuthCodeDeliveryDetails
import com.amplifyframework.ui.authenticator.states.PasskeyCreatedStateImpl
import com.amplifyframework.ui.authenticator.states.PasskeyCreationPromptStateImpl
import com.amplifyframework.ui.authenticator.states.PasswordResetConfirmStateImpl
import com.amplifyframework.ui.authenticator.states.PasswordResetStateImpl
import com.amplifyframework.ui.authenticator.states.SignInConfirmMfaStateImpl
import com.amplifyframework.ui.authenticator.states.SignInConfirmTotpCodeStateImpl
import com.amplifyframework.ui.authenticator.states.SignInContinueWithEmailSetupStateImpl
import com.amplifyframework.ui.authenticator.states.SignInContinueWithMfaSelectionStateImpl
import com.amplifyframework.ui.authenticator.states.SignInContinueWithMfaSetupSelectionStateImpl
import com.amplifyframework.ui.authenticator.states.SignInContinueWithTotpSetupStateImpl
import com.amplifyframework.ui.authenticator.states.SignInStateImpl
import com.amplifyframework.ui.authenticator.states.SignUpStateImpl

internal fun mockSignInState() = SignInStateImpl(
    signInMethod = SignInMethod.Username,
    onSubmit = { _, _ -> },
    onMoveTo = { }
)

internal fun mockSignUpState() = SignUpStateImpl(
    signInMethod = SignInMethod.Username,
    signUpAttributes = listOf(AuthUserAttributeKey.email()),
    passwordCriteria = PasswordCriteria(8, false, false, false, false),
    signUpForm = FormData(emptyList()),
    onSubmit = { _, _, _ -> },
    onMoveTo = { }
)

internal fun mockPasswordResetState() = PasswordResetStateImpl(
    signInMethod = SignInMethod.Username,
    onSubmit = {},
    onMoveTo = {}
)

internal fun mockPasswordResetConfirmState(
    passwordCriteria: PasswordCriteria = PasswordCriteria(8, false, false, false, false),
    deliveryDetails: AuthCodeDeliveryDetails = mockAuthCodeDeliveryDetails(),
    onSubmit: (String, String) -> Unit = { _, _ -> },
    onMoveTo: (AuthenticatorInitialStep) -> Unit = {}
) = PasswordResetConfirmStateImpl(
    passwordCriteria = passwordCriteria,
    deliveryDetails = deliveryDetails,
    onSubmit = onSubmit,
    onMoveTo = onMoveTo
)

internal fun mockSignInConfirmTotpCodeState(
    onSubmit: (String) -> Unit = { },
    onMoveTo: (AuthenticatorInitialStep) -> Unit = { }
) = SignInConfirmTotpCodeStateImpl(
    onSubmit = onSubmit,
    onMoveTo = onMoveTo
)

internal fun mockSignInContinueWithEmailSetupState(
    onSubmit: suspend (email: String) -> Unit = {},
    onMoveTo: (step: AuthenticatorInitialStep) -> Unit = {}
) = SignInContinueWithEmailSetupStateImpl(
    onSubmit = onSubmit,
    onMoveTo = onMoveTo
)

internal fun mockSignInContinueWithMfaSelectionState(
    allowedMfaTypes: Set<MFAType> = MFAType.entries.toSet(),
    onSubmit: (String) -> Unit = {},
    onMoveTo: (AuthenticatorInitialStep) -> Unit = {}
) = SignInContinueWithMfaSelectionStateImpl(
    allowedMfaTypes = allowedMfaTypes,
    onSubmit = onSubmit,
    onMoveTo = onMoveTo
)

internal fun mockSignInContinueWithTotpSetupState(
    sharedSecret: String = "",
    setupUri: String = "",
    onSubmit: (String) -> Unit = { },
    onMoveTo: (AuthenticatorInitialStep) -> Unit = { }
) = SignInContinueWithTotpSetupStateImpl(
    sharedSecret = sharedSecret,
    setupUri = setupUri,
    onSubmit = onSubmit,
    onMoveTo = onMoveTo
)

internal fun mockSignInConfirmMfaState(deliveryDetails: AuthCodeDeliveryDetails = mockAuthCodeDeliveryDetails()) =
    SignInConfirmMfaStateImpl(
        deliveryDetails = deliveryDetails,
        onSubmit = { },
        onMoveTo = { }
    )

internal fun mockSignInContinueWithMfaSetupSelectionState(
    allowedMfaTypes: Set<MFAType> = setOf(MFAType.TOTP, MFAType.SMS, MFAType.EMAIL)
) = SignInContinueWithMfaSetupSelectionStateImpl(
    allowedMfaTypes = allowedMfaTypes,
    onSubmit = { },
    onMoveTo = { }
)

internal fun mockPasskeyCreatedState(
    passkeys: List<AuthWebAuthnCredential> = emptyList(),
    onDone: suspend () -> Unit = {}
) = PasskeyCreatedStateImpl(
    passkeys = passkeys,
    onDone = onDone
)

internal fun mockPasskeyCreationPromptState(onSubmit: suspend () -> Unit = {}, onSkip: suspend () -> Unit = {}) =
    PasskeyCreationPromptStateImpl(
        onSubmit = onSubmit,
        onSkip = onSkip
    )
