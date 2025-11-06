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

import android.app.Activity
import aws.sdk.kotlin.services.cognitoidentityprovider.getUserAuthFactors
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFactorType as KotlinAuthFactorType
import com.amplifyframework.auth.AWSCognitoAuthMetadataType
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.PasswordProtectionSettings
import com.amplifyframework.auth.cognito.UsernameAttribute
import com.amplifyframework.auth.cognito.VerificationMechanism as AmplifyVerificationMechanism
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthWebAuthnCredential
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

/**
 * An abstraction of the Amplify.Auth API that allows us to use coroutines with no exceptions
 */
internal interface AuthProvider {
    suspend fun signIn(username: String, password: String?, options: AuthSignInOptions): AmplifyResult<AuthSignInResult>

    suspend fun confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions
    ): AmplifyResult<AuthSignInResult>

    suspend fun signUp(username: String, password: String?, options: AuthSignUpOptions): AmplifyResult<AuthSignUpResult>

    suspend fun confirmSignUp(username: String, code: String): AmplifyResult<AuthSignUpResult>

    suspend fun resendSignUpCode(username: String): AmplifyResult<AuthCodeDeliveryDetails>

    suspend fun autoSignIn(): AmplifyResult<AuthSignInResult>

    suspend fun resetPassword(username: String): AmplifyResult<AuthResetPasswordResult>

    suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String
    ): AmplifyResult<Unit>

    suspend fun signOut(): AuthSignOutResult

    suspend fun fetchAuthSession(): AmplifyResult<AuthSession>

    suspend fun createPasskey(activity: Activity): AmplifyResult<Unit>

    suspend fun getPasskeys(): AmplifyResult<List<AuthWebAuthnCredential>>

    suspend fun fetchUserAttributes(): AmplifyResult<List<AuthUserAttribute>>

    suspend fun confirmUserAttribute(key: AuthUserAttributeKey, confirmationCode: String): AmplifyResult<Unit>

    suspend fun resendUserAttributeConfirmationCode(key: AuthUserAttributeKey): AmplifyResult<AuthCodeDeliveryDetails>

    suspend fun getCurrentUser(): AmplifyResult<AuthUser>

    suspend fun getAvailableAuthFactors(): AmplifyResult<List<AuthFactorType>>

    fun authStatusEvents(): Flow<HubEvent<*>>

    suspend fun getConfiguration(): AuthConfigurationResult
}

internal sealed interface AuthConfigurationResult {
    data class Valid(val configuration: AmplifyAuthConfiguration) : AuthConfigurationResult

    data class Invalid(val message: String, val cause: Exception? = null) : AuthConfigurationResult

    object Missing : AuthConfigurationResult
}

/**
 * The [AuthProvider] implementation that calls through to [Amplify.Auth]
 */
internal class RealAuthProvider : AuthProvider {
    init {
        val cognitoPlugin = getCognitoPlugin()
        cognitoPlugin?.addToUserAgent(AWSCognitoAuthMetadataType.Authenticator, BuildConfig.VERSION_NAME)
    }

    override suspend fun signIn(username: String, password: String?, options: AuthSignInOptions) =
        suspendCoroutine { continuation ->
            Amplify.Auth.signIn(
                username,
                password,
                options,
                { continuation.resume(AmplifyResult.Success(it)) },
                { continuation.resume(AmplifyResult.Error(it)) }
            )
        }

    override suspend fun confirmSignIn(challengeResponse: String, options: AuthConfirmSignInOptions) =
        suspendCoroutine { continuation ->
            Amplify.Auth.confirmSignIn(
                challengeResponse,
                options,
                { continuation.resume(AmplifyResult.Success(it)) },
                { continuation.resume(AmplifyResult.Error(it)) }
            )
        }

    override suspend fun signUp(username: String, password: String?, options: AuthSignUpOptions) =
        suspendCoroutine { continuation ->
            Amplify.Auth.signUp(
                username,
                password,
                options,
                { continuation.resume(AmplifyResult.Success(it)) },
                { continuation.resume(AmplifyResult.Error(it)) }
            )
        }

