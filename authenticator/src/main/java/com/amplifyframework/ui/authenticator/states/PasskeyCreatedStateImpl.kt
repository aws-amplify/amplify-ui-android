package com.amplifyframework.ui.authenticator.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.amplifyframework.auth.result.AuthWebAuthnCredential
import com.amplifyframework.ui.authenticator.PasskeyCreatedState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep

internal class PasskeyCreatedStateImpl(
    override val passkeys: List<AuthWebAuthnCredential>,
    private val onDone: suspend () -> Unit
) : PasskeyCreatedState,
    MutableActionState<PasskeyCreatedState.Action> {
    override val step: AuthenticatorStep = AuthenticatorStep.PasskeyCreated

    override var action: PasskeyCreatedState.Action? by mutableStateOf(null)

    override suspend fun continueSignIn() = withAction(PasskeyCreatedState.Action.ContinueSignIn()) {
        onDone()
    }
}
