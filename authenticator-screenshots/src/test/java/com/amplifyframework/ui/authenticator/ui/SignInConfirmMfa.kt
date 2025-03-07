/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.ui.authenticator.ScreenshotTestBase
import com.amplifyframework.ui.authenticator.SignInConfirmMfaState
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldConfig
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.mockFieldData
import com.amplifyframework.ui.authenticator.mockFieldState
import com.amplifyframework.ui.authenticator.mockForm
import org.junit.Test

class SignInConfirmMfa : ScreenshotTestBase() {

    @Test
    fun sign_in_confirm_email_mfa_default() {
        screenshot {
            SignInConfirmMfa(
                mockSignInConfirmMfaState(
                    AuthCodeDeliveryDetails(
                        "email@email.com",
                        AuthCodeDeliveryDetails.DeliveryMedium.EMAIL
                    )
                )
            )
        }
    }

    @Test
    fun sign_in_confirm_email_mfa_incorrect_code() {
        screenshot {
            SignInConfirmMfa(
                mockSignInConfirmMfaState(
                    AuthCodeDeliveryDetails(
                        "email@email.com",
                        AuthCodeDeliveryDetails.DeliveryMedium.EMAIL
                    ),
                    "123456",
                    FieldError.ConfirmationCodeIncorrect
                )
            )
        }
    }

    @Test
    fun sign_in_confirm_sms_mfa_default() {
        screenshot {
            SignInConfirmMfa(
                mockSignInConfirmMfaState(
                    AuthCodeDeliveryDetails(
                        "123-123-1234",
                        AuthCodeDeliveryDetails.DeliveryMedium.SMS
                    )
                )
            )
        }
    }

    @Test
    fun sign_in_confirm_sms_mfa_incorrect_code() {
        screenshot {
            SignInConfirmMfa(
                mockSignInConfirmMfaState(
                    AuthCodeDeliveryDetails(
                        "123-123-1234",
                        AuthCodeDeliveryDetails.DeliveryMedium.SMS
                    ),
                    "123456",
                    FieldError.ConfirmationCodeIncorrect
                )
            )
        }
    }

    private fun mockSignInConfirmMfaState(
        deliveryDetails: AuthCodeDeliveryDetails,
        confirmationCode: String = "",
        fieldError: FieldError? = null
    ) = object : SignInConfirmMfaState {
        override val form = mockForm(
            mockFieldData(
                config = FieldConfig.Text(FieldKey.ConfirmationCode),
                state = mockFieldState(content = confirmationCode, error = fieldError)
            )
        )
        override val deliveryDetails: AuthCodeDeliveryDetails
            get() = deliveryDetails

        override fun moveTo(step: AuthenticatorInitialStep) {}
        override suspend fun confirmSignIn() {
            TODO("Not yet implemented")
        }

        override val step = AuthenticatorStep.SignInContinueWithEmailSetup
    }
}
