package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun <T : Any> RadioGroup(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        items.forEach { item ->
            RadioItem(
                modifier = Modifier.fillMaxWidth(),
                selected = selected == item,
                onSelect = onSelect,
                value = item,
                label = label(item),
                enabled = enabled
            )
        }
    }
}

@Composable
internal fun <T> RadioItem(
    selected: Boolean,
    onSelect: (T) -> Unit,
    value: T,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.clickable { onSelect(value) }.testTag(value.toString()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelect(value) },
            enabled = enabled
        )
        Text(label)
    }
}
