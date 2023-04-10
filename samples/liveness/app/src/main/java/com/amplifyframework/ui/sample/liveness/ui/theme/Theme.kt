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

package com.amplifyframework.ui.sample.liveness.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import com.amplifyframework.ui.liveness.ui.LivenessColorScheme

private val DarkColorScheme = LivenessColorScheme.Defaults.darkColorScheme.copy(
    surfaceVariant = Color(0xFF5C6670),
    onSurfaceVariant = Color.White,
    error = Color(0xFFF5BCBC),
    errorContainer = Color(0xFFF5BCBC),
    onErrorContainer = Color(0xFF660000)
)

private val LightColorScheme = LivenessColorScheme.Defaults.lightColorScheme.copy(
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFF0D1926),
    error = Color(0xFF950404),
    errorContainer = Color(0xFFF5BCBC),
    onErrorContainer = Color(0xFF660000)
)

val ColorScheme.successContainer: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) Color(0xFFD6F5DB) else Color(0xFF365E3D)

val ColorScheme.onSuccessContainer: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) Color(0xFF365E3D) else Color(0xFFD6F5DB)



@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}