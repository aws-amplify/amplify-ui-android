
/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.challengeResponse
import com.amplifyframework.ui.authenticator.SignInContinueWithMfaSetupSelectionState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal class SignInContinueWithMfaSetupSelectionStateImpl(
    override val allowedMfaTypes: Set<MFAType>,
    private val onSubmit: suspend (selection: String) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignInContinueWithMfaSetupSelectionState {

    init {
        form.addFields {
            mfaSelection()
        }
        form.fields[FieldKey.MfaSelection]?.state?.content = allowedMfaTypes.first().challengeResponse
    }

    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun continueSignIn() = doSubmit {
        val selected = form.fields[FieldKey.MfaSelection]!!.state.content
        onSubmit(selected)
    }

    override val step = AuthenticatorStep.SignInContinueWithMfaSetupSelection
}
