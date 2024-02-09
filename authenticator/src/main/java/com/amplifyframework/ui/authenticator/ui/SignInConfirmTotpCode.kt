package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInConfirmTotpCodeState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.launch

@Composable
fun SignInConfirmTotpCode(
    state: SignInConfirmTotpCodeState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInConfirmTotpCodeState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_confirm_totp))
    },
    footerContent: @Composable (state: SignInConfirmTotpCodeState) -> Unit = {
        SignInConfirmTotpCodeFooter(state = it)
    }
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(R.string.amplify_ui_authenticator_enter_totp_code)
        )
        AuthenticatorForm(
            state = state.form
        )
        AuthenticatorButton(
            modifier = modifier.testTag(TestTags.SignInConfirmButton),
            onClick = { scope.launch { state.confirmSignIn() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
fun SignInConfirmTotpCodeFooter(
    state: SignInConfirmTotpCodeState,
    modifier: Modifier = Modifier
) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)
