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
import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockSignInSelectAuthFactorState
import com.amplifyframework.ui.authenticator.ui.robots.signInSelectAuthFactor
import com.amplifyframework.ui.testing.ScreenshotTest
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SignInSelectAuthFactorTest : AuthenticatorUiTest() {

    @Test
    fun `title is Choose how to sign in`() {
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState())
        }
        signInSelectAuthFactor {
            hasTitle("Choose how to sign in")
        }
    }

    @Test
    fun `username field is populated`() {
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState(username = "testuser"))
        }
        signInSelectAuthFactor {
            hasUsername("testuser")
        }
    }

    @Test
    fun `shows password button when available`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(availableAuthFactors = setOf(AuthFactor.Password()))
            )
        }
        signInSelectAuthFactor {
            hasPasswordButton()
        }
    }

    @Test
    fun `shows passkey button when available`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(
                    availableAuthFactors = setOf(AuthFactor.WebAuthn)
                )
            )
        }
        signInSelectAuthFactor {
            hasPasskeyButton()
        }
    }

    @Test
    fun `shows email button when available`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(
                    availableAuthFactors = setOf(AuthFactor.EmailOtp)
                )
            )
        }
        signInSelectAuthFactor {
            hasEmailButton()
        }
    }

    @Test
    fun `shows sms button when available`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(
                    availableAuthFactors = setOf(AuthFactor.SmsOtp)
                )
            )
        }
        signInSelectAuthFactor {
            hasSmsButton()
        }
    }

    @Test
    fun `selects password`() {
        val onSelect = mockk<(AuthFactor) -> Unit>(relaxed = true)
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState(onSelect = onSelect))
        }
        signInSelectAuthFactor {
            clickOnAuthFactor(AuthFactor.Password())
        }
        verify {
            onSelect(withArg { it.shouldBeInstanceOf<AuthFactor.Password>() })
        }
    }

    @Test
    fun `selects sms otp`() {
        val onSelect = mockk<(AuthFactor) -> Unit>(relaxed = true)
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState(onSelect = onSelect))
        }
        signInSelectAuthFactor {
            clickOnAuthFactor(AuthFactor.SmsOtp)
        }
        verify {
            onSelect(AuthFactor.SmsOtp)
        }
    }

    @Test
    fun `selects email otp`() {
        val onSelect = mockk<(AuthFactor) -> Unit>(relaxed = true)
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState(onSelect = onSelect))
        }
        signInSelectAuthFactor {
            clickOnAuthFactor(AuthFactor.EmailOtp)
        }
        verify {
            onSelect(AuthFactor.EmailOtp)
        }
    }

    @Test
    fun `selects passkey`() {
        val onSelect = mockk<(AuthFactor) -> Unit>(relaxed = true)
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState(onSelect = onSelect))
        }
        signInSelectAuthFactor {
            clickOnAuthFactor(AuthFactor.WebAuthn)
        }
        verify {
            onSelect(AuthFactor.WebAuthn)
        }
    }

    @Test
    @ScreenshotTest
    fun `default state with all factors`() {
        setContent {
            SignInSelectAuthFactor(state = mockSignInSelectAuthFactorState())
        }
    }

    @Test
    @ScreenshotTest
    fun `default state with all factors with phone number`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(
                    username = "+19021234567",
                    signInMethod = SignInMethod.PhoneNumber
                )
            )
        }
    }

    @Test
    @ScreenshotTest
    fun `default state with all factors with email`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(
                    username = "test@test.com",
                    signInMethod = SignInMethod.Email
                )
            )
        }
    }

    @Test
    @ScreenshotTest
    fun `no password`() {
        setContent {
            SignInSelectAuthFactor(
                state = mockSignInSelectAuthFactorState(
                    availableAuthFactors = setOf(AuthFactor.EmailOtp, AuthFactor.SmsOtp)
                )
            )
        }
    }
}
