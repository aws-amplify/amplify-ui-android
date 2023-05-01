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

import com.amplifyframework.ui.authenticator.PasswordResetState
import com.amplifyframework.ui.authenticator.ScreenshotTestBase
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldError.NotFound
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.MutableFormState
import com.amplifyframework.ui.authenticator.mockFieldData
import com.amplifyframework.ui.authenticator.mockFieldState
import com.amplifyframework.ui.authenticator.mockForm
import org.junit.Test

class PasswordResetScreenshots : ScreenshotTestBase() {

    @Test
    fun default_state() {
        screenshot {
            PasswordReset(state = mockPasswordResetState())
        }
    }

    @Test
    fun username_not_found() {
        screenshot {
            PasswordReset(state = mockPasswordResetState(error = NotFound))
        }
    }

    private fun mockPasswordResetState(
        error: FieldError? = null
    ) = object : PasswordResetState {
        override fun moveTo(step: AuthenticatorInitialStep) {}
        override suspend fun submitPasswordReset() {}
        override val form: MutableFormState = mockForm(
            mockFieldData(FieldConfig.Text(FieldKey.Username), state = mockFieldState(error = error))
        )
        override val step = AuthenticatorStep.PasswordReset
    }
}
