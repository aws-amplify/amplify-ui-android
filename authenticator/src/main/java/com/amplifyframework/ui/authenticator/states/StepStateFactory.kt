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

package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.ui.authenticator.auth.AmplifyAuthConfiguration
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.forms.FormData

internal class StepStateFactory(
    private val authConfiguration: AmplifyAuthConfiguration,
    private val signUpForm: FormData,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) {

    fun newSignedInState(
        user: AuthUser,
        onSignOut: suspend () -> AuthSignOutResult
    ) = SignedInStateImpl(
        user = user,
        onSignOut = onSignOut
    )

    fun newSignInState(
        onSubmit: suspend (username: String, password: String) -> Unit
    ) = SignInStateImpl(
        signInMethod = authConfiguration.signInMethod,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newSignInMfaState(
        codeDeliveryDetails: AuthCodeDeliveryDetails?,
        onSubmit: suspend (confirmationCode: String) -> Unit
    ) = SignInConfirmMfaStateImpl(
        deliveryDetails = codeDeliveryDetails,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newSignInConfirmCustomState(
        codeDeliveryDetails: AuthCodeDeliveryDetails?,
        additionalInfo: Map<String, String>,
        onSubmit: suspend (confirmationCode: String) -> Unit
    ) = SignInConfirmCustomStateImpl(
        deliveryDetails = codeDeliveryDetails,
        additionalInfo = additionalInfo,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newSignInConfirmNewPasswordState(
        onSubmit: suspend (password: String) -> Unit
    ) = SignInConfirmNewPasswordStateImpl(
        passwordCriteria = authConfiguration.passwordCriteria,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newSignUpState(
        onSubmit: suspend (username: String, password: String, attributes: List<AuthUserAttribute>) -> Unit
    ) = SignUpStateImpl(
        signInMethod = authConfiguration.signInMethod,
        signUpAttributes = authConfiguration.signUpAttributes,
        passwordCriteria = authConfiguration.passwordCriteria,
        signUpForm = signUpForm,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newSignUpConfirmState(
        deliveryDetails: AuthCodeDeliveryDetails?,
        onResendCode: suspend () -> Unit,
        onSubmit: suspend (confirmationCode: String) -> Unit
    ) = SignUpConfirmStateImpl(
        deliveryDetails = deliveryDetails,
        onResendCode = onResendCode,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newResetPasswordState(
        onSubmit: suspend (username: String) -> Unit
    ) = PasswordResetStateImpl(
        signInMethod = authConfiguration.signInMethod,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newResetPasswordConfirmState(
        deliveryDetails: AuthCodeDeliveryDetails?,
        onSubmit: suspend (newPassword: String, confirmationCode: String) -> Unit
    ) = PasswordResetConfirmStateImpl(
        passwordCriteria = authConfiguration.passwordCriteria,
        deliveryDetails = deliveryDetails,
        onSubmit = onSubmit,
        onMoveTo = onMoveTo
    )

    fun newVerifyUserState(
        attributes: List<AuthUserAttribute>,
        onSubmit: suspend (attribute: AuthUserAttributeKey) -> Unit,
        onSkip: () -> Unit
    ) = VerifyUserStateImpl(
        attributes = attributes,
        onSubmit = onSubmit,
        onSkip = onSkip
    )

    fun newVerifyUserConfirmState(
        deliveryDetails: AuthCodeDeliveryDetails?,
        onSubmit: suspend (confirmationCode: String) -> Unit,
        onResendCode: suspend () -> Unit,
        onSkip: () -> Unit
    ) = VerifyUserConfirmStateImpl(
        deliveryDetails = deliveryDetails,
        onSubmit = onSubmit,
        onResendCode = onResendCode,
        onSkip = onSkip
    )
}
