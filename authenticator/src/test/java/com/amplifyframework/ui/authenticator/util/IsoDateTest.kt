/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.ui.authenticator.ui.epochMillisToIsoDate
import io.kotest.matchers.shouldBe
import org.junit.Test

class IsoDateTest {

    @Test
    fun `epoch zero maps to unix epoch date`() {
        epochMillisToIsoDate(0L) shouldBe "1970-01-01"
    }

    @Test
    fun `one day of millis maps to the next day`() {
        epochMillisToIsoDate(86_400_000L) shouldBe "1970-01-02"
    }

    @Test
    fun `a UTC millisecond value maps to the expected date`() {
        epochMillisToIsoDate(1_686_787_200_000L) shouldBe "2023-06-15"
    }

    @Test
    fun `a leap day is formatted correctly`() {
        epochMillisToIsoDate(1_582_934_400_000L) shouldBe "2020-02-29"
    }
}
