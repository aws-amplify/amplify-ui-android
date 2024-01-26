package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.ui.authenticator.SignInContinueWithTotpSetupState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal class SignInContinueWithTotpSetupStateImpl(
    override val sharedSecret: String,
    override val setupUri: String,
    private val onSubmit: suspend (confirmationCode: String) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignInContinueWithTotpSetupState {

    init {
        form.addFields {
            confirmationCode()
        }
    }

    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun continueSignIn() = doSubmit {
        val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
        onSubmit(confirmationCode)
    }

    override val step = AuthenticatorStep.SignInContinueWithTotpSetup
}
