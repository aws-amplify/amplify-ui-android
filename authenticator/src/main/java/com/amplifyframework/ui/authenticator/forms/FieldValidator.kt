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

package com.amplifyframework.ui.authenticator.forms

import android.util.Patterns
import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.forms.FieldError.InvalidFormat
import com.amplifyframework.ui.authenticator.forms.FieldError.PasswordsDoNotMatch
import com.amplifyframework.ui.authenticator.forms.FieldKey.Password
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

/**
 * The receiver scope for a [FieldValidator]. Allows access to the content being validated and the other fields in the form.
 */
interface FieldValidatorScope {
    val content: String
    val formContent: Map<FieldKey, String>
}

/**
 * A validation function for a field.
 * @return A [FieldError] instance if the field content is invalid, or null otherwise.
 */
typealias FieldValidator = FieldValidatorScope.() -> FieldError?

/**
 * Concatenate two field validators together. This validator will run first, and the [other] validator will run
 * if this validator passes.
 * @param other A [FieldValidator] to run if this validator does not produce an error.
 */
internal operator fun FieldValidator.plus(other: FieldValidator): FieldValidator = { this@plus() ?: other() }

/**
 * Common [FieldValidator] implementations.
 */
internal object FieldValidators {

    private val usernamePattern = """[\p{L}\p{M}\p{S}\p{N}\p{P}]+""".toPattern()
    private val confirmationCodePattern = """\d{6}""".toPattern()
    private val specialRegex = """[\^\$\{}\*\.\[\]\{}\(\)\?\-"!@#%&/\\,><':;|_~`+=\s]+""".toRegex()
    private val numbersRegex = "\\d+".toRegex()
    private val upperRegex = "[A-Z]+".toRegex()
    private val lowerRegex = "[a-z]+".toRegex()
    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * The empty [FieldValidator] instance. This never returns an error.
     */
    val None: FieldValidator = { null }

    internal fun required(
        error: FieldError = FieldError.MissingRequired
    ): FieldValidator = {
        if (content.isBlank()) error else null
    }

    private fun matchingField(
        other: FieldKey,
        error: FieldError
    ): FieldValidator = {
        if (content != formContent[other]) error else null
    }

    fun confirmPassword() = matchingField(Password, PasswordsDoNotMatch)

    private fun pattern(
        pattern: Pattern,
        error: FieldError = InvalidFormat
    ): FieldValidator = {
        if (content.isNotBlank() && !pattern.matcher(content).matches()) error else null
    }

    fun username() = pattern(usernamePattern)
    fun email() = pattern(Patterns.EMAIL_ADDRESS)
    fun phoneNumber() = pattern(Patterns.PHONE)
    fun webUrl() = pattern(Patterns.WEB_URL)

    fun date(
        error: FieldError = InvalidFormat
    ): FieldValidator = {
        if (content.isNotBlank()) {
            try {
                dateFormat.parse(content)
                null
            } catch (e: DateTimeParseException) {
                error
            }
        } else {
            null
        }
    }

    internal fun confirmationCode() = pattern(confirmationCodePattern)

    internal fun password(
        criteria: PasswordCriteria
    ): FieldValidator = {
        if (content.isNotBlank()) {
            val potentialErrors = mutableListOf<PasswordError>()
            if (content.length < criteria.length) {
                potentialErrors.add(
                    PasswordError.InvalidPasswordLength(criteria.length)
                )
            }

            if (criteria.requiresSpecial && !specialRegex.containsMatchIn(content)) {
                potentialErrors.add(PasswordError.InvalidPasswordMissingSpecial)
            }

            if (criteria.requiresNumber && !numbersRegex.containsMatchIn(content)) {
                potentialErrors.add(PasswordError.InvalidPasswordMissingNumber)
            }

            if (criteria.requiresUpper && !upperRegex.containsMatchIn(content)) {
                potentialErrors.add(PasswordError.InvalidPasswordMissingUpper)
            }
            if (criteria.requiresLower && !lowerRegex.containsMatchIn(content)) {
                potentialErrors.add(PasswordError.InvalidPasswordMissingLower)
            }

            if (potentialErrors.isNotEmpty()) {
                FieldError.InvalidPassword(potentialErrors)
            } else {
                null
            }
        } else {
            null
        }
    }
}
