/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.ui.authenticator.data

/**
 * Class that contains configuration values for when/if to show prompts to create passkeys to the user.
 */
data class PasskeyPrompts(
    /**
     * Show a prompt after a user who does not have a passkey registered signs in to the application.
     */
    val afterSignIn: PasskeyPrompt = PasskeyPrompt.Always,
    /**
     * Show a prompt to create a passkey after the automatic sign in following a new user signing up.
     */
    val afterSignUp: PasskeyPrompt = PasskeyPrompt.Always
)

/**
 * Possible selections for controlling passkey prompts.
 */
sealed interface PasskeyPrompt {
    /**
     * Never prompt users to create a passkey after signing in.
     */
    data object Never : PasskeyPrompt

    /**
     * Always prompt users to create a passkey after signing in if they don't already have an existing registered
     * passkey.
     */
    data object Always : PasskeyPrompt
}
