package com.amplifyframework.ui.authenticator.data

import com.amplifyframework.ui.authenticator.enums.SignInSource

internal data class UserInfo(val username: String, val password: String?, val signInSource: SignInSource)
