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

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.testing.ComposeTest

fun ComposeTest.signUp(func: SignUpRobot.() -> Unit) = SignUpRobot(composeTestRule).func()

class SignUpRobot(rule: ComposeTestRule) : ScreenLevelRobot(rule) {
    fun hasSubmitButton(expected: String) = assertExists(TestTags.SignUpButton, expected)
    fun hasUsernameContentType(contentType: ContentType) = hasContentType(FieldKey.Username, contentType)
    fun hasPasswordContentType(contentType: ContentType) = hasContentType(FieldKey.Password, contentType)
    fun hasConfirmPasswordContentType(contentType: ContentType) = hasContentType(FieldKey.ConfirmPassword, contentType)

    fun setUsername(value: String) = setFieldContent(FieldKey.Username, value)
    fun setPassword(value: String) = setFieldContent(FieldKey.Password, value)
    fun setConfirmPassword(value: String) = setFieldContent(FieldKey.ConfirmPassword, value)
    fun setEmail(value: String) = setFieldContent(FieldKey.Email, value)
    fun clickShowPassword(fieldKey: FieldKey) = clickOnShowIcon(fieldKey)
    fun clickSubmitButton() = clickOnTag(TestTags.SignUpButton)
    fun clickBackToSignIn() = clickOnTag(TestTags.BackToSignInButton)
}
