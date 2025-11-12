package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.PasskeyCreationPromptState
import com.amplifyframework.ui.authenticator.PasskeyCreationPromptState.Action
import com.amplifyframework.ui.authenticator.R
import kotlinx.coroutines.launch

@Composable
fun PasskeyPrompt(
    state: PasskeyCreationPromptState,
    modifier: Modifier = Modifier,
    headerContent: @Composable (PasskeyCreationPromptState) -> Unit = {
        AuthenticatorTitle(stringResource(R.string.amplify_ui_authenticator_title_prompt_for_passkey))
    },
    footerContent: @Composable (PasskeyCreationPromptState) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val action = state.action

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        headerContent(state)

        Text(stringResource(R.string.amplify_ui_authenticator_passkey_prompt_content))

        Spacer(modifier = Modifier.size(16.dp))

        Image(
            painter = painterResource(R.drawable.authenticator_passkey),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.size(16.dp))

        AuthenticatorButton(
            onClick = {
                scope.launch { state.createPasskey() }
            },
            loading = action is Action.CreatePasskey,
            enabled = action == null,
            label = stringResource(R.string.amplify_ui_authenticator_button_create_passkey),
            modifier = Modifier.testTag(TestTags.CreatePasskeyButton)
        )

        AuthenticatorButton(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.SkipPasskeyButton),
            onClick = {
                scope.launch { state.skip() }
            },
            loading = action is Action.Skip,
            enabled = action == null,
            label = stringResource(R.string.amplify_ui_authenticator_button_skip_passkey),
            style = ButtonStyle.Secondary
        )

        footerContent(state)
    }
}
