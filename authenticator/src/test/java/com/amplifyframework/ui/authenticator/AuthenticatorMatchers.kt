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

import com.amplifyframework.ui.authenticator.util.AuthenticatorMessage
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf

fun haveMessage(expected: String) = Matcher<AuthenticatorMessage.Error> {
    MatcherResult(
        it.cause.message == expected,
        { "error has message of ${it.cause.message} but it should have message $expected" },
        { "error should not have message $expected" }
    )
}

fun haveRecoverySuggestion(expected: String) = Matcher<AuthenticatorMessage.Error> {
    MatcherResult(
        it.cause.recoverySuggestion == expected,
        { "error has message of ${it.cause.recoverySuggestion} but it should have message $expected" },
        { "error should not have message $expected" }
    )
}

fun AuthenticatorMessage?.shouldBeError(
    causeMessage: String? = null,
    recoverySuggestion: String? = null
) {
    val casted = this.shouldBeInstanceOf<AuthenticatorMessage.Error>()
    causeMessage?.let { casted should haveMessage(it) }
    recoverySuggestion?.let { casted should haveRecoverySuggestion(it) }
}
