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

import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.states.SignInStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.signIn
import com.amplifyframework.ui.testing.ComposeTest
import org.junit.Test

class SignInTest : ComposeTest() {

    @Test
    fun `title is Sign In`() {
        setContent {
            SignIn(state = mockSignInState())
        }
        signIn {
            hasTitle("Sign In")
        }
    }

    @Test
    fun `button is Sign In`() {
        setContent {
            SignIn(state = mockSignInState())
        }
        signIn {
            hasSubmitButton("Sign In")
        }
    }

    private fun mockSignInState() = SignInStateImpl(
        signInMethod = SignInMethod.Username,
        onSubmit = { _, _ -> },
        onMoveTo = { }
    )
}
