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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.MutableFieldState
import com.amplifyframework.ui.authenticator.strings.StringResolver
import com.amplifyframework.ui.authenticator.util.Region
import com.amplifyframework.ui.authenticator.util.regionList
import com.amplifyframework.ui.authenticator.util.regionMap
import java.util.Locale
import kotlinx.coroutines.launch

@Stable
private class PhoneNumberFieldState(
    initialRegionCode: String,
    initialNumber: String = ""
) {
    var region by mutableStateOf(regionMap[initialRegionCode] ?: regionMap["US"]!!)
    var number by mutableStateOf(initialNumber)
    var expanded by mutableStateOf(false)
    val fieldValue by derivedStateOf {
        if (number.isEmpty()) "" else region.dialCode + number
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
            save = { listOf(it.region.code, it.number) },
            restore = { PhoneNumberFieldState(it[0], it[1]) }
        )
    ) {
        val regionCode = Locale.getDefault().country
        PhoneNumberFieldState(initialRegionCode = regionCode)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialCodeSelector(
    state: PhoneNumberFieldState
) {
    Text(
        modifier = Modifier
            .clickable { state.expanded = true }
            .padding(8.dp),
        text = state.region.dialCode
    )

    if (state.expanded) {
        val scope = rememberCoroutineScope()
        val bottomSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { state.expanded = false }
        ) {
            val listState = rememberLazyListState()
            var filterTerm by remember { mutableStateOf("") }
            val displayedRegions by remember {
                derivedStateOf {
                    if (filterTerm.isBlank()) {
                        regionList
                    } else {
                        regionList.filter { it.name.contains(filterTerm, ignoreCase = true) }
                    }
                }
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RegionSearchBox(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).padding(horizontal = 16.dp),
                        value = filterTerm,
                        onValueChange = { filterTerm = it }
                    )
                    LazyColumn(state = listState) {
                        itemsIndexed(displayedRegions) { index, region ->
                            RegionItem(
                                showDivider = index < displayedRegions.lastIndex,
                                region = region,
                                onClick = {
                                    state.region = it
                                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                                        if (!bottomSheetState.isVisible) {
                                            state.expanded = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_authenticator_search),
                contentDescription = null
            )
        },
        label = {
            Text(stringResource(R.string.amplify_ui_authenticator_field_phone_search))
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_authenticator_clear),
                        contentDescription = stringResource(R.string.amplify_ui_authenticator_field_phone_search_clear)
                    )
                }
            }
        }
    )
}

@Composable
private fun LazyItemScope.RegionItem(
    showDivider: Boolean,
    region: Region,
    onClick: (Region) -> Unit
) {
    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .clickable { onClick(region) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${region.flagEmoji} ${region.name}",
            modifier = Modifier.semantics {
                contentDescription = region.name // Don't read the flag emoji when using talkback
            }
        )
        Text(region.dialCode)
    }

    if (showDivider) {
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
