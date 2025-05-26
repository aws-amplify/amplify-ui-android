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

package com.amplifyframework.ui.testing

import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScreenshotTest

/**
 * Rule that captures a RoboRazzi screenshot only for annotated tests. To use this rule:
 *
 * 1. Extend the `ComposeTest` base class.
 * 2. Write UI tests.
 * 3. Annotate tests with `@ScreenshotTest` to have a screenshot automatically taken at the end of the test.
 *
 * Screenshot will be named "ClassName_function-name.png"
 */
class ScreenshotRule(val composeTestRule: ComposeTestRule) : TestRule {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(
            // Allow a 0.5% difference when comparing to allow for platform rendering differences
            changeThreshold = 0.005f
        )
    )

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            base.evaluate()
            if (description.getAnnotation(ScreenshotTest::class.java) != null) {
                composeTestRule.onNode(isRoot()).captureRoboImage(
                    file = File("src/test/screenshots", generateScreenshotName(description)),
                    roborazziOptions = options
                )
            }
        }
    }

    private fun generateScreenshotName(description: Description): String {
        val className = description.className.takeLastWhile { it != '.' }
        val methodName = description.methodName.replace("\\s+".toRegex(), "-")
        return "${className}_$methodName.png"
    }
}
