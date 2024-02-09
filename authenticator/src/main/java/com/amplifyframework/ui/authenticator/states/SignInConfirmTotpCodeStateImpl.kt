package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.ui.authenticator.SignInConfirmTotpCodeState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey

internal class SignInConfirmTotpCodeStateImpl(
    private val onSubmit: suspend (confirmationCode: String) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignInConfirmTotpCodeState {
    init {
        form.addFields {
            confirmationCode()
        }
    }

    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun confirmSignIn() = doSubmit {
        val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
        onSubmit(confirmationCode)
    }

    override val step = AuthenticatorStep.SignInConfirmTotpCode
}
