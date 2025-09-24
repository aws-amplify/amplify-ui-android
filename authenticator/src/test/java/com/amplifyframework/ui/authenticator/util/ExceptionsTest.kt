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

package com.amplifyframework.ui.authenticator.util

import io.kotest.matchers.shouldBe
import org.junit.Test

class ExceptionsTest {

    @Test
    fun `InvalidConfigurationException maps to the expected resource name`() {
        val exception = InvalidConfigurationException("test", null)
        exception.toResourceName() shouldBe "amplify_ui_authenticator_error_invalid_configuration"
    }
}
