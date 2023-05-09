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

import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.ui.authenticator.VerifyUserState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal class VerifyUserStateImpl(
    override val attributes: List<AuthUserAttribute>,
    private val onSubmit: suspend (attribute: AuthUserAttributeKey) -> Unit,
    private val onSkip: () -> Unit
) : BaseStateImpl(), VerifyUserState {

    init {
        form.addFields {
            verificationAttribute()
        }
    }

    override val step = AuthenticatorStep.VerifyUser

    override suspend fun verifyUser() = doSubmit {
        val keyString = form.getTrimmed(FieldKey.VerificationAttribute)!!
        val key = AuthUserAttributeKey.custom(keyString)
        onSubmit(key)
    }

    override fun skip() = onSkip()
}
