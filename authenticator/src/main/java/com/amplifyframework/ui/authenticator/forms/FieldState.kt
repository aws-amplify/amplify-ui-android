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

/**
 * The [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for a single
 * field within a form.
 */
@Stable
interface FieldState {
    /**
     * The content value of the field.
     */
    val content: String

    /**
     * The error, if any, associated with this field. Errors may represent client-side validation errors or certain
     * classes of server-side error responses.
     */
    val error: FieldError?
}

/**
 * The mutable [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for a single
 * field within a form.
 */
@Stable
interface MutableFieldState : FieldState {
    /**
     * The mutable content value of the field.
     */
    override var content: String

    /**
     * The error, if any, associated with this field. Errors may represent client-side validation errors or certain
     * classes of server-side error responses.
     */
    override var error: FieldError?
}

/**
 * The [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for a password field.
 */
@Stable
interface PasswordFieldState : FieldState {
    /**
     * Flag indicating whether the field content is visible.
     */
    val fieldContentVisible: Boolean
}

/**
 * The mutable [state holder](https://developer.android.com/jetpack/compose/state#managing-state) for a password field.
 */
@Stable
interface MutablePasswordFieldState : PasswordFieldState, MutableFieldState {
    /**
     * Flag indicating whether the field content is visible.
     */
    override var fieldContentVisible: Boolean
}

internal open class FieldStateImpl constructor(
    initialValue: String = ""
) : MutableFieldState {
    override var content by mutableStateOf(initialValue)
    override var error by mutableStateOf<FieldError?>(null)
}

internal class PasswordFieldStateImpl(
    initialFieldContentVisible: Boolean = false
) : FieldStateImpl(), MutablePasswordFieldState {
    override var fieldContentVisible by mutableStateOf(initialFieldContentVisible)
}
