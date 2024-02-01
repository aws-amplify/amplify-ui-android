package com.amplifyframework.ui.authenticator.util

import com.amplifyframework.auth.MFAType

// Amplify currently doesn't expose the strings we need to pass to confirmSignIn. This should be fixed in a future
// Amplify version
internal val MFAType.challengeResponse: String
    get() = when (this) {
        MFAType.SMS -> "SMS_MFA"
        MFAType.TOTP -> "SOFTWARE_TOKEN_MFA"
    }
