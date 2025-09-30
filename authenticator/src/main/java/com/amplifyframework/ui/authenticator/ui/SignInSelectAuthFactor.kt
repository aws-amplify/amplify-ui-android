package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInSelectAuthFactorState
import com.amplifyframework.ui.authenticator.auth.toFieldKey
import com.amplifyframework.ui.authenticator.data.AuthFactor
import com.amplifyframework.ui.authenticator.data.containsPassword
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.states.getPasswordFactor
import com.amplifyframework.ui.authenticator.states.signInMethod
import com.amplifyframework.ui.authenticator.strings.StringResolver
import com.amplifyframework.ui.authenticator.util.AuthenticatorUiConstants
import kotlinx.coroutines.launch

@Composable
fun SignInSelectAuthFactor(
    state: SignInSelectAuthFactorState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (SignInSelectAuthFactorState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_select_factor))
    },
    footerContent: @Composable (SignInSelectAuthFactorState) -> Unit = { SignInSelectAuthFactorFooter(it) }
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        headerContent(state)

        val usernameLabel = StringResolver.fieldName(state.signInMethod.toFieldKey())
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(FieldKey.Username.testTag),
            value = state.username,
            onValueChange = {},
            label = { Text(usernameLabel) },
            enabled = false
        )
        Spacer(modifier = Modifier.size(AuthenticatorUiConstants.spaceBetweenFields))
        AuthenticatorForm(
            state = state.form
        )

        if (state.availableAuthFactors.containsPassword()) {
            AuthFactorButton(authFactor = state.getPasswordFactor(), state = state)
            if (state.availableAuthFactors.size > 1) {
                DividerWithText(
                    text = stringResource(R.string.amplify_ui_authenticator_or),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.availableAuthFactors.contains(AuthFactor.WebAuthn)) {
            AuthFactorButton(authFactor = AuthFactor.WebAuthn, state = state)
        }
        if (state.availableAuthFactors.contains(AuthFactor.EmailOtp)) {
            AuthFactorButton(authFactor = AuthFactor.EmailOtp, state = state)
        }
        if (state.availableAuthFactors.contains(AuthFactor.SmsOtp)) {
            AuthFactorButton(authFactor = AuthFactor.SmsOtp, state = state)
        }
        footerContent(state)
    }
}

@Composable
fun SignInSelectAuthFactorFooter(state: SignInSelectAuthFactorState, modifier: Modifier = Modifier) =
    BackToSignInFooter(
        modifier = modifier,
        onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
    )

@Composable
private fun AuthFactorButton(
    authFactor: AuthFactor,
    state: SignInSelectAuthFactorState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    AuthenticatorButton(
        onClick = { scope.launch { state.select(authFactor) } },
        loading = state.selectedFactor == authFactor,
        enabled = state.selectedFactor == null,
        label = stringResource(authFactor.signInResourceId),
        modifier = modifier.testTag(authFactor.testTag)
    )
}

private val AuthFactor.signInResourceId: Int
    get() = when (this) {
        is AuthFactor.Password -> R.string.amplify_ui_authenticator_button_signin_password
        AuthFactor.SmsOtp -> R.string.amplify_ui_authenticator_button_signin_sms
        AuthFactor.EmailOtp -> R.string.amplify_ui_authenticator_button_signin_email
        AuthFactor.WebAuthn -> R.string.amplify_ui_authenticator_button_signin_passkey
    }
