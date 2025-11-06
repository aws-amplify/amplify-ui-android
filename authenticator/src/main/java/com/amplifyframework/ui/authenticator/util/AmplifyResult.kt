package com.amplifyframework.ui.authenticator.util

import com.amplifyframework.auth.AuthException

internal sealed interface AmplifyResult<out T : Any> {
    data class Success<out T : Any>(val data: T) : AmplifyResult<T>
    data class Error(val error: AuthException) : AmplifyResult<Nothing>
}

internal inline fun <T : Any> AmplifyResult<T>.getOrDefault(crossinline provider: () -> T) = when (this) {
    is AmplifyResult.Error -> provider()
    is AmplifyResult.Success -> this.data
}
