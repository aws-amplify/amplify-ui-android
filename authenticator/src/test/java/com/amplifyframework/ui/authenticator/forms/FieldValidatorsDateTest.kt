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

package com.amplifyframework.ui.authenticator.forms

import io.kotest.matchers.shouldBe
import org.junit.Test

class FieldValidatorsDateTest {

    private fun validate(content: String): FieldError? {
        val scope = object : FieldValidatorScope {
            override val content = content
            override val formContent = emptyMap<FieldKey, String>()
        }
        return FieldValidators.date().invoke(scope)
    }

    @Test
    fun `a valid ISO date passes`() {
        validate("2023-06-15") shouldBe null
    }

    @Test
    fun `a blank value passes`() {
        validate("") shouldBe null
    }

    @Test
    fun `an impossible calendar date is rejected`() {
        validate("2023-02-29") shouldBe FieldError.InvalidFormat
    }

    @Test
    fun `an out of range month is rejected`() {
        validate("2023-13-01") shouldBe FieldError.InvalidFormat
    }

    @Test
    fun `a non zero padded date is rejected`() {
        validate("2023-1-5") shouldBe FieldError.InvalidFormat
    }

    @Test
    fun `a non date string is rejected`() {
        validate("not-a-date") shouldBe FieldError.InvalidFormat
    }

    @Test
    fun `a valid date with trailing garbage is rejected`() {
        validate("2023-06-15garbage") shouldBe FieldError.InvalidFormat
    }
}
