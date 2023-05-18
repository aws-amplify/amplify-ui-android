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

import android.widget.DatePicker
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.strings.StringResolver
import java.time.Duration
import java.time.LocalDate

private val invalidDateChars = """[^\d-]+""".toRegex()

private object DateVisualTransformation : VisualTransformation {
    fun transform(original: String): String {
        val trimmed: String = original.take(8)
        if (trimmed.length < 4) return trimmed
        if (trimmed.length == 4) return "$trimmed-"
        val (year, monthAndOrDate) = trimmed.chunked(4)
        if (trimmed.length == 5) return "$year-$monthAndOrDate"
        if (trimmed.length == 6) return "$year-$monthAndOrDate-"
        val (month, date) = monthAndOrDate.chunked(2)
        return "$year-$month-$date"
    }

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(transform(text.text)),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 3) return offset
                    if (offset <= 5) return offset + 1
                    if (offset <= 7) return offset + 2
                    return 10
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 4) return offset
                    if (offset <= 7) return offset - 1
                    if (offset <= 10) return offset - 2
                    return 8
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateInputField(
    fieldConfig: FieldConfig.Date,
    fieldState: MutableFieldState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val label = StringResolver.label(fieldConfig)
    val hint = StringResolver.hint(fieldConfig)

    var dialogVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        value = fieldState.content.replace("-", ""),
        onValueChange = { fieldState.content = filterDate(it) },
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        visualTransformation = DateVisualTransformation,
        singleLine = true,
        isError = fieldState.error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) }
        ),
        trailingIcon = {
            IconButton(onClick = { dialogVisible = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_authenticator_calendar),
                    contentDescription = stringResource(R.string.amplify_ui_authenticator_field_a11y_date_picker)
                )
            }
        },
        supportingText = {
            AuthenticatorFieldError(
                modifier = Modifier.fillMaxWidth(),
                fieldConfig = fieldConfig,
                error = fieldState.error
            )
        }
    )

    if (dialogVisible) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { dialogVisible = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            // Convert the UTC milliseconds into an ISO 8601 date string
                            fieldState.content = LocalDate.ofEpochDay(Duration.ofMillis(it).toDays()).toString()
                        }
                        dialogVisible = false
                    }
                ) {
                    Text(stringResource(R.string.amplify_ui_authenticator_button_submit))
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

private fun filterDate(value: String): String {
    val stripped = value.replace(invalidDateChars, "").take(8)
    val year = stripped.take(4)
    val month = stripped.drop(4).take(2)
    val day = stripped.drop(6)
    return "$year-$month-$day"
}
