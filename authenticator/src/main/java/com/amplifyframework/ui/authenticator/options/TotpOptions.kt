package com.amplifyframework.ui.authenticator.options

/**
 * Options for configuring the
 * [TOTP MFA](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-settings-mfa-totp.html) experience.
 */
data class TotpOptions(
    /**
     * The 'issuer' is the title displayed in a user's authenticator application, preceding the account name.
     * In most cases this should be the name of your app. For example, if your app is called "My App", your user
     * will see "My App - Username" in their authenticator application.
     * Defaults to the name of the Android application if not supplied.
     */
    val issuer: String? = null
)
