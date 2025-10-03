package com.amplifyframework.ui.authenticator.util

import android.app.Activity
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
