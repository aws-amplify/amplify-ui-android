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

import androidx.compose.runtime.Immutable

/**
 * The various errors that can occur during password validation checks.
 */
interface PasswordError {
    /**
     * Password does not meet the minimum length threshold.
     */
    class InvalidPasswordLength(val minimumLength: Int) : PasswordError

    /**
     * Password requires at least one digit.
     */
    object InvalidPasswordMissingNumber : PasswordError

    /**
     * Password requires at least one special character.
     */
    object InvalidPasswordMissingSpecial : PasswordError

    /**
     * Password requires at least one uppercase letter.
     */
    object InvalidPasswordMissingUpper : PasswordError

    /**
     * Password requires at least one lowercase letter.
     */
    object InvalidPasswordMissingLower : PasswordError
}

abstract class FieldError private constructor() {
    /**
     * The submitted field value does not exist in the Auth backend.
     */
    object NotFound : FieldError()

    /**
     * A required field is missing a value.
     */
    object MissingRequired : FieldError()

    /**
     * The confirm password field value does not match the password field value.
     */
    object PasswordsDoNotMatch : FieldError()

    /**
     * The field value does not conform to the expected format.
     */
    object InvalidFormat : FieldError()

    /**
     * A field value conflicts with an existing user account, for example the user tried to sign up
     * with a username that already exists.
     */
    object FieldValueExists : FieldError()

    /**
     * The user entered a confirmation code that is incorrect.
     */
    object ConfirmationCodeIncorrect : FieldError()

    /**
     * A custom validation error.
     */
    class Custom(val message: String) : FieldError()

    /**
     * A password failed to meet the configured criteria.
     * @param errors The
     */
    @Immutable
    data class InvalidPassword internal constructor(
        val errors: List<PasswordError>
    ) : FieldError()
}
