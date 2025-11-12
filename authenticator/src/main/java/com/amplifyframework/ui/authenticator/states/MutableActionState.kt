package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.ui.authenticator.AuthenticatorActionState

internal interface MutableActionState<T> : AuthenticatorActionState<T> {
    override var action: T?
}

internal inline fun <T> MutableActionState<T>.withAction(action: T, func: () -> Unit) {
    this.action = action
    func()
    this.action = null
}
