package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.PasskeyCreationPromptState
import com.amplifyframework.ui.authenticator.R
import kotlinx.coroutines.launch

private enum class Action {
    CreatingPasskey,
    Skipping
}

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

    var inProgress by remember { mutableStateOf<Action?>(null) }

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
                scope.launch {
                    inProgress = Action.CreatingPasskey
                    state.createPasskey()
                    inProgress = null
                }
            },
            loading = inProgress == Action.CreatingPasskey,
            label = stringResource(R.string.amplify_ui_authenticator_button_create_passkey),
            modifier = Modifier.testTag(TestTags.CreatePasskeyButton)
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.SkipPasskeyButton),
            onClick = { scope.launch {
                inProgress = Action.Skipping
                state.skip()
                inProgress = null
            } }
        ) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_skip_passkey))
        }

        footerContent(state)
    }
}
