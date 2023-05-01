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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.KeyboardType

/**
 * An [FieldConfig] specifies the metadata for an input field within the Authenticator composable. The
 * various concrete types represent the different types of supported fields.
 */
@Immutable
abstract class FieldConfig private constructor() {

    /**
     * The identifier key for this field.
     */
    abstract val key: FieldKey

    /**
     * Whether this field requires a non-blank value to submit the form.  If [required] is
     * true then the [validator] will be prepended with a validator that checks that the
     * field content is non-blank. If false, the [label] will have the optional string marker appended.
     */
    abstract val required: Boolean

    /**
     * The [FieldValidator] instance for this field. If [required] is true
     * this validator will be prepended with a check that the field content is non-blank.
     */
    abstract val validator: FieldValidator

    /**
     * The label for this field. This must be non-null for custom fields. Standard fields will instead
     * read their label from resources.
     */
    abstract val label: String?

    /**
     * The hint for this field. This may be non-null for custom fields. Standard fields will instead
     * read their hint from resources.
     */
    abstract val hint: String?

    /**
     * Configuration instance for a plain text input.
     * @param key The identifier key for this field.
     * @param required Whether this field requires a non-blank value to submit the form.
     * @param validator The [FieldValidator] instance for this field.
     * @param label The label for this field. This is null for standard fields.
     * @param hint The hint for this field. This is null for standard fields.
     * @param keyboardType The type of soft keyboard that is shown when the field is focused.
     * @param maxLines The maximum number of lines the field will expand to show.
     * @param maxLength The maximum number of characters that can be input into this field.
     */
    @Immutable
    data class Text(
        override val key: FieldKey,
        override val required: Boolean = true,
        override val validator: FieldValidator = FieldValidators.None,
        override val label: String? = null,
        override val hint: String? = null,
        val keyboardType: KeyboardType = KeyboardType.Text,
        val maxLines: Int = 1,
        val maxLength: Int = 2048
    ) : FieldConfig()

    /**
     * Configuration object for a password input
     * @param key The identifier key for this field.
     * @param required Whether this field requires a non-blank value to submit the form.
     * @param validator The [FieldValidator] instance for this field.
     * @param label The label for this field. This is null for standard fields.
     * @param hint The hint for this field. This is null for standard fields.
     * @param keyboardType The type of soft keyboard that is shown when the field is focused.
     */
    @Immutable
    data class Password(
        override val key: FieldKey,
        override val required: Boolean = true,
        override val validator: FieldValidator = FieldValidators.None,
        override val label: String? = null,
        override val hint: String? = null,
        val keyboardType: KeyboardType = KeyboardType.Password
    ) : FieldConfig()

    /**
     * Configuration object for a date input
     * @param key The identifier key for this field.
     * @param required Whether this field requires a non-blank value to submit the form.
     * @param validator The [FieldValidator] instance for this field.
     * @param label The label for this field. This is null for standard fields.
     * @param hint The hint for this field. This is null for standard fields.
     */
    @Immutable
    data class Date(
        override val key: FieldKey,
        override val required: Boolean = true,
        override val validator: FieldValidator = FieldValidators.date(),
        override val label: String? = null,
        override val hint: String? = null
    ) : FieldConfig()

    /**
     * Configuration object for a phone number input
     * @param key The identifier key for this field.
     * @param required Whether this field requires a non-blank value to submit the form.
     * @param validator The [FieldValidator] instance for this field.
     * @param label The label for this field. This is null for standard fields.
     * @param hint The hint for this field. This is null for standard fields.
     */
    @Immutable
    data class PhoneNumber(
        override val key: FieldKey,
        override val required: Boolean = true,
        override val validator: FieldValidator = FieldValidators.phoneNumber(),
        override val label: String? = null,
        override val hint: String? = null
    ) : FieldConfig()

    /**
     * Configuration object for a custom fields
     * @param key The identifier key for this field.
     * @param required Whether this field requires a non-blank value to submit the form.
     * @param validator The [FieldValidator] instance for this field.
     * @param label The label for this field.
     * @param hint The hint for this field.
     * @param content The composable content for this field. The content composable has a receiver of [FieldScope]
     *                that provides the state holder instance for the field.
     */
    @Immutable
    data class Custom(
        override val key: FieldKey,
        override val required: Boolean = false,
        override val validator: FieldValidator = FieldValidators.None,
        override val label: String,
        override val hint: String?,
        val content: @Composable FieldScope.() -> Unit
    ) : FieldConfig()
}
