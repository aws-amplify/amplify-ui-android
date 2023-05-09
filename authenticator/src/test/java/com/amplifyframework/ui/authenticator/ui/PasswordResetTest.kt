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

package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.states.PasswordResetStateImpl
import com.amplifyframework.ui.authenticator.ui.robots.passwordReset
import com.amplifyframework.ui.testing.ComposeTest
import org.junit.Test

class PasswordResetTest : ComposeTest() {

    @Test
    fun `title is reset password`() {
        setContent {
            PasswordReset(state = mockPasswordResetState())
        }
        passwordReset {
            hasTitle("Reset Password")
        }
    }

    @Test
    fun `button is Submit`() {
        setContent {
            PasswordReset(state = mockPasswordResetState())
        }
        passwordReset {
            hasSubmitButton("Submit")
        }
    }

    @Test
    fun `has Back to Sign In button`() {
        setContent {
            PasswordReset(state = mockPasswordResetState())
        }
        passwordReset {
            assertExists("Back to Sign In")
        }
    }

    private fun mockPasswordResetState() = PasswordResetStateImpl(
        signInMethod = SignInMethod.Username,
        onSubmit = {},
        onMoveTo = {}
    )
}
