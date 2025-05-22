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

import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockPasswordResetState
import com.amplifyframework.ui.authenticator.ui.robots.passwordReset
import com.amplifyframework.ui.testing.ScreenshotTest
import org.junit.Test

class PasswordResetTest : AuthenticatorUiTest() {

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

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            PasswordReset(state = mockPasswordResetState())
        }
    }

    @Test
    @ScreenshotTest
    fun `username not found`() {
        val state = mockPasswordResetState()
        setContent {
            PasswordReset(state = state)
        }
        passwordReset {
            setUsername("username")
        }
        state.form.setFieldError(FieldKey.Username, FieldError.NotFound)
    }
}
