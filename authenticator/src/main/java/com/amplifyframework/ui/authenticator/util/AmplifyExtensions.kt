package com.amplifyframework.ui.authenticator.util

import android.app.Activity
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.smithy.kotlin.runtime.ServiceException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignInOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.data.AuthenticationFlow
import com.amplifyframework.ui.authenticator.data.toAuthFactorType

internal fun AWSCognitoAuthSignInOptions.CognitoBuilder.preferredFirstFactor(
    authenticationFlow: AuthenticationFlow,
    override: AuthFactor?
) = apply {
    if (authenticationFlow is AuthenticationFlow.UserChoice) {
        val factor = override ?: authenticationFlow.preferredAuthFactor
        preferredFirstFactor(factor?.toAuthFactorType())
    }
}

internal fun AWSCognitoAuthSignInOptions.CognitoBuilder.authFlow(authFlow: AuthFlowType?) = apply {
    authFlow?.let { authFlowType(it) }
}

internal fun AWSCognitoAuthSignInOptions.CognitoBuilder.callingActivity(activity: Activity?) = apply {
    activity?.let { callingActivity(it) }
}

internal fun AWSCognitoAuthConfirmSignInOptions.CognitoBuilder.callingActivity(activity: Activity?) = apply {
    activity?.let { callingActivity(it) }
}

internal fun AuthException.isAuthFlowSessionExpiredError(): Boolean {
    val sdkException = getCauseOrNull<NotAuthorizedException>()
    if (sdkException == null) return false
    return sdkException.sdkErrorMetadata.errorType == ServiceException.ErrorType.Client &&
        sdkException.message.contains("session")
}

internal inline fun <reified T : Exception> AuthException.getCauseOrNull(): T? {
    var causedBy = this.cause
    while (causedBy != null && causedBy != this) {
        if (causedBy is T) return causedBy
        causedBy = causedBy.cause
    }
    return null
}
