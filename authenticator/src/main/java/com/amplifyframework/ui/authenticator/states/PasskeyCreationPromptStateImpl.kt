package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.ui.authenticator.PasskeyCreationPromptState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PasskeyCreationPromptStateImpl(private val onSubmit: suspend () -> Unit, private val onSkip: suspend () -> Unit) :
    PasskeyCreationPromptState {
    private val mutex = Mutex()

    override suspend fun createPasskey() {
        mutex.withLock {
            onSubmit()
        }
    }

    override suspend fun skip() = onSkip()

    override val step = AuthenticatorStep.PasskeyCreationPrompt
}
