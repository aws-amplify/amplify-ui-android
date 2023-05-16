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
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.auth.toFieldKey

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

    /**
     * Adds the standard password field.
     */
    fun password()

    /**
     * Adds the standard password confirmation field.
     */
    fun confirmPassword()

    /**
     * Adds the standard email field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun email(required: Boolean = false)

    /**
     * Adds the standard phone number field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun phoneNumber(required: Boolean = false)

    /**
     * Adds the standard birthdate field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun birthdate(required: Boolean = false)

    /**
     * Adds the standard family name field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun familyName(required: Boolean = false)

    /**
     * Adds the standard given name field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun givenName(required: Boolean = false)

    /**
     * Adds the standard middle name field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun middleName(required: Boolean = false)

    /**
     * Adds the standard name field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun name(required: Boolean = false)

    /**
     * Adds the standard nickname field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun nickname(required: Boolean = false)

    /**
     * Adds the standard preferred username field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun preferredUsername(required: Boolean = false)

    /**
     * Adds the standard profile field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
    fun profile(required: Boolean = false)

    /**
     * Adds the standard website field.
     * @param required Set to true to mark the field as required. Default is false. Note that this value will be
     * overridden if this field is marked as a required sign-up field in your configuration.
     */
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
     * The [MutableFieldState] for the custom field.
     */
    val fieldState: MutableFieldState

    /**
     * The read-only [FormState] for the entire form.
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

    fun fieldForSignInMethod(method: SignInMethod) = when (method) {
        SignInMethod.Username -> username()
        SignInMethod.Email -> email(required = true)
        SignInMethod.PhoneNumber -> phoneNumber(required = true)
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

private fun FieldConfig.required() = when (this) {
    is FieldConfig.Text -> copy(required = true)
    is FieldConfig.Password -> copy(required = true)
    is FieldConfig.Date -> copy(required = true)
    is FieldConfig.PhoneNumber -> copy(required = true)
    is FieldConfig.Custom -> copy(required = true)
    else -> this
}
