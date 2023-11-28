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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.liveness.R

@Composable
internal fun PhotosensitivityAlert(onDismiss: () -> Unit) {
    AlertDialog(
        title = {
            Text(
                stringResource(
                    R.string.amplify_ui_liveness_get_ready_photosensitivity_dialog_title
                )
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.amplify_ui_liveness_get_ready_photosensitivity_dialog_description
                )
            )
        },
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(
                    stringResource(
                        R.string.amplify_ui_liveness_get_ready_photosensitivity_dialog_dismiss
                    )
                )
            }
        }
    )
}

@Composable
internal fun PhotosensitivityView(infoClicked: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(end = 6.dp)
                .weight(1f)
        ) {
            Text(
                text = stringResource(
                    R.string.amplify_ui_liveness_get_ready_photosensitivity_title
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(
                    R.string.amplify_ui_liveness_get_ready_photosensitivity_description
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        IconButton(
            onClick = { infoClicked()}
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(
                    /* ktlint-disable max-line-length */
                    R.string.amplify_ui_liveness_get_ready_a11y_photosensitivity_icon_content_description
                    /* ktlint-enable max-line-length */
                ),

                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun GetReadyViewPreview() {
    LivenessPreviewContainer {
        PhotosensitivityView {}
    }
}
