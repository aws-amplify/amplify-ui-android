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

import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldValidator
import com.amplifyframework.ui.authenticator.forms.MutableFieldData
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.forms.MutableFormState
import com.amplifyframework.ui.authenticator.forms.MutablePasswordFieldState

fun mockForm(
    vararg fields: MutableFieldData,
    enabled: Boolean = true
) = object : MutableFormState {
    override val fields = fields.associateBy { it.config.key }
    override var enabled = enabled
}

fun mockFieldData(
    config: FieldConfig,
    state: MutableFieldState = mockFieldState(),
    validator: FieldValidator = { null }
) = object : MutableFieldData {
    override val state = state
    override val config = config
    override val validator = validator
}

fun mockFieldState(
    content: String = "",
    error: FieldError? = null
) = object : MutableFieldState {
    override var content = content
    override var error = error
}

fun mockPasswordFieldState(
    content: String = "",
    error: FieldError? = null,
    visible: Boolean = false
) = object : MutablePasswordFieldState {
    override var fieldContentVisible = visible
    override var content = content
    override var error: FieldError? = error
}
