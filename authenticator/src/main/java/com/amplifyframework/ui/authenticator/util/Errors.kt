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
 */ // ktlint-disable filename

package com.amplifyframework.ui.authenticator.util

import com.amplifyframework.auth.cognito.exceptions.service.InvalidPasswordException
import com.amplifyframework.ui.authenticator.forms.FieldError

// This SHOULD be caught by the field validator, but it's possible that the local configuration is
// different from the server configuration. In such cases this exception will be returned.
// We cannot extract the specific problem from the exception, so just show the message returned
// or an InvalidFormat if none
internal fun InvalidPasswordException.toFieldError(): FieldError {
    val cause = this.cause
    return if (
        cause is aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidPasswordException &&
        cause.localizedMessage != null
    ) {
        FieldError.Custom(cause.localizedMessage!!)
    } else {
        FieldError.InvalidFormat
    }
}
