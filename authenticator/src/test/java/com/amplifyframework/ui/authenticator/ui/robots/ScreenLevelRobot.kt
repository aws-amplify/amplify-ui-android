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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.authenticator.ui.testTag
import com.amplifyframework.ui.testing.ComposeRobot

abstract class ScreenLevelRobot(rule: ComposeTestRule) : ComposeRobot(rule) {
    // Check that the composable has the expected title
    fun hasTitle(expected: String) = assertExists(TestTags.AuthenticatorTitle, expected)


    fun hasContentType(key: FieldKey, contentType: ContentType) = composeTestRule.onNode(
        hasTestTag(key.testTag) and SemanticsMatcher.expectValue(SemanticsProperties.ContentType, contentType)
    ).assertExists()

    fun setFieldContent(key: FieldKey, content: String) = writeTo(key.testTag, content)

    fun clickOnShowIcon(key: FieldKey) = composeTestRule.onNode(
        hasTestTag(TestTags.ShowPasswordIcon) and hasAnyAncestor(hasTestTag(key.testTag))
    ).performClick()

    fun field(key: FieldKey) = FieldLevelRobot(key, composeTestRule)
}

class FieldLevelRobot(private val fieldKey: FieldKey, private val composeTestRule: ComposeTestRule) {
    private val composeRobot = ComposeRobot(composeTestRule)

    // Check that the given field has the specified content type set
    fun hasContentType(contentType: ContentType) = composeTestRule.onNode(
        hasTestTag(fieldKey.testTag) and SemanticsMatcher.expectValue(SemanticsProperties.ContentType, contentType)
    ).assertExists()

    fun setContent(content: String) = composeRobot.writeTo(fieldKey.testTag, content)
}