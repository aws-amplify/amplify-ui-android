/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.strings.StringResolver
import com.amplifyframework.ui.authenticator.util.dialCodeFor
import com.amplifyframework.ui.authenticator.util.dialCodeList
import java.util.Locale

@Stable
private class PhoneNumberFieldState(
    initialDialCode: String,
    initialNumber: String = ""
) {
    var dialCode by mutableStateOf(initialDialCode)
    var number by mutableStateOf(initialNumber)
    var expanded by mutableStateOf(false)
    val fieldValue by derivedStateOf {
        if (number.isEmpty()) "" else dialCode + number
    }
}

@Composable
internal fun PhoneInputField(
    fieldConfig: FieldConfig.PhoneNumber,
    fieldState: MutableFieldState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val label = StringResolver.label(fieldConfig)
    val hint = StringResolver.hint(fieldConfig)

    val state = rememberSaveable(
        saver = listSaver(
            save = { listOf(it.dialCode, it.number) },
            restore = { PhoneNumberFieldState(it[0], it[1]) }
        )
    ) {
        val countryCode = Locale.getDefault().country
        PhoneNumberFieldState(initialDialCode = dialCodeFor(countryCode))
    }

    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        value = state.number,
        onValueChange = { state.number = it },
        label = { Text(label) },
        leadingIcon = { DialCodeSelector(state) },
        placeholder = hint?.let { { Text(it) } },
        isError = fieldState.error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) }
        ),
        supportingText = {
            AuthenticatorFieldError(
                modifier = Modifier.fillMaxWidth(),
                fieldConfig = fieldConfig,
                error = fieldState.error
            )
        }
    )

    LaunchedEffect(state.fieldValue) {
        fieldState.content = state.fieldValue
    }
}

@Composable
private fun DialCodeSelector(
    state: PhoneNumberFieldState
) {
    Text(
        modifier = Modifier
            .clickable { state.expanded = true }
            .padding(8.dp),
        text = state.dialCode
    )

    if (state.expanded) {
        Dialog(
            onDismissRequest = { state.expanded = false }
        ) {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = dialCodeList.indexOf(state.dialCode)
            )
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .width(100.dp)
            ) {
                Surface {
                    LazyColumn(
                        state = listState
                    ) {
                        items(dialCodeList) { dialCode ->
                            val color = if (dialCode == state.dialCode) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val background = if (dialCode == state.dialCode) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            }
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(background)
                                    .clickable {
                                        state.dialCode = dialCode
                                        state.expanded = false
                                    }
                                    .padding(8.dp),
                                color = color,
                                text = dialCode
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
