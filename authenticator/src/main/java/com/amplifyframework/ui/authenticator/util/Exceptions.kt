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

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.result.step.AuthSignInStep
import java.net.UnknownHostException

/**
 * Exception that is passed to the errorContent if the application does not have the auth plugin
 * configured when attempting to use Authenticator
 */
class MissingConfigurationException :
    AuthException(
        "Missing auth configuration",
        "Make sure the Auth plugin is added and Amplify.configure is called. See " +
            "https://docs.amplify.aws/lib/auth/getting-started/q/platform/android/ for details"
    )

/**
 * Exception that is passed to the errorContent if the configuration passed to the auth plugin is missing a required
 * property or has an invalid property
 */
class InvalidConfigurationException(message: String, cause: Exception?) :
    AuthException(
        message = message,
        recoverySuggestion = "Check that the configuration passed to Amplify.configure has all required fields",
        cause = cause
    )

/**
 * Exception that occurs if Amplify returns a "nextStep" that is not supported by Authenticator. This might happen
 * if Amplify has released a new feature and support is still outstanding in Authenticator.
 */
class UnsupportedNextStepException internal constructor(nextStep: AuthSignInStep) :
    AuthException(
        message = "Unsupported next step $nextStep.",
        recoverySuggestion =
        "Authenticator does not support this Authentication flow, disable it to use this version of " +
            "Authenticator, or check if support has been added in a new version."
    )

internal fun Throwable.isConnectivityIssue(): Boolean {
    if (this is UnknownHostException) {
        return true
    }
    return when (val cause = this.cause) {
        null -> false
        else -> cause.isConnectivityIssue()
    }
}

private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
private fun String.toSnakeCase() = camelRegex.replace(this) { "_${it.value}" }.lowercase()

internal fun AuthException.toResourceName() =
    "amplify_ui_authenticator_error_" + this::class.simpleName?.removeSuffix("Exception")?.toSnakeCase()
