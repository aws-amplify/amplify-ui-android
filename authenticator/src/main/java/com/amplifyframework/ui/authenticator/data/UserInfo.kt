package com.amplifyframework.ui.authenticator.data

import com.amplifyframework.ui.authenticator.enums.SignInSource

internal data class UserInfo(
    val username: String,
    val password: String?,
    val signInSource: SignInSource,
    val selectedAuthFactor: AuthFactor? = null
) {
    override fun toString() = "UserInfo(" +
        "username=$username, " +
        "password=***, " +
        "signInSource=$signInSource, " +
        "selectedAuthFactor=$selectedAuthFactor)"
}
