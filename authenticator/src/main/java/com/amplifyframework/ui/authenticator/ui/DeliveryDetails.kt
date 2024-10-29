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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.ui.authenticator.R
import com.amplifyframework.ui.authenticator.util.annotatedStringResource

/**
 * Displays a message outlining the information in an [AuthCodeDeliveryDetails].
 * @param details The [AuthCodeDeliveryDetails]. A generic message is shown if the details are null.
 * @param modifier The composable Modifier
 */
@Composable
internal fun DeliveryDetails(
    details: AuthCodeDeliveryDetails?,
    modifier: Modifier = Modifier
) {
    val content = when (details?.deliveryMedium) {
        AuthCodeDeliveryDetails.DeliveryMedium.EMAIL -> annotatedStringResource(
            R.string.amplify_ui_authenticator_confirmation_code_sent,
            details.destination
        )
        AuthCodeDeliveryDetails.DeliveryMedium.SMS -> annotatedStringResource(
            R.string.amplify_ui_authenticator_confirmation_code_sent,
            details.destination
        )
        else -> annotatedStringResource(R.string.amplify_ui_authenticator_confirmation_code_sent_unknown)
    }
    Text(modifier = modifier, text = content)
}
