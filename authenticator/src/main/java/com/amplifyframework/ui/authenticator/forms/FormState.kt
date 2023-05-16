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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.amplifyframework.auth.AuthUserAttribute

/**
 * A [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for an input form within the
 * Authenticator.
 */
@Stable
interface FormState {
    /**
     * The individual fields within a form. Each field is uniquely identified by a [FieldKey], which maps to a [FieldData]
     * holding configuration options and state for the field.
     */
    val fields: Map<FieldKey, FieldData>

    /**
     * True if the fields in this form should be enabled.
     */
    val enabled: Boolean
}

/**
 * A mutable [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for an input form within the
 * Authenticator.
 */
@Stable
interface MutableFormState : FormState {
    /**
     * The individual fields within a form. Each field is uniquely identified by a [FieldKey], which maps to a [FieldData]
     * holding configuration options and mutable state for the field.
     */
    override val fields: Map<FieldKey, MutableFieldData>

    /**
     * True if the fields in this form should be enabled.
     */
    override var enabled: Boolean
}

/**
 * Holds the data relevant to a single field within a [FormState].
 */
@Stable
interface FieldData {
    /**
     * The configuration options for the field.
     */
    val config: FieldConfig

    /**
     * The [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for the field.
     */
    val state: FieldState

    /**
     * The validator function that determines whether the field's contents are valid prior to form submission.
     */
    val validator: FieldValidator
}

/**
 * Holds the data relevant to a single field within a [MutableFormState].
 */
@Stable
interface MutableFieldData : FieldData {
    /**
     * The mutable [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for the field.
     */
    override val state: MutableFieldState
}

internal class FormStateImpl : MutableFormState {

    // Backing property for the fields map
    private val _fields = mutableMapOf<FieldKey, FieldDataImpl>()
    override val fields = _fields

    override var enabled by mutableStateOf(true)

    fun add(config: FieldConfig) {
        // Prepend the required validator for any fields that are required
        var fieldValidator = config.validator
        if (config.required) {
            fieldValidator = FieldValidators.required() + fieldValidator
        }

        val state = when (config) {
            is FieldConfig.Password -> PasswordFieldStateImpl()
            else -> FieldStateImpl()
        }

        fields[config.key] = FieldDataImpl(
            config = config,
            state = state,
            validator = fieldValidator
        )
    }

    fun addFields(func: FormBuilderImpl.() -> Unit) {
        buildForm(func).fields.forEach { add(it) }
    }

    fun getTrimmed(key: FieldKey): String? {
        return fields[key]?.state?.content?.trim()
    }

    fun getUserAttributes() = fields.mapNotNull { (key, field) ->
        if (key is FieldKey.UserAttributeKey) {
            AuthUserAttribute(key.attributeKey, field.state.content.trim())
        } else {
            null
        }
    }
}

internal class FieldDataImpl(
    override val config: FieldConfig,
    override val state: FieldStateImpl,
    override val validator: FieldValidator
) : MutableFieldData

internal fun FormState.setFieldError(fieldKey: FieldKey, error: FieldError) {
    val impl = this as FormStateImpl
    impl.fields[fieldKey]?.state?.error = error
}

internal class FieldValidatorScopeImpl(
    override val formContent: Map<FieldKey, String>
) : FieldValidatorScope {
    override var content: String = ""
}
