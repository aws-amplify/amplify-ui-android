/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.ui.authenticator.amplify.AmplifyAuthConfiguration
import com.amplifyframework.ui.authenticator.amplify.PasswordCriteria
import com.amplifyframework.ui.authenticator.amplify.SignInMethod
import com.amplifyframework.ui.authenticator.amplify.toFieldKey
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.FieldValidators
import com.amplifyframework.ui.authenticator.forms.FormBuilderImpl
import com.amplifyframework.ui.authenticator.forms.FormData
import com.amplifyframework.ui.authenticator.forms.FormStateImpl
import com.amplifyframework.ui.authenticator.forms.buildForm
import com.amplifyframework.ui.authenticator.forms.toState

internal class ScreenStateFactory(
    private val authConfiguration: AmplifyAuthConfiguration,
    private val signUpForm: FormData,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) {

    fun newSignedInState(
        user: AuthUser,
        onSignOut: suspend () -> AuthSignOutResult
    ): SignedInStateImpl {
        return SignedInStateImpl(user, onSignOut)
    }

    fun newSignInState(
        onSubmit: suspend (username: String, password: String) -> Unit
    ): SignInStateImpl {
        val form = buildForm {
            signInMethod()
            password()
        }.toState()

        form.onSubmit {
            val username = form.getSignInMethod()!!
            val password = form.getTrimmed(FieldKey.Password)!!
            onSubmit(username, password)
        }

        return SignInStateImpl(form, onMoveTo)
    }

    fun newSignInMfaState(
        codeDeliveryDetails: AuthCodeDeliveryDetails?,
        onSubmit: suspend (confirmationCode: String) -> Unit
    ): SignInConfirmMfaStateImpl {
        val form = buildForm {
            confirmationCode()
        }.toState()

        form.onSubmit {
            val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
            onSubmit(confirmationCode)
        }
        return SignInConfirmMfaStateImpl(codeDeliveryDetails, form, onMoveTo)
    }

    fun newSignInConfirmCustomState(
        codeDeliveryDetails: AuthCodeDeliveryDetails?,
        additionalInfo: Map<String, String>,
        onSubmit: suspend (confirmationCode: String) -> Unit
    ): SignInConfirmCustomStateImpl {
        val form = buildForm {
            confirmationCode()
        }.toState()

        form.onSubmit {
            val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
            onSubmit(confirmationCode)
        }
        return SignInConfirmCustomStateImpl(form, codeDeliveryDetails, additionalInfo, onMoveTo)
    }

    fun newSignInConfirmNewPasswordState(
        onSubmit: suspend (password: String) -> Unit
    ): SignInConfirmNewPasswordStateImpl {
        val form = buildForm {
            password(authConfiguration.passwordCriteria)
            confirmPassword()
        }.toState()

        form.onSubmit {
            val password = form.getTrimmed(FieldKey.Password)!!
            onSubmit(password)
        }
        return SignInConfirmNewPasswordStateImpl(form, onMoveTo)
    }

    fun newSignUpState(
        onSubmit: suspend (username: String, password: String, attributes: List<AuthUserAttribute>) -> Unit
    ): SignUpStateImpl {
        val form = buildForm {
            // First add all fields required by configuration in the standard order
            signInMethod()
            password(authConfiguration.passwordCriteria)
            confirmPassword()
            authConfiguration.signUpAttributes.forEach { attribute ->
                when (attribute) {
                    AuthUserAttributeKey.birthdate() -> birthdate(required = true)
                    AuthUserAttributeKey.email() -> email(required = true)
                    AuthUserAttributeKey.familyName() -> familyName(required = true)
                    AuthUserAttributeKey.givenName() -> givenName(required = true)
                    AuthUserAttributeKey.middleName() -> middleName(required = true)
                    AuthUserAttributeKey.nickname() -> nickname(required = true)
                    AuthUserAttributeKey.phoneNumber() -> phoneNumber(required = true)
                    AuthUserAttributeKey.preferredUsername() -> preferredUsername(required = true)
                    AuthUserAttributeKey.profile() -> profile(required = true)
                    AuthUserAttributeKey.website() -> website(required = true)
                    AuthUserAttributeKey.name() -> name(required = true)
                }
            }

            // Next, replace and reorder the form according to the customer-supplied configuration
            replaceAndReorderFields(signUpForm)

            // Lastly, re-mark any of the fields that are required to successfully sign up
            markRequiredFields(
                authConfiguration.signInMethod,
                authConfiguration.signUpAttributes
            )
        }.toState()

        form.onSubmit {
            val username = form.getSignInMethod()
            val password = form.getTrimmed(FieldKey.Password)
            val attributes = form.getUserAttributes()
            onSubmit(username!!, password!!, attributes)
        }
        return SignUpStateImpl(form, onMoveTo)
    }

    fun newSignUpConfirmState(
        deliveryDetails: AuthCodeDeliveryDetails?,
        onResendCode: suspend () -> Unit,
        onSubmit: suspend (confirmationCode: String) -> Unit
    ): SignUpConfirmStateImpl {
        val form = buildForm {
            confirmationCode()
        }.toState()

        form.onSubmit {
            val code = form.getTrimmed(FieldKey.ConfirmationCode)!!
            onSubmit(code)
        }
        return SignUpConfirmStateImpl(
            deliveryDetails,
            form,
            onResendCode,
            onMoveTo
        )
    }

    fun newResetPasswordState(
        onSubmit: suspend (username: String) -> Unit
    ): PasswordResetStateImpl {
        val form = buildForm {
            signInMethod()
        }.toState()

        form.onSubmit {
            onSubmit(form.getSignInMethod()!!)
        }
        return PasswordResetStateImpl(form, onMoveTo)
    }

    fun newResetPasswordConfirmState(
        deliveryDetails: AuthCodeDeliveryDetails?,
        onSubmit: suspend (newPassword: String, confirmationCode: String) -> Unit
    ): PasswordResetConfirmStateImpl {
        val form = buildForm {
            confirmationCode()
            password(authConfiguration.passwordCriteria)
            confirmPassword()
        }.toState()

        form.onSubmit {
            val newPassword = form.getTrimmed(FieldKey.Password)!!
            val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
            onSubmit(newPassword, confirmationCode)
        }
        return PasswordResetConfirmStateImpl(form, deliveryDetails, onMoveTo)
    }

    fun newVerifyUserState(
        attributes: List<AuthUserAttribute>,
        onSubmit: suspend (attribute: AuthUserAttributeKey) -> Unit,
        onSkip: () -> Unit
    ): VerifyUserStateImpl {
        val form = buildForm {
            verificationAttribute()
        }.toState()

        form.onSubmit {
            val keyString = form.getTrimmed(FieldKey.VerificationAttribute)!!
            val key = AuthUserAttributeKey.custom(keyString)
            onSubmit(key)
        }

        return VerifyUserStateImpl(
            attributes = attributes,
            form = form,
            onSkip = onSkip
        )
    }

    fun newVerifyUserConfirmState(
        deliveryDetails: AuthCodeDeliveryDetails?,
        onSubmit: suspend (confirmationCode: String) -> Unit,
        onResendCode: suspend () -> Unit,
        onSkip: () -> Unit
    ): VerifyUserConfirmStateImpl {
        val form = buildForm {
            confirmationCode()
        }.toState()
        form.onSubmit {
            val confirmationCode = form.getTrimmed(FieldKey.ConfirmationCode)!!
            onSubmit(confirmationCode)
        }
        return VerifyUserConfirmStateImpl(
            form = form,
            deliveryDetails = deliveryDetails,
            onResendCode = onResendCode,
            onSkip = onSkip
        )
    }

    private fun FormBuilderImpl.signInMethod() =
        when (authConfiguration.signInMethod) {
            SignInMethod.Username -> username()
            SignInMethod.Email -> email(required = true)
            SignInMethod.PhoneNumber -> phoneNumber(required = true)
        }

    private fun FormStateImpl.getSignInMethod() = getTrimmed(authConfiguration.signInMethod.toFieldKey())

    private fun FormBuilderImpl.password(criteria: PasswordCriteria) {
        password(validator = FieldValidators.password(criteria))
    }

    private fun FormStateImpl.getTrimmed(key: FieldKey) = getContent(key)?.trim()
}