    override suspend fun confirmSignUp(username: String, code: String) = suspendCoroutine { continuation ->
        Amplify.Auth.confirmSignUp(
            username,
            code,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun resendSignUpCode(username: String) = suspendCoroutine { continuation ->
        Amplify.Auth.resendSignUpCode(
            username,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun autoSignIn() = suspendCoroutine { continuation ->
        Amplify.Auth.autoSignIn(
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun resetPassword(username: String) = suspendCoroutine { continuation ->
        Amplify.Auth.resetPassword(
            username,
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun confirmResetPassword(username: String, newPassword: String, confirmationCode: String) =
        suspendCoroutine { continuation ->
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

    override suspend fun createPasskey(activity: Activity) = suspendCoroutine { continuation ->
        Amplify.Auth.associateWebAuthnCredential(
            activity,
            { continuation.resume(AmplifyResult.Success(Unit)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun getPasskeys(): AmplifyResult<List<AuthWebAuthnCredential>> = suspendCoroutine { continuation ->
        Amplify.Auth.listWebAuthnCredentials(
            { continuation.resume(AmplifyResult.Success(it.credentials)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun fetchUserAttributes() = suspendCoroutine { continuation ->
        Amplify.Auth.fetchUserAttributes(
            { continuation.resume(AmplifyResult.Success(it)) },
            { continuation.resume(AmplifyResult.Error(it)) }
        )
    }

    override suspend fun confirmUserAttribute(key: AuthUserAttributeKey, confirmationCode: String) =
        suspendCoroutine { continuation ->
            Amplify.Auth.confirmUserAttribute(
                key,
                confirmationCode,
                { continuation.resume(AmplifyResult.Success(Unit)) },
                { continuation.resume(AmplifyResult.Error(it)) }
            )
        }

    override suspend fun resendUserAttributeConfirmationCode(key: AuthUserAttributeKey) =
        suspendCoroutine { continuation ->
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

    override suspend fun getAvailableAuthFactors(): AmplifyResult<List<AuthFactorType>> {
        // Get the identity provider client from Amplify
        val client = getCognitoPlugin()?.escapeHatch?.cognitoIdentityProviderClient ?: return AmplifyResult.Error(
            InvalidUserPoolConfigurationException()
        )

        // Get the user's access token
        val token = when (val authSession = fetchAuthSession()) {
            is AmplifyResult.Error -> return authSession
            is AmplifyResult.Success -> {
                val cognitoSession = authSession.data as AWSCognitoAuthSession
                cognitoSession.accessToken
            }
        }

        // Fetch the list of auth factors
        val response = try {
            client.getUserAuthFactors { accessToken = token }
        } catch (e: Exception) {
            return AmplifyResult.Error(UnknownException("Could not fetch auth factors", e))
        }

        // Map the factors to Amplify types
        val factors = response.configuredUserAuthFactors?.mapNotNull { factor ->
            when (factor) {
                KotlinAuthFactorType.EmailOtp -> AuthFactorType.EMAIL_OTP
                KotlinAuthFactorType.Password -> AuthFactorType.PASSWORD
                KotlinAuthFactorType.SmsOtp -> AuthFactorType.SMS_OTP
                KotlinAuthFactorType.WebAuthn -> AuthFactorType.WEB_AUTHN
                else -> null
            }
        } ?: emptyList()

        return AmplifyResult.Success(factors)
    }

    override fun authStatusEvents(): Flow<HubEvent<*>> = callbackFlow {
        val filter = HubEventFilter {
            it.name == AuthChannelEventName.SIGNED_OUT.name || it.name == AuthChannelEventName.SIGNED_IN.name
        }
        val token = Amplify.Hub.subscribe(HubChannel.AUTH, filter) { trySendBlocking(it) }
        awaitClose { Amplify.Hub.unsubscribe(token) }
    }

    override suspend fun getConfiguration(): AuthConfigurationResult {
        val authConfiguration = getCognitoPlugin()?.getAuthConfiguration() ?: return AuthConfigurationResult.Missing

        val passwordCriteria = authConfiguration.passwordProtectionSettings?.toPasswordCriteria()
            ?: return AuthConfigurationResult.Invalid(
                """
                    Your auth configuration does not define passwordProtectionSettings. 
                    Authenticator needs these settings to perform client-side validation of passwords.
                """.trimIndent()
            )

        val verificationMechanisms = authConfiguration.verificationMechanisms.map {
            when (it) {
                AmplifyVerificationMechanism.Email -> VerificationMechanism.Email
                AmplifyVerificationMechanism.PhoneNumber -> VerificationMechanism.PhoneNumber
            }
        }.toSet()

        val amplifyAuthConfiguration = AmplifyAuthConfiguration(
            signInMethod = getSignInMethod(authConfiguration.usernameAttributes),
            signUpAttributes = authConfiguration.signUpAttributes,
            passwordCriteria = passwordCriteria,
            verificationMechanisms = verificationMechanisms
        )

        return AuthConfigurationResult.Valid(amplifyAuthConfiguration)
    }

    private fun getCognitoPlugin(): AWSCognitoAuthPlugin? = try {
        Amplify.Auth.getPlugin("awsCognitoAuthPlugin") as AWSCognitoAuthPlugin
    } catch (e: Throwable) {
        null
    }

    private fun getSignInMethod(attributes: List<UsernameAttribute>) = when {
        attributes.contains(UsernameAttribute.Email) -> SignInMethod.Email
        attributes.contains(UsernameAttribute.PhoneNumber) -> SignInMethod.PhoneNumber
        else -> SignInMethod.Username
    }

    private fun PasswordProtectionSettings.toPasswordCriteria() = PasswordCriteria(
        length = length,
        requiresNumber = requiresNumber,
        requiresSpecial = requiresSpecial,
        requiresUpper = requiresUpper,
        requiresLower = requiresLower
    )
}
