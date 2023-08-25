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

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.amplifyframework.ui.liveness.util.findActivity

@SuppressLint("SourceLockedOrientationActivity")
@Composable
internal fun LockPortraitOrientation(content: @Composable (resetOrientation: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return content {}
    val originalOrientation by rememberSaveable { mutableStateOf(activity.requestedOrientation) }
    SideEffect {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // wait until screen is rotated correctly
    if (activity.resources.configuration.orientation == ORIENTATION_PORTRAIT) {
        content {
            activity.requestedOrientation = originalOrientation
        }
    }
}
