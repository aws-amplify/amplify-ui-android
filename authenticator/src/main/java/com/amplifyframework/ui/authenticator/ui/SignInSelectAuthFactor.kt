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
import com.amplifyframework.ui.authenticator.enums.AuthFactor
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.enums.containsPassword
import com.amplifyframework.ui.authenticator.locals.LocalStringResolver
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
    footerContent: @Composable (SignInSelectAuthFactorState) -> Unit = { SignInSelectFactorFooter(it) }
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        headerContent(state)

        val usernameLabel = StringResolver.fieldName(state.signInMethod.toFieldKey())
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.username,
            onValueChange =  {},
            label = { Text(usernameLabel) },
            enabled = false
        )
        Spacer(modifier = Modifier.size(AuthenticatorUiConstants.spaceBetweenFields))
        AuthenticatorForm(
            state = state.form
        )

        if (state.availableAuthFactors.containsPassword()) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(state.getPasswordFactor()) } },
                loading = state.selectedFactor == state.getPasswordFactor(),
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_password),
                modifier = Modifier.testTag(TestTags.AuthFactorPassword)
            )

            if (state.availableAuthFactors.size > 1) {
                DividerWithText(
                    text = stringResource(R.string.amplify_ui_authenticator_or),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.availableAuthFactors.contains(AuthFactor.WebAuthn)) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(AuthFactor.WebAuthn) } },
                loading = state.selectedFactor == AuthFactor.WebAuthn,
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_passkey),
                modifier = Modifier.testTag(TestTags.AuthFactorPasskey)
            )
        }
        if (state.availableAuthFactors.contains(AuthFactor.EmailOtp)) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(AuthFactor.EmailOtp) } },
                loading = state.selectedFactor == AuthFactor.EmailOtp,
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_email),
                modifier = Modifier.testTag(TestTags.AuthFactorEmail)
            )
        }
        if (state.availableAuthFactors.contains(AuthFactor.SmsOtp)) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(AuthFactor.SmsOtp) } },
                loading = state.selectedFactor == AuthFactor.SmsOtp,
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_sms),
                modifier = Modifier.testTag(TestTags.AuthFactorSms)
            )
        }
        footerContent(state)
    }
}

@Composable
fun SignInSelectFactorFooter(state: SignInSelectAuthFactorState, modifier: Modifier = Modifier) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)
