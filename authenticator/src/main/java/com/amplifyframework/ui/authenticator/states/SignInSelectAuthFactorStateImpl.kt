package com.amplifyframework.ui.authenticator.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.amplifyframework.ui.authenticator.SignInSelectAuthFactorState
import com.amplifyframework.ui.authenticator.SignInSelectAuthFactorState.Action
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.data.containsPassword
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep

internal class SignInSelectAuthFactorStateImpl(
    override val username: String,
    val signInMethod: SignInMethod,
    override val availableAuthFactors: Set<AuthFactor>,
    private val onSubmit: suspend (authFactor: AuthFactor) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(),
    SignInSelectAuthFactorState,
    MutableActionState<Action> {
    override val step: AuthenticatorStep = AuthenticatorStep.SignInSelectAuthFactor

    override var action: Action? by mutableStateOf(null)

    init {
        if (availableAuthFactors.containsPassword()) {
            form.addFields { password() }
        }
    }

    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun select(authFactor: AuthFactor) = withAction(Action.SelectAuthFactor(authFactor)) {
        // Clear errors
        form.fields.values.forEach { it.state.error = null }

        form.enabled = false
        onSubmit(authFactor)
        form.enabled = true
    }
}

internal fun SignInSelectAuthFactorState.getPasswordFactor(): AuthFactor =
    availableAuthFactors.first { it is AuthFactor.Password }

internal val SignInSelectAuthFactorState.signInMethod: SignInMethod
    get() = (this as SignInSelectAuthFactorStateImpl).signInMethod
