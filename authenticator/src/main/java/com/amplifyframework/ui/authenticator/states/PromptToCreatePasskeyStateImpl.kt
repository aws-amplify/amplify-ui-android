package com.amplifyframework.ui.authenticator.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.amplifyframework.ui.authenticator.PromptToCreatePasskeyState
import com.amplifyframework.ui.authenticator.PromptToCreatePasskeyState.Action
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PromptToCreatePasskeyStateImpl(private val onSubmit: suspend () -> Unit, private val onSkip: suspend () -> Unit) :
    PromptToCreatePasskeyState,
    MutableActionState<Action> {
    private val mutex = Mutex()

    override val step = AuthenticatorStep.PasskeyCreationPrompt

    override var action: Action? by mutableStateOf(null)

    override suspend fun createPasskey() = withAction(Action.CreatePasskey()) {
        mutex.withLock {
            onSubmit()
        }
    }

    override suspend fun skip() = withAction(Action.Skip()) { onSkip() }
}
