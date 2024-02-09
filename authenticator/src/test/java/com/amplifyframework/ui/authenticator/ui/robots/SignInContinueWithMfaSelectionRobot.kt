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

import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.amplifyframework.auth.MFAType
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.authenticator.util.challengeResponse
import com.amplifyframework.ui.testing.ComposeTest

fun ComposeTest.signInContinueWithMfaSelection(func: SignInContinueWithMfaSelectionRobot.() -> Unit) =
    SignInContinueWithMfaSelectionRobot(composeTestRule).func()

class SignInContinueWithMfaSelectionRobot(composeTestRule: ComposeTestRule) : ScreenLevelRobot(composeTestRule) {
    fun hasSubmitButton(expected: String) = assertExists(TestTags.SignInConfirmButton, expected)
    fun hasMfaTypeSelected(expected: MFAType) {
        composeTestRule.onNode(hasParent(hasTestTag(expected.challengeResponse)) and isSelected()).assertExists()
    }

    fun selectMfaType(type: MFAType) = clickOnTag(type.challengeResponse)
    fun clickSubmitButton() = clickOnTag(TestTags.SignInConfirmButton)
    fun clickBackToSignInButton() = clickOnTag(TestTags.BackToSignInButton)
}
