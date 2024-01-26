package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInContinueWithTotpSetupState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import kotlinx.coroutines.launch

@Composable
fun SignInContinueWithTotpSetup(
    state: SignInContinueWithTotpSetupState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInContinueWithTotpSetupState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_continue_totp_setup))
    },
    footerContent: @Composable (state: SignInContinueWithTotpSetupState) -> Unit = {
        SignInContinueWithTotpSetupFooter(state = it)
    }
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        StepHeader(text = stringResource(R.string.amplify_ui_authenticator_step1_title))
        StepContent(text = stringResource(R.string.amplify_ui_authenticator_step1_content))
        StepHeader(text = stringResource(R.string.amplify_ui_authenticator_step2_title))
        StepContent(text = stringResource(R.string.amplify_ui_authenticator_step2_content))
        QrCode(
            modifier = Modifier
                .height(150.dp)
                .width(150.dp)
                .align(alignment = Alignment.CenterHorizontally),
            uri = state.setupUri
        )
        OutlinedButton(
            modifier = Modifier
                .testTag(TestTags.CopyKeyButton)
                .padding(vertical = 16.dp)
                .align(alignment = Alignment.CenterHorizontally),
            onClick = { clipboard.setText(AnnotatedString(state.sharedSecret)) }
        ) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_copy_key))
        }
        StepHeader(text = stringResource(R.string.amplify_ui_authenticator_step3_title))
        StepContent(text = stringResource(R.string.amplify_ui_authenticator_step3_content))
        AuthenticatorForm(state = state.form)
        AuthenticatorButton(
            modifier = modifier.testTag(TestTags.SignInConfirmButton),
            onClick = { scope.launch { state.continueSignIn() } },
            loading = !state.form.enabled
        )
        footerContent(state)
    }
}

@Composable
fun SignInContinueWithTotpSetupFooter(
    state: SignInContinueWithTotpSetupState,
    modifier: Modifier = Modifier
) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)

@Composable
private fun StepHeader(text: String) = Text(style = MaterialTheme.typography.titleSmall, text = text)

@Composable
private fun StepContent(text: String) = Text(
    modifier = Modifier.padding(bottom = 16.dp),
    style = MaterialTheme.typography.bodyMedium,
    text = text
)
