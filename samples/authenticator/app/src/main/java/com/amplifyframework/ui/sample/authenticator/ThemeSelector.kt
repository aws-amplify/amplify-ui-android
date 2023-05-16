/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.sample.authenticator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.sample.authenticator.theme.amplifytheme.AmplifyTheme
import com.amplifyframework.ui.sample.authenticator.theme.default.AppTheme

enum class SupportedTheme {
    Default,
    Amplify
}

@Composable
fun ThemeSelector(
    modifier: Modifier = Modifier,
    currentTheme: SupportedTheme,
    darkMode: Boolean,
    onChangeCurrentTheme: (SupportedTheme) -> Unit,
    onChangeDarkMode: (Boolean) -> Unit
) {
    Column(modifier = modifier) {
        SupportedTheme.values().forEach { theme ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = theme == currentTheme, onClick = { onChangeCurrentTheme(theme) })
                Text(modifier = Modifier.padding(start = 8.dp), text = theme.name)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = darkMode, onCheckedChange = { onChangeDarkMode(!darkMode) })
            Text(modifier = Modifier.padding(start = 8.dp), text = "Dark Mode")
        }
    }
}

@Composable
fun ApplyTheme(theme: SupportedTheme, darkMode: Boolean, content: @Composable () -> Unit) {
    when (theme) {
        SupportedTheme.Default -> AppTheme(darkTheme = darkMode, content = content)
        SupportedTheme.Amplify -> AmplifyTheme(useDarkTheme = darkMode, content = content)
    }
}
