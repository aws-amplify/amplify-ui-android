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
import androidx.compose.ui.text.input.KeyboardType
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.ui.authenticator.amplify.SignInMethod
import com.amplifyframework.ui.authenticator.amplify.toFieldKey

internal data class FormData(val fields: List<FieldConfig>)

internal fun buildForm(func: FormBuilderImpl.() -> Unit): FormData {
    return FormBuilderImpl().apply(func).build()
}

/**
 * Builder API for supplying custom form metadata for the signup form.
 */
interface SignUpFormBuilder {
    /**
     * Adds the standard username field.
     */
    fun username()
    fun password()
    fun confirmPassword()
    fun email(required: Boolean = false)
    fun phoneNumber(required: Boolean = false)
    fun birthdate(required: Boolean = false)
    fun familyName(required: Boolean = false)
    fun givenName(required: Boolean = false)
    fun middleName(required: Boolean = false)
    fun name(required: Boolean = false)
    fun nickname(required: Boolean = false)
    fun preferredUsername(required: Boolean = false)
    fun profile(required: Boolean = false)
    fun website(required: Boolean = false)

    fun text(
        key: FieldKey,
        label: String,
        hint: String? = null,
        required: Boolean = false,
        validator: FieldValidator = FieldValidators.None,
        keyboardType: KeyboardType = KeyboardType.Text,
        maxLines: Int = 1,
        maxLength: Int = 2048
    )

    fun date(
        key: FieldKey,
        label: String,
        hint: String? = null,
        required: Boolean = false,
        validator: FieldValidator = FieldValidators.date()
    )

    fun phone(
        key: FieldKey,
        label: String,
        hint: String? = null,
        required: Boolean = false,
        validator: FieldValidator = FieldValidators.phoneNumber()
    )

    fun custom(
        key: FieldKey,
        label: String,
        hint: String? = null,
        required: Boolean = false,
        validator: FieldValidator = FieldValidators.None,
        content: @Composable FieldScope.() -> Unit
    )
}

/**
 * Receiver context for [SignUpFormBuilder.custom] field content.
 */
interface FieldScope {
    /**
     * The [FieldState] for the custom field.
     */
    val fieldState: MutableFieldState

    /**
     * The [FormState] for the entire form.
     */
    val formState: FormState
}

internal class FormBuilderImpl : SignUpFormBuilder {
    // Note: Kotlin mutable map preserves entry order
    private val fields = mutableMapOf<FieldKey, FieldConfig>()

    override fun username() {
        this += FieldConfig.Text(
            key = FieldKey.Username,
            required = true,
            validator = FieldValidators.username()
        )
    }

    override fun password() = password(validator = FieldValidators.None)

    fun password(validator: FieldValidator) {
        this += FieldConfig.Password(
            key = FieldKey.Password,
            validator = validator
        )
    }

    override fun confirmPassword() {
        this += FieldConfig.Password(
            key = FieldKey.ConfirmPassword,
            validator = FieldValidators.confirmPassword()
        )
    }

    fun confirmationCode() {
        this += FieldConfig.Text(
            key = FieldKey.ConfirmationCode,
            validator = FieldValidators.confirmationCode(),
            keyboardType = KeyboardType.Number,
            maxLength = 6
        )
    }

    override fun email(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.Email,
            required = required,
            validator = FieldValidators.email(),
            keyboardType = KeyboardType.Email
        )
    }

    override fun phoneNumber(required: Boolean) {
        this += FieldConfig.PhoneNumber(
            key = FieldKey.PhoneNumber,
            required = required,
            validator = FieldValidators.phoneNumber()
        )
    }

    override fun birthdate(required: Boolean) {
        this += FieldConfig.Date(
            key = FieldKey.Birthdate,
            required = required,
            validator = FieldValidators.date()
        )
    }

    override fun familyName(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.FamilyName,
            required = required
        )
    }

    override fun givenName(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.GivenName,
            required = required
        )
    }

    override fun middleName(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.MiddleName,
            required = required
        )
    }

    override fun name(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.Name,
            required = required
        )
    }

    override fun nickname(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.Nickname,
            required = required
        )
    }

    override fun preferredUsername(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.PreferredUsername,
            required = required
        )
    }

    override fun profile(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.Profile,
            required = required
        )
    }

    override fun website(required: Boolean) {
        this += FieldConfig.Text(
            key = FieldKey.Website,
            required = required,
            validator = FieldValidators.webUrl(),
            keyboardType = KeyboardType.Uri
        )
    }

    override fun text(
        key: FieldKey,
        label: String,
        hint: String?,
        required: Boolean,
        validator: FieldValidator,
        keyboardType: KeyboardType,
        maxLines: Int,
        maxLength: Int
    ) {
        this += FieldConfig.Text(
            key = key,
            label = label,
            hint = hint,
            required = required,
            validator = validator,
            keyboardType = keyboardType,
            maxLines = maxLines,
            maxLength = maxLength
        )
    }

    override fun date(
        key: FieldKey,
        label: String,
        hint: String?,
        required: Boolean,
        validator: FieldValidator
    ) {
        this += FieldConfig.Date(
            key = key,
            label = label,
            hint = hint,
            required = required,
            validator = validator
        )
    }

    override fun phone(
        key: FieldKey,
        label: String,
        hint: String?,
        required: Boolean,
        validator: FieldValidator
    ) {
        this += FieldConfig.PhoneNumber(
            key = key,
            label = label,
            hint = hint,
            required = required,
            validator = validator
        )
    }

    override fun custom(
        key: FieldKey,
        label: String,
        hint: String?,
        required: Boolean,
        validator: FieldValidator,
        content: @Composable FieldScope.() -> Unit
    ) {
        this += FieldConfig.Custom(
            key = key,
            label = label,
            hint = hint,
            required = required,
            validator = validator,
            content = content
        )
    }

    fun verificationAttribute() {
        this += FieldConfig.Text(
            key = FieldKey.VerificationAttribute
        )
    }

    private operator fun plusAssign(config: FieldConfig) {
        fields[config.key] = config
    }

    fun replaceAndReorderFields(form: FormData) {
        val map = form.fields.associateBy { it.key }
        // Remove each existing instance of the field so that the form will be reordered as they are re-added
        map.keys.forEach { fields.remove(it) }
        fields.putAll(map)
    }

    fun markRequiredFields(
        signInMethod: SignInMethod,
        requiredKeys: List<AuthUserAttributeKey>
    ) {
        fields.replaceAll { fieldKey, config ->
            if (fieldKey is FieldKey.UserAttributeKey && requiredKeys.contains(fieldKey.attributeKey)) {
                config.required()
            } else if (fieldKey == signInMethod.toFieldKey()) {
                config.required()
            } else {
                config
            }
        }
    }

    fun build() = FormData(fields = fields.values.toList())
}

internal fun FormData.toState(): FormStateImpl {
    val state = FormStateImpl()
    fields.forEach { state.add(it) }
    return state
}

private fun FieldConfig.required() = when (this) {
    is FieldConfig.Text -> copy(required = true)
    is FieldConfig.Password -> copy(required = true)
    is FieldConfig.Date -> copy(required = true)
    is FieldConfig.PhoneNumber -> copy(required = true)
    is FieldConfig.Custom -> copy(required = true)
    else -> this
}
