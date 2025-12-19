package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.auth.result.AuthWebAuthnCredential
import com.amplifyframework.ui.authenticator.PasskeyCreatedState
import com.amplifyframework.ui.authenticator.PasskeyCreatedState.Action
import com.amplifyframework.ui.authenticator.R
import kotlinx.coroutines.launch

@Composable
fun PasskeyCreated(
    state: PasskeyCreatedState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (PasskeyCreatedState) -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(R.drawable.authenticator_success),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_passkey_created))
        }
    },
    footerContent: @Composable (PasskeyCreatedState) -> Unit = { }
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)

        if (state.passkeys.isNotEmpty()) {
            Text(
                stringResource(R.string.amplify_ui_authenticator_existing_passkeys),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.size(8.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    state.passkeys.forEachIndexed { index, passkey ->
                        Passkey(passkey)
                        if (index != state.passkeys.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }

        AuthenticatorButton(
            modifier = Modifier.testTag(TestTags.ContinueButton),
            onClick = { scope.launch { state.continueSignIn() } },
            loading = state.action is Action.ContinueSignIn,
            label = stringResource(R.string.amplify_ui_authenticator_button_continue)
        )

        footerContent(state)
    }
}

@Composable
private fun Passkey(credential: AuthWebAuthnCredential) {
    Text(credential.friendlyName ?: stringResource(R.string.amplify_ui_authenticator_unknown_passkey))
}
