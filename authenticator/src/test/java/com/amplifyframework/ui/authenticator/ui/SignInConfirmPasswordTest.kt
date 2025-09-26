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
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignInConfirmPasswordState
import com.amplifyframework.ui.authenticator.ui.robots.signInConfirmPassword
import com.amplifyframework.ui.testing.ScreenshotTest
import org.junit.Test

class SignInConfirmPasswordTest : AuthenticatorUiTest() {

    @Test
    fun `title is Enter your password`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState())
        }
        signInConfirmPassword {
            hasTitle("Enter your password")
        }
    }

    @Test
    fun `button is Sign In`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState())
        }
        signInConfirmPassword {
            hasSubmitButton("Sign In")
        }
    }

    @Test
    fun `username field is populated with username`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState(username = "testuser"))
        }
        signInConfirmPassword {
            hasUsername("testuser")
        }
    }

    @Test
    fun `has back to sign in footer`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState())
        }
        signInConfirmPassword {
            hasBackToSignInButton()
        }
    }

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState())
        }
    }

    @Test
    @ScreenshotTest
    fun `ready to submit`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState())
        }
        signInConfirmPassword {
            setPassword("password123")
        }
    }

    @Test
    @ScreenshotTest
    fun `ready to submit with email`() {
        setContent {
            SignInConfirmPassword(
                state = mockSignInConfirmPasswordState(username = "test@test.com", signInMethod = SignInMethod.Email)
            )
        }
        signInConfirmPassword {
            setPassword("password123")
        }
    }

    @Test
    @ScreenshotTest
    fun `ready to submit with phonenumber`() {
        setContent {
            SignInConfirmPassword(
                state = mockSignInConfirmPasswordState(
                    username = "+19021231234",
                    signInMethod = SignInMethod.PhoneNumber
                )
            )
        }
        signInConfirmPassword {
            setPassword("password123")
        }
    }

    @Test
    @ScreenshotTest
    fun `password visible`() {
        setContent {
            SignInConfirmPassword(state = mockSignInConfirmPasswordState())
        }
        signInConfirmPassword {
            setPassword("password123")
            clickShowPassword()
        }
    }
}
