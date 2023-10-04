package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.amplifyframework.ui.authenticator.R

@Composable
internal fun BackToSignInFooter(
    onClickBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            modifier = Modifier.testTag(TestTags.BackToSignInButton),
            onClick = onClickBackToSignIn
        ) {
            Text(stringResource(R.string.amplify_ui_authenticator_button_back_to_signin))
        }
    }
}
