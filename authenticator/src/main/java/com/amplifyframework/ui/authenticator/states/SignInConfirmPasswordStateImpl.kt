package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.ui.authenticator.SignInConfirmNewPasswordState
import com.amplifyframework.ui.authenticator.SignInConfirmPasswordState
import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.FieldValidators
import com.amplifyframework.ui.authenticator.forms.MutableFormState

internal class SignInConfirmPasswordStateImpl(
    override val username: String,
    val signInMethod: SignInMethod,
    private val onSubmit: suspend (password: String) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignInConfirmPasswordState {

    init {
        form.addFields {
            password()
        }
    }

    override val step: AuthenticatorStep = AuthenticatorStep.SignInConfirmPassword
    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun signIn() = doSubmit {
        val password = form.getTrimmed(FieldKey.Password)!!
        onSubmit(password)
    }
}

internal val SignInConfirmPasswordState.signInMethod: SignInMethod
    get() = (this as SignInConfirmPasswordStateImpl).signInMethod