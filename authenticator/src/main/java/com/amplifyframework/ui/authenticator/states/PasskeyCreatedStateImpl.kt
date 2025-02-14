package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.auth.result.AuthWebAuthnCredential
import com.amplifyframework.ui.authenticator.PasskeyCreatedState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep

internal class PasskeyCreatedStateImpl(
    override val passkeys: List<AuthWebAuthnCredential>,
    private val onDone: suspend () -> Unit
) : PasskeyCreatedState {
    override val step: AuthenticatorStep = AuthenticatorStep.PasskeyCreated

    override suspend fun done() = onDone()
}