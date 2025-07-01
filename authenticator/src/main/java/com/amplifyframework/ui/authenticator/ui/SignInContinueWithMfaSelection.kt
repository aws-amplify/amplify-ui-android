package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.challengeResponse
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.SignInContinueWithMfaSelectionState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import kotlinx.coroutines.launch

private val MFA_SELECTION_ORDER = setOf(MFAType.TOTP, MFAType.SMS, MFAType.EMAIL)

@Composable
fun SignInContinueWithMfaSelection(
    state: SignInContinueWithMfaSelectionState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInContinueWithMfaSelectionState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_continue_mfa_select))
    },
    footerContent: @Composable (state: SignInContinueWithMfaSelectionState) -> Unit = {
        SignInContinueWithMfaSelectionFooter(state = it)
    }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fieldState = state.form.fields[FieldKey.MfaSelection]?.state!!

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = stringResource(R.string.amplify_ui_authenticator_mfa_selection_description)
        )
        val items = remember { MFA_SELECTION_ORDER.intersect(state.allowedMfaTypes).map { it.challengeResponse } }
        RadioGroup(
            items = items,
            selected = fieldState.content,
            onSelect = { fieldState.content = it },
            label = {
                when (it) {
                    MFAType.SMS.challengeResponse -> context.getString(R.string.amplify_ui_authenticator_mfa_sms)
                    MFAType.EMAIL.challengeResponse -> context.getString(R.string.amplify_ui_authenticator_mfa_email)
                    else -> context.getString(R.string.amplify_ui_authenticator_mfa_totp)
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        AuthenticatorButton(
            modifier = modifier.testTag(TestTags.SignInConfirmButton),
            onClick = { scope.launch { state.continueSignIn() } },
            loading = !state.form.enabled,
            label = stringResource(R.string.amplify_ui_authenticator_button_continue)
        )
        footerContent(state)
    }
}

@Composable
fun SignInContinueWithMfaSelectionFooter(
    state: SignInContinueWithMfaSelectionState,
    modifier: Modifier = Modifier
) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)
