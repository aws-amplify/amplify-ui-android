/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.forms.FormData
import com.amplifyframework.ui.authenticator.states.SignUpStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.signUp
import com.amplifyframework.ui.testing.ComposeTest
import org.junit.Test

class SignUpTest : ComposeTest() {

    @Test
    fun `title is Create Account`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
        signUp {
            hasTitle("Create Account")
        }
    }

    @Test
    fun `button is Create Account`() {
        setContent {
            SignUp(state = mockSignUpState())
        }
        signUp {
            hasSubmitButton("Create Account")
        }
    }

    private fun mockSignUpState() = SignUpStateImpl(
        signInMethod = SignInMethod.Username,
        signUpAttributes = emptyList(),
        passwordCriteria = PasswordCriteria(8, false, false, false, false),
        signUpForm = FormData(emptyList()),
        onSubmit = { _, _, _ -> },
        onMoveTo = { }
    )
}
