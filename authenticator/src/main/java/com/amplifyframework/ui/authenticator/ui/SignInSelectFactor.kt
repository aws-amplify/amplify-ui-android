package com.amplifyframework.ui.authenticator.ui

import android.widget.Space
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInSelectFactorState
import com.amplifyframework.ui.authenticator.enums.AuthFactor
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.enums.containsPassword
import com.amplifyframework.ui.authenticator.states.getPasswordFactor
import com.amplifyframework.ui.authenticator.util.AuthenticatorUiConstants
import kotlinx.coroutines.launch

@Composable
fun SignInSelectFactor(
    state: SignInSelectFactorState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (SignInSelectFactorState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_select_factor))
    },
    footerContent: @Composable (SignInSelectFactorState) -> Unit = { SignInSelectFactorFooter(it) }
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        headerContent(state)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.username,
            onValueChange =  {},
            label = { Text("Username") }, // todo proper labelling
            enabled = false
        )
        Spacer(modifier = Modifier.size(AuthenticatorUiConstants.spaceBetweenFields))
        AuthenticatorForm(
            state = state.form,
            bottomSpace = 0.dp
        )

        if (state.availableAuthFactors.containsPassword()) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(state.getPasswordFactor()) } },
                loading = state.selectedFactor == state.getPasswordFactor(),
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_password),
                modifier = Modifier.testTag("SignInPasswordButton")
            )

            if (state.availableAuthFactors.size > 1) {
                DividerWithText(
                    text = "or", // todo string resource
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
                modifier = Modifier.testTag("SignInPasskeyButton")
            )
        }
        if (state.availableAuthFactors.contains(AuthFactor.EmailOtp)) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(AuthFactor.EmailOtp) } },
                loading = state.selectedFactor == AuthFactor.EmailOtp,
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_email),
                modifier = Modifier.testTag("SignInEmailButton")
            )
        }
        if (state.availableAuthFactors.contains(AuthFactor.SmsOtp)) {
            AuthenticatorButton(
                onClick = { scope.launch { state.select(AuthFactor.SmsOtp) } },
                loading = state.selectedFactor == AuthFactor.SmsOtp,
                enabled = state.selectedFactor == null,
                label = stringResource(R.string.amplify_ui_authenticator_button_signin_sms),
                modifier = Modifier.testTag("SignInSmsButton")
            )
        }
        footerContent(state)
    }
}

@Composable
fun SignInSelectFactorFooter(state: SignInSelectFactorState, modifier: Modifier = Modifier) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)
