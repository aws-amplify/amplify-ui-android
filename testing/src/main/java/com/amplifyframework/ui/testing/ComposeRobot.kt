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

package com.amplifyframework.ui.testing

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText

/**
 * Base class for a [compose testing robot](https://proandroiddev.com/jetpack-compose-with-robot-testing-pattern-ad6293335a20)
 * pattern. Contains common matchers that can be used in specific implementations.
 *
 * When using a robot implementation for UI testing the test class should specify _what_ is being asserted, and the
 * robot specifies _how_ it is asserted.
 */
open class ComposeRobot(protected val composeTestRule: ComposeTestRule) {
    fun assertExists(text: String) = composeTestRule.onNodeWithText(text).assertExists()

    fun assertExists(tag: String, text: String) {
        composeTestRule.onNode(hasTestTag(tag) and hasText(text)).assertExists()
    }
}
