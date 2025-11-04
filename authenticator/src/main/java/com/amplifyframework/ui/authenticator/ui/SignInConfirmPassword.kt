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
import com.amplifyframework.ui.authenticator.SignInConfirmPasswordState
import com.amplifyframework.ui.authenticator.auth.toFieldKey
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.states.signInMethod
import com.amplifyframework.ui.authenticator.strings.StringResolver
import com.amplifyframework.ui.authenticator.util.AuthenticatorUiConstants
import kotlinx.coroutines.launch

@Composable
fun SignInConfirmPassword(
    state: SignInConfirmPasswordState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (state: SignInConfirmPasswordState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_signin_confirm_password))
    },
    footerContent: @Composable (state: SignInConfirmPasswordState) -> Unit = { SignInConfirmPasswordFooter(it) }
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
        AuthenticatorButton(
            onClick = { scope.launch { state.signIn() } },
            loading = !state.form.enabled,
            label = stringResource(R.string.amplify_ui_authenticator_button_signin),
            modifier = Modifier.testTag(TestTags.SignInButton)
        )
        footerContent(state)
    }
}

@Composable
fun SignInConfirmPasswordFooter(state: SignInConfirmPasswordState, modifier: Modifier = Modifier) = BackToSignInFooter(
    modifier = modifier,
    onClickBackToSignIn = { state.moveTo(AuthenticatorStep.SignIn) }
)
