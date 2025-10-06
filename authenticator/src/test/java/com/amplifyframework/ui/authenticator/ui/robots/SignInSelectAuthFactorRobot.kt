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

package com.amplifyframework.ui.authenticator.ui.robots

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.authenticator.ui.testTag
import com.amplifyframework.ui.testing.ComposeTest

fun ComposeTest.signInSelectAuthFactor(func: SignInSelectAuthFactorRobot.() -> Unit) =
    SignInSelectAuthFactorRobot(composeTestRule).func()

class SignInSelectAuthFactorRobot(rule: ComposeTestRule) : ScreenLevelRobot(rule) {
    fun hasUsername(expected: String) = composeTestRule.onNode(
        hasTestTag(FieldKey.Username.testTag) and hasText(expected)
    ).assertExists()

    fun hasPasswordButton() = composeTestRule.onNode(hasTestTag(TestTags.AuthFactorPassword)).assertExists()
    fun hasPasskeyButton() = composeTestRule.onNode(hasTestTag(TestTags.AuthFactorPasskey)).assertExists()
    fun hasEmailButton() = composeTestRule.onNode(hasTestTag(TestTags.AuthFactorEmail)).assertExists()
    fun hasSmsButton() = composeTestRule.onNode(hasTestTag(TestTags.AuthFactorSms)).assertExists()

    fun clickOnAuthFactor(factor: AuthFactor) = clickOnTag(factor.testTag)
}
