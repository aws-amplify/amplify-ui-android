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

package com.amplifyframework.ui.authenticator

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.amplifyframework.ui.authenticator.theme.AmplifyTheme
import org.junit.Rule

abstract class ScreenshotTestBase {

    @get:Rule
    val screenshotRule = Paparazzi()

    protected open fun screenshot(name: String? = null, content: @Composable () -> Unit) =
        screenshotRule.snapshot(name) {
            AmplifyTheme {
                Surface {
                    content()
                }
            }
        }
}
