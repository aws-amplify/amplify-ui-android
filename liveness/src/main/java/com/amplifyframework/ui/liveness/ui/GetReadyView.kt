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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.liveness.R

@Composable
internal fun GetReadyView(readyButtonOnClick: () -> Unit) {
    val showPhotosensitivityAlert = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.amplify_ui_liveness_get_ready_page_title),
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(R.string.amplify_ui_liveness_get_ready_page_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    onClick = { showPhotosensitivityAlert.value = true }
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.amplify_ui_liveness_get_ready_steps_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            BoxWithConstraints {
                if (maxWidth < 316.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FaceOvalInstructions()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start)
                    ) {
                        FaceOvalInstructions()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Step(1, stringResource(R.string.amplify_ui_liveness_get_ready_step_1))
            Step(2, stringResource(R.string.amplify_ui_liveness_get_ready_step_2))
            Step(3, stringResource(R.string.amplify_ui_liveness_get_ready_step_3))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = readyButtonOnClick
        ) {
            Text(stringResource(R.string.amplify_ui_liveness_get_ready_begin_check))
        }
    }

    if (showPhotosensitivityAlert.value) {
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
            onDismissRequest = { showPhotosensitivityAlert.value = false },
            confirmButton = {
                TextButton(
                    onClick = { showPhotosensitivityAlert.value = false }
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
}

@Composable
private fun FaceOvalInstructions() {
    val successColor = Color(0xFF365E3D)
    val successBackgroundTextColor = Color(0xFFD6F5DB)
    val errorColor = Color(0xFF660000)
    val errorBackgroundTextColor = Color(0xFFF5BCBC)
    FaceInstructionBox(
        imageResource = R.drawable.amplify_ui_liveness_face_oval_good_fit,
        icon = Icons.Filled.Check,
        iconContentDescriptionResource =
        R.string.amplify_ui_liveness_get_ready_a11y_good_face_fit_icon_content_description,
        color = successColor,
        textResource = R.string.amplify_ui_liveness_get_ready_good_fit,
        textBackgroundColor = successBackgroundTextColor
    )
    FaceInstructionBox(
        imageResource = R.drawable.amplify_ui_liveness_face_oval_too_far,
        icon = Icons.Filled.Close,
        iconContentDescriptionResource =
        R.string.amplify_ui_liveness_get_ready_a11y_wrong_face_fit_icon_content_description,
        color = errorColor,
        textResource = R.string.amplify_ui_liveness_get_ready_too_far,
        textBackgroundColor = errorBackgroundTextColor
    )
}

@Composable
private fun FaceInstructionBox(
    imageResource: Int,
    icon: ImageVector,
    iconContentDescriptionResource: Int,
    color: Color,
    textResource: Int,
    textBackgroundColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, color)
                .size(150.dp)
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(id = imageResource),
                contentDescription =
                stringResource(
                    R.string.amplify_ui_liveness_get_ready_a11y_illustration_content_description
                ),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
            )
            Icon(
                imageVector = icon,
                contentDescription = stringResource(iconContentDescriptionResource),
                tint = Color.White,
                modifier = Modifier.background(color)
            )
        }
        Text(
            text = stringResource(textResource),
            modifier = Modifier
                .background(textBackgroundColor)
                .width(150.dp)
                .padding(4.dp)
                .fillMaxHeight(),
            color = color
        )
    }
}

private class Oval : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            addOval(
                Rect(
                    left = size.width * 0.19f,
                    top = 0f,
                    right = size.width * 0.81f,
                    bottom = size.height
                )
            )
        }
        return Outline.Generic(path = path)
    }
}

@Composable
private fun Step(stepNumber: Int, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "$stepNumber.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun GetReadyViewPreview() {
    LivenessPreviewContainer {
        GetReadyView {}
    }
}
