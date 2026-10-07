/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.ui.sample.liveness.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.amplifyframework.ui.sample.liveness.LivenessChallengeOption
import com.amplifyframework.ui.sample.liveness.LivenessCodecOption

private const val PREFERENCES_NAME = "liveness_sample_prefs"
private const val KEY_CHALLENGE = "challenge_option"
private const val KEY_CODEC = "codec_option"

/**
 * A selection that is restored when the app launches and written back whenever it changes.
 */
class PersistedOption<T : Enum<T>> internal constructor(
    private val preferences: SharedPreferences,
    private val key: String,
    initial: T
) {
    var value: T by mutableStateOf(initial)
        private set

    fun select(option: T) {
        value = option
        preferences.edit().putString(key, option.name).apply()
    }
}

/** The challenge the next session is created with, restored from the last launch. */
@Composable
fun rememberChallengeOption(): PersistedOption<LivenessChallengeOption> = rememberPersistedOption(
    key = KEY_CHALLENGE,
    default = LivenessChallengeOption.Light,
    options = LivenessChallengeOption.entries
)

/** The codec the next check records with, restored from the last launch. */
@Composable
fun rememberCodecOption(): PersistedOption<LivenessCodecOption> = rememberPersistedOption(
    key = KEY_CODEC,
    default = LivenessCodecOption.VP8,
    options = LivenessCodecOption.entries
)

@Composable
private fun <T : Enum<T>> rememberPersistedOption(
    key: String,
    default: T,
    options: List<T>
): PersistedOption<T> {
    val context = LocalContext.current
    return remember(context, key) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val storedName = preferences.getString(key, null)
        PersistedOption(
            preferences = preferences,
            key = key,
            initial = options.firstOrNull { it.name == storedName } ?: default
        )
    }
}

/**
 * A row of mutually exclusive options, one segment per entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> OptionSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label(option))
            }
        }
    }
}
