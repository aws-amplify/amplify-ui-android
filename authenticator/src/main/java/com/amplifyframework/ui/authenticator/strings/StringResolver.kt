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

package com.amplifyframework.ui.authenticator.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.amplifyframework.auth.AuthException
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.PasswordError
import com.amplifyframework.ui.authenticator.locals.LocalStringResolver
import com.amplifyframework.ui.authenticator.util.toResourceName
import kotlin.reflect.KClass

internal open class StringResolver {
    // Avoid recomputing the same error message for each type of exception
    private val cachedErrorMessages = mutableMapOf<KClass<out AuthException>, String>()

    @Composable
    @ReadOnlyComposable
    open fun label(config: FieldConfig): String {
        var label = title(config)
        if (!config.required) {
            label = stringResource(R.string.amplify_ui_authenticator_field_optional, label)
        }
        return label
    }

    @Composable
    @ReadOnlyComposable
    private fun title(config: FieldConfig): String = config.label ?: when (config.key) {
        FieldKey.ConfirmPassword -> stringResource(R.string.amplify_ui_authenticator_field_label_password_confirm)
        FieldKey.ConfirmationCode -> stringResource(R.string.amplify_ui_authenticator_field_label_confirmation_code)
        FieldKey.Password -> stringResource(R.string.amplify_ui_authenticator_field_label_password)
        FieldKey.PhoneNumber -> stringResource(R.string.amplify_ui_authenticator_field_label_phone_number)
        FieldKey.Email -> stringResource(R.string.amplify_ui_authenticator_field_label_email)
        FieldKey.Username -> stringResource(R.string.amplify_ui_authenticator_field_label_username)
        FieldKey.Birthdate -> stringResource(R.string.amplify_ui_authenticator_field_label_birthdate)
        FieldKey.FamilyName -> stringResource(R.string.amplify_ui_authenticator_field_label_family_name)
        FieldKey.GivenName -> stringResource(R.string.amplify_ui_authenticator_field_label_given_name)
        FieldKey.MiddleName -> stringResource(R.string.amplify_ui_authenticator_field_label_middle_name)
        FieldKey.Name -> stringResource(R.string.amplify_ui_authenticator_field_label_name)
        FieldKey.Website -> stringResource(R.string.amplify_ui_authenticator_field_label_website)
        FieldKey.PhoneNumber -> stringResource(R.string.amplify_ui_authenticator_field_label_phone_number)
        FieldKey.Nickname -> stringResource(R.string.amplify_ui_authenticator_field_label_nickname)
        FieldKey.PreferredUsername ->
            stringResource(R.string.amplify_ui_authenticator_field_label_preferred_username)
        FieldKey.Profile -> stringResource(R.string.amplify_ui_authenticator_field_label_profile)
        FieldKey.VerificationAttribute ->
            stringResource(R.string.amplify_ui_authenticator_field_label_verification_attribute)
        else -> ""
    }

    @Composable
    @ReadOnlyComposable
    open fun hint(config: FieldConfig): String? = config.hint ?: when {
        config.key == FieldKey.ConfirmPassword ->
            stringResource(R.string.amplify_ui_authenticator_field_hint_password_confirm)
        config is FieldConfig.Date -> "yyyy-mm-dd"
        else -> {
            val label = label(config)
            stringResource(R.string.amplify_ui_authenticator_field_hint, label)
        }
    }

    @Composable
    @ReadOnlyComposable
    open fun error(config: FieldConfig, error: FieldError): String = when (error) {
        is FieldError.InvalidPassword -> {
            var errorText = stringResource(R.string.amplify_ui_authenticator_field_password_requirements)
            error.errors.forEach {
                errorText += "\n" +
                    when (it) {
                        is PasswordError.InvalidPasswordLength ->
                            pluralStringResource(
                                id = R.plurals.amplify_ui_authenticator_field_password_too_short,
                                count = it.minimumLength,
                                it.minimumLength
                            )
                        PasswordError.InvalidPasswordMissingSpecial ->
                            stringResource(R.string.amplify_ui_authenticator_field_password_missing_special)
                        PasswordError.InvalidPasswordMissingNumber ->
                            stringResource(R.string.amplify_ui_authenticator_field_password_missing_number)
                        PasswordError.InvalidPasswordMissingUpper ->
                            stringResource(R.string.amplify_ui_authenticator_field_password_missing_upper)
                        PasswordError.InvalidPasswordMissingLower ->
                            stringResource(R.string.amplify_ui_authenticator_field_password_missing_lower)
                        else -> ""
                    }
            }
            errorText
        }
        FieldError.PasswordsDoNotMatch ->
            stringResource(R.string.amplify_ui_authenticator_field_warn_unmatched_password)
        FieldError.MissingRequired -> {
            val label = title(config)
            stringResource(R.string.amplify_ui_authenticator_field_warn_empty, label)
        }
        FieldError.InvalidFormat -> {
            val label = title(config)
            stringResource(R.string.amplify_ui_authenticator_field_warn_invalid_format, label)
        }
        FieldError.FieldValueExists -> {
            val label = title(config)
            stringResource(R.string.amplify_ui_authenticator_field_warn_existing, label)
        }
        FieldError.ConfirmationCodeIncorrect -> {
            stringResource(R.string.amplify_ui_authenticator_field_warn_incorrect_code)
        }
        is FieldError.Custom -> error.message
        FieldError.NotFound -> {
            val label = title(config)
            stringResource(R.string.amplify_ui_authenticator_field_warn_not_found, label)
        }
        else -> ""
    }

    @Composable
    @ReadOnlyComposable
    open fun error(error: AuthException): String {
        val context = LocalContext.current
        return cachedErrorMessages.getOrPut(error::class) {
            // Check if the customer application has defined a specific string for this Exception type. If not, return
            // the generic error message.
            val resourceName = error.toResourceName()
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            val message = if (resourceId != 0) context.getString(resourceId) else null
            message ?: stringResource(R.string.amplify_ui_authenticator_error_unknown)
        }
    }

    companion object {
        @Composable
        @ReadOnlyComposable
        fun label(config: FieldConfig) = LocalStringResolver.current.label(config = config)

        @Composable
        @ReadOnlyComposable
        fun hint(config: FieldConfig) = LocalStringResolver.current.hint(config = config)

        @Composable
        @ReadOnlyComposable
        fun error(config: FieldConfig, error: FieldError) =
            LocalStringResolver.current.error(config = config, error = error)

        @Composable
        @ReadOnlyComposable
        fun error(error: AuthException) = LocalStringResolver.current.error(error = error)
    }
}
