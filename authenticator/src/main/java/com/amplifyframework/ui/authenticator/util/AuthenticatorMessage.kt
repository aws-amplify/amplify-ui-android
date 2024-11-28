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

package com.amplifyframework.ui.authenticator.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.amplifyframework.auth.AuthException
import com.amplifyframework.ui.authenticator.R
import kotlin.reflect.KClass

/**
 * Messages that may be displayed in the Authenticator UI.
 */
interface AuthenticatorMessage {
    /**
     * Get the localized message content from a Composable function.
     */
    @get:Composable
    val message: String

    /**
     * Get the localized message content using a [Context] instance.
     */
    fun message(context: Context): String

    /**
     * Indicates an informational update for the user, such as the successful completion of an async process.
     */
    interface Info : AuthenticatorMessage

    /**
     * Indicates an error occurred. You may inspect the [cause] for more information.
     */
    interface Error : AuthenticatorMessage {
        /**
         * The [AuthException] that triggered this message.
         */
        val cause: AuthException
    }
}

internal abstract class AuthenticatorMessageImpl(protected val resource: Int) : AuthenticatorMessage {

    override val message: String
        @Composable
        get() = stringResource(resource)

    /**
     * Get the message content using a [Context] instance.
     */
    override fun message(context: Context) = context.getString(resource)
}

/**
 * The user's password has been successfully reset.
 */
internal object PasswordResetMessage :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_message_password_reset),
    AuthenticatorMessage.Info

/**
 * A confirmation code was sent to the user.
 */
internal object CodeSentMessage :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_message_code_sent),
    AuthenticatorMessage.Info

/**
 * The user cannot reset their password because their account is in an invalid state.
 */
internal class UnableToResetPasswordMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_cannot_reset_password),
    AuthenticatorMessage.Error

// Avoid recomputing the same error message multiple times
private val cachedErrorMessages = mutableMapOf<KClass<out AuthException>, String>()

/**
 * An unknown error occurred.
 */
internal class UnknownErrorMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_unknown),
    AuthenticatorMessage.Error {
    @SuppressLint("DiscouragedApi")
    override fun message(context: Context): String {
        return cachedErrorMessages.getOrPut(cause::class) {
            // Check if the customer application has defined a specific string for this Exception type. If not, return
            // the generic error message.
            val resourceName = cause.toResourceName()
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            val message = if (resourceId != 0) context.getString(resourceId) else super.message(context)
            return message
        }
    }
}

/**
 * The username or password were incorrect.
 */
internal class InvalidLoginMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_invalid_signin),
    AuthenticatorMessage.Error

/**
 * The server could not send a confirmation code to the user.
 */
internal class CannotSendCodeMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_send_code),
    AuthenticatorMessage.Error

/**
 * The entered confirmation code has expired.
 */
internal class ExpiredCodeMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_expired_code),
    AuthenticatorMessage.Error

/**
 * The device may not have connectivity.
 */
internal class NetworkErrorMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_network),
    AuthenticatorMessage.Error

/**
 * User tried an action too many times.
 */
internal class LimitExceededMessage(override val cause: AuthException) :
    AuthenticatorMessageImpl(R.string.amplify_ui_authenticator_error_limit_exceeded),
    AuthenticatorMessage.Error
