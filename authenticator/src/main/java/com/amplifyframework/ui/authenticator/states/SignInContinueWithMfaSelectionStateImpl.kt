package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.auth.MFAType
import com.amplifyframework.ui.authenticator.SignInContinueWithMfaSelectionState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.util.challengeResponse

internal class SignInContinueWithMfaSelectionStateImpl(
    override val allowedMfaTypes: Set<MFAType>,
    private val onSubmit: suspend (selection: String) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignInContinueWithMfaSelectionState {

    init {
        form.addFields {
            mfaSelection()
        }
    }

    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun continueSignIn() = doSubmit {
        val selected = form.fields[FieldKey.MfaSelection]!!.state.content
        onSubmit(selected)
    }

    override val step = AuthenticatorStep.SignInContinueWithMfaSelection
}
