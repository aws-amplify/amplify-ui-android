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

import com.amplifyframework.auth.AWSCognitoAuthMetadataType
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.hub.HubEventFilter
import com.amplifyframework.ui.authenticator.BuildConfig
import com.amplifyframework.ui.authenticator.auth.AmplifyAuthConfiguration
import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.auth.VerificationMechanism
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONException

/**
 * An abstraction of the Amplify.Auth API that allows us to use coroutines with no exceptions
 */
internal interface AuthProvider {
    suspend fun signIn(
        username: String,
        password: String
    ): AmplifyResult<AuthSignInResult>

    suspend fun confirmSignIn(
        challengeResponse: String
    ): AmplifyResult<AuthSignInResult>

    suspend fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions
    ): AmplifyResult<AuthSignUpResult>

    suspend fun confirmSignUp(
        username: String,
        code: String
    ): AmplifyResult<AuthSignUpResult>

    suspend fun resendSignUpCode(
        username: String
    ): AmplifyResult<AuthCodeDeliveryDetails>

    suspend fun resetPassword(
        username: String
    ): AmplifyResult<AuthResetPasswordResult>

    suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String
    ): AmplifyResult<Unit>

    suspend fun signOut(): AuthSignOutResult

    suspend fun fetchAuthSession(): AmplifyResult<AuthSession>

    suspend fun fetchUserAttributes(): AmplifyResult<List<AuthUserAttribute>>

    suspend fun confirmUserAttribute(
        key: AuthUserAttributeKey,
        confirmationCode: String
    ): AmplifyResult<Unit>

    suspend fun resendUserAttributeConfirmationCode(key: AuthUserAttributeKey): AmplifyResult<AuthCodeDeliveryDetails>

    suspend fun getCurrentUser(): AmplifyResult<AuthUser>

    fun authStatusEvents(): Flow<HubEvent<*>>

    suspend fun getConfiguration(): AmplifyAuthConfiguration?
}

/**
 * The [AuthProvider] implementation that calls through to [Amplify.Auth]
 */
internal class RealAuthProvider : AuthProvider {

    init {
        val cognitoPlugin = getCognitoPlugin()
        cognitoPlugin?.addToUserAgent(AWSCognitoAuthMetadataType.Authenticator, BuildConfig.VERSION_NAME)
    }

    override suspend fun signIn(username: String, password: String) = suspendCoroutine { continuation ->
        Amplify.Auth.signIn(
            username,
            password,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun confirmSignIn(challengeResponse: String) = suspendCoroutine { continuation ->
        Amplify.Auth.confirmSignIn(
            challengeResponse,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.signUp(
            username,
            password,
            options,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun confirmSignUp(
        username: String,
        code: String
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.confirmSignUp(
            username,
            code,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun resendSignUpCode(
        username: String
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.resendSignUpCode(
            username,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun resetPassword(
        username: String
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.resetPassword(
            username,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.confirmResetPassword(
            username,
            newPassword,
            confirmationCode,
            { continuation.resume(AmplifyResult.Success(Unit)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun signOut() = suspendCoroutine { continuation ->
        Amplify.Auth.signOut { continuation.resume(it) }
    }

    override suspend fun fetchAuthSession() = suspendCoroutine { continuation ->
        Amplify.Auth.fetchAuthSession(
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun fetchUserAttributes() = suspendCoroutine { continuation ->
        Amplify.Auth.fetchUserAttributes(
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun confirmUserAttribute(
        key: AuthUserAttributeKey,
        confirmationCode: String
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.confirmUserAttribute(
            key,
            confirmationCode,
            { continuation.resume(AmplifyResult.Success(Unit)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun resendUserAttributeConfirmationCode(
        key: AuthUserAttributeKey
    ) = suspendCoroutine { continuation ->
        Amplify.Auth.resendUserAttributeConfirmationCode(
            key,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun getCurrentUser() = suspendCoroutine { continuation ->
        Amplify.Auth.getCurrentUser(
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override fun authStatusEvents(): Flow<HubEvent<*>> = callbackFlow {
        val filter = HubEventFilter {
            it.name == AuthChannelEventName.SIGNED_OUT.name || it.name == AuthChannelEventName.SIGNED_IN.name
        }
        val token = Amplify.Hub.subscribe(HubChannel.AUTH, filter) { trySendBlocking(it) }
        awaitClose { Amplify.Hub.unsubscribe(token) }
    }

    override suspend fun getConfiguration(): AmplifyAuthConfiguration? {
        val authConfigJSON = getCognitoPlugin()?.getPluginConfiguration() ?: return null
        try {
            val innerJSON = authConfigJSON
                .getJSONObject("Auth")
                .getJSONObject("Default")
            val signUpAttributes = innerJSON.getJSONArray("signupAttributes")
            val usernameAttributes = innerJSON.getJSONArray("usernameAttributes")
            val passwordAttributes = innerJSON.getJSONObject("passwordProtectionSettings")

            val signInAttributeList = List(usernameAttributes.length()) {
                usernameAttributes.getString(it)
            }
            val containsEmail = signInAttributeList.contains("EMAIL")
            val containsPhoneNumber = signInAttributeList.contains("PHONE_NUMBER")
            val signInMethod = when {
                containsEmail -> SignInMethod.Email
                containsPhoneNumber -> SignInMethod.PhoneNumber
                else -> SignInMethod.Username
            }

            val signUpAttributeList = List(signUpAttributes.length()) {
                AuthUserAttributeKey.custom(signUpAttributes.getString(it).lowercase())
            }

            val passwordRequirementsJSON = passwordAttributes
                .getJSONArray("passwordPolicyCharacters")
            val passwordRequirements = List(passwordRequirementsJSON.length()) {
                passwordRequirementsJSON.getString(it)
            }
            val passwordCriteria = PasswordCriteria(
                length = passwordAttributes.getInt("passwordPolicyMinLength"),
                requiresNumber = passwordRequirements.contains("REQUIRES_NUMBERS"),
                requiresSpecial = passwordRequirements.contains("REQUIRES_SYMBOLS"),
                requiresLower = passwordRequirements.contains("REQUIRES_LOWER"),
                requiresUpper = passwordRequirements.contains("REQUIRES_UPPER")
            )

            val verificationMechanismsJson = innerJSON.getJSONArray("verificationMechanisms")
            val verificationMechanisms = List(verificationMechanismsJson.length()) {
                when (verificationMechanismsJson.getString(it)) {
                    "EMAIL" -> VerificationMechanism.Email
                    else -> VerificationMechanism.PhoneNumber
                }
            }.toSet()

            return AmplifyAuthConfiguration(
                signInMethod,
                signUpAttributeList,
                passwordCriteria,
                verificationMechanisms
            )
        } catch (e: JSONException) {
            return null
        }
    }

    private fun getCognitoPlugin(): AWSCognitoAuthPlugin? {
        return try {
            Amplify.Auth.getPlugin("awsCognitoAuthPlugin")
                as AWSCognitoAuthPlugin
        } catch (e: Throwable) {
            null
        }
    }
}

internal sealed interface AmplifyResult<out T : Any> {
    data class Success<out T : Any>(val data: T) : AmplifyResult<T>
    data class Error(val error: AuthException) : AmplifyResult<Nothing>
}
