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

package com.amplifyframework.ui.authenticator.states

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.amplifyframework.ui.authenticator.forms.FieldValidatorScopeImpl
import com.amplifyframework.ui.authenticator.forms.FormStateImpl

/**
 * Base state for the interactive step states
 */
@Stable
internal abstract class BaseStateImpl {
    val form: FormStateImpl = FormStateImpl()

    private var busy: Boolean = false

    // Validates the form and marks the state as busy while invoking the
    // submission function
    protected suspend fun doSubmit(func: suspend () -> Unit) {
        if (!busy) {
            if (validate()) {
                busy = true
                form.enabled = false
                func()
                form.enabled = true
                busy = false
            }
        }
    }

    private fun validate(): Boolean {
        val validationScope = FieldValidatorScopeImpl(form.fields.mapValues { it.value.state.content })
        form.fields.values.forEach { field ->
            validationScope.content = field.state.content
            field.state.error = validationScope.run(field.validator)
        }
        return form.fields.values.all { it.state.error == null }
    }
}
