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

import com.amplifyframework.ui.authenticator.SignInState
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.auth.toFieldKey
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal class SignInStateImpl(
    private val signInMethod: SignInMethod,
    private val onSubmit: suspend (username: String, password: String) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignInState {

    init {
        form.addFields {
            fieldForSignInMethod(signInMethod)
            password()
        }
    }

    override val step = AuthenticatorStep.SignIn
    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun signIn() = doSubmit {
        val username = form.getTrimmed(signInMethod.toFieldKey())!!
        val password = form.getTrimmed(FieldKey.Password)!!
        onSubmit(username, password)
    }
}
