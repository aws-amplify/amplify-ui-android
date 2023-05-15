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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.amplifyframework.ui.authenticator.theme.AmplifyTheme
import org.junit.Rule

abstract class ScreenshotTestBase {

    @get:Rule
    val screenshotRule = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        showSystemUi = false
    )

    protected open fun screenshot(name: String? = null, content: @Composable () -> Unit) =
        screenshotRule.snapshot(name) {
            AmplifyTheme {
                Surface {
                    Box(modifier = Modifier.padding(top = 56.dp)) {
                        content()
                    }
                }
            }
        }
}
