/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import com.amplifyframework.ui.authenticator.SignInState
import com.amplifyframework.ui.authenticator.forms.buildForm
import com.amplifyframework.ui.authenticator.forms.toState
import com.amplifyframework.ui.authenticator.states.SignInStateImpl
import com.amplifyframework.ui.testing.ComposeTest
import org.junit.Test

class SignInTest : ComposeTest() {

    @Test
    fun `title is Sign In`() {
        composeTestRule.setContent {
            SignIn(state = mockSignInState())
        }
        composeTestRule.onNode(hasTestTag("AuthenticatorTitle") and hasText("Sign In")).assertExists()
    }

    private fun mockSignInState(): SignInState {
        val form = buildForm {
            username()
            password()
        }.toState()

        return SignInStateImpl(form, {})
    }
}
