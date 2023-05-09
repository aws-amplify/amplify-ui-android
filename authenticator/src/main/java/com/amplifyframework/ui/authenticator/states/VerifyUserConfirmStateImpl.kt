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
import com.amplifyframework.ui.authenticator.VerifyUserConfirmState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal class VerifyUserConfirmStateImpl(
    override val deliveryDetails: AuthCodeDeliveryDetails?,
    private val onSubmit: suspend (confirmationCode: String) -> Unit,
    private val onResendCode: suspend () -> Unit,
    private val onSkip: () -> Unit
) : BaseStateImpl(), VerifyUserConfirmState {

    init {
        form.addFields {
            confirmationCode()
        }
    }

    override val step = AuthenticatorStep.VerifyUserConfirm
    override suspend fun confirmVerifyUser() = doSubmit {
        val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
        onSubmit(confirmationCode)
    }

    override suspend fun resendCode() = onResendCode()
    override fun skip() = onSkip()
}
