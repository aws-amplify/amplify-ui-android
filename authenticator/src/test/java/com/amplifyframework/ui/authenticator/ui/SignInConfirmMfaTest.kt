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

package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignInConfirmMfaState
import com.amplifyframework.ui.authenticator.ui.robots.signInConfirmMfa
import com.amplifyframework.ui.testing.ScreenshotTest
import org.junit.Test

class SignInConfirmMfaTest : AuthenticatorUiTest() {
    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            SignInConfirmMfa(
                mockSignInConfirmMfaState()
            )
        }
    }

    @Test
    @ScreenshotTest
    fun `incorrect code`() {
        val state = mockSignInConfirmMfaState()
        setContent {
            SignInConfirmMfa(state)
        }

        signInConfirmMfa {
            setConfirmationCode("123456")
        }

        state.form.setFieldError(FieldKey.ConfirmationCode, FieldError.ConfirmationCodeIncorrect)
    }
}
