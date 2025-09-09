package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.amplifyframework.ui.authenticator.R

internal enum class ButtonStyle {
    Primary,
    Secondary
}

/**
 * The button displayed in Authenticator.
 * @param onClick The click handler for the button
 * @param loading True to show the [loadingIndicator] content, false to show the button label.
 * @param modifier The [Modifier] for the composable.
 * @param label The label for the button
 * @param loadingIndicator The content to show when loading.
 */
@Composable
internal fun AuthenticatorButton(
    onClick: () -> Unit,
    loading: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.amplify_ui_authenticator_button_submit),
    loadingIndicator: @Composable () -> Unit = { LoadingIndicator() },
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Primary
) {
    if (style == ButtonStyle.Primary) {
        Button(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            enabled = enabled && !loading
        ) {
            if (loading) {
                loadingIndicator()
            } else {
                Text(label)
            }
        }
    } else {
        OutlinedButton(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            enabled = enabled && !loading
        ) {
            if (loading) {
                loadingIndicator()
            } else {
                Text(label)
            }
        }
    }
}
