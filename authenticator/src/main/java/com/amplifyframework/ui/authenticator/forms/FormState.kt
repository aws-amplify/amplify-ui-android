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
 * The state holder class for an input form within the Authenticator. Holds a collection of fields as well as
 * some state values relating to the entire form.
 */
@Stable
interface FormState {
    val fields: Map<FieldKey, FieldData>
    val submitting: Boolean
    val fieldsHidden: Boolean
}

@Stable
interface MutableFormState : FormState {
    override val fields: Map<FieldKey, MutableFieldData>
    fun toggleHiddenFields()
}

@Stable
interface FieldData {
    val config: FieldConfig
    val state: FieldState
    val validator: FieldValidator
}

@Stable
interface MutableFieldData : FieldData {
    override val state: MutableFieldState
}

internal class FormStateImpl : MutableFormState {

    // Backing property for the fields map
    private val _fields = mutableMapOf<FieldKey, FieldDataImpl>()
    override val fields = _fields

    override var submitting by mutableStateOf(false)

    override var fieldsHidden by mutableStateOf(true)

    private var onSubmit: suspend () -> Unit = {}

    fun add(config: FieldConfig) {
        // Prepend the required validator for any fields that are required
        var fieldValidator = config.validator
        if (config.required) {
            fieldValidator = FieldValidators.required() + fieldValidator
        }

        fields[config.key] = FieldDataImpl(
            config = config,
            state = FieldStateImpl(),
            validator = fieldValidator
        )
    }

    fun validate(): Boolean {
        val validationScope = FieldValidatorScopeImpl(fields.mapValues { it.value.state.content })
        _fields.values.forEach { field ->
            validationScope.content = field.state.content
            field.state.error = validationScope.run(field.validator)
        }
        return fields.values.all { it.state.error == null }
    }

    fun getContent(key: FieldKey): String? {
        return fields[key]?.state?.content
    }

    override fun toggleHiddenFields() {
        fieldsHidden = !fieldsHidden
    }

    suspend fun submit() {
        if (validate()) {
            onSubmit()
        }
    }

    fun getUserAttributes() = fields.mapNotNull { (key, field) ->
        if (key is FieldKey.UserAttributeKey) {
            AuthUserAttribute(key.attributeKey, field.state.content.trim())
        } else {
            null
        }
    }

    fun onSubmit(func: suspend () -> Unit) {
        onSubmit = func
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

internal inline fun FormState.markSubmitting(
    body: () -> Unit
) {
    val form = this as FormStateImpl
    form.submitting = true
    body()
    form.submitting = false
}

internal class FieldValidatorScopeImpl(
    override val formContent: Map<FieldKey, String>
) : FieldValidatorScope {
    override var content: String = ""
}
