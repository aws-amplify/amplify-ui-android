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

package com.amplifyframework.ui.liveness.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * LivenessColorScheme to color the FaceLivenessDetector
 */
object LivenessColorScheme {

    /**
     * @return default liveness light/dark theme depending on system theme mode.
     */
    @Composable
    @ReadOnlyComposable
    fun default() = default(isSystemInDarkTheme())

    private fun default(isSystemInDarkTheme: Boolean) =
        if (isSystemInDarkTheme) {
            Defaults.darkColorScheme
        } else {
            Defaults.lightColorScheme
        }

    /**
     * Default Liveness [ColorScheme] Values
     */
    object Defaults {

        /** Liveness Light [ColorScheme] overrides Material3 defaults as necessary */
        val lightColorScheme = lightColorScheme(
            primary = Color(0xFF047D95),
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color(0xFF0D1926),
            surface = Color.White,
            onSurface = Color(0xFF0D1926),
            error = Color(0xFF950404),
            onError = Color.White,
            errorContainer = Color(0xFFF5D9BC),
            onErrorContainer = Color(0xFF663300)
        )

        /** Liveness Dark [ColorScheme] overrides Material3 defaults as necessary */
        val darkColorScheme = darkColorScheme(
            primary = Color(0xFF7DD6E8),
            onPrimary = Color(0xFF0D1926),
            background = Color(0xFF0D1926),
            onBackground = Color.White,
            surface = Color(0xFF0D1926),
            onSurface = Color.White,
            error = Color(0xFFEF8F8F),
            onError = Color(0xFF0D1926),
            errorContainer = Color(0xFF663300),
            onErrorContainer = Color(0xFFEFBF8F),
        )
    }
}
