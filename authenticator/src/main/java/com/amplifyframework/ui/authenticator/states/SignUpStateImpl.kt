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

package com.amplifyframework.ui.authenticator.states

import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.ui.authenticator.SignUpState
import com.amplifyframework.ui.authenticator.auth.PasswordCriteria
import com.amplifyframework.ui.authenticator.auth.SignInMethod
import com.amplifyframework.ui.authenticator.auth.toFieldKey
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.forms.FieldValidators
import com.amplifyframework.ui.authenticator.forms.FormData
import com.amplifyframework.ui.authenticator.forms.buildForm

internal class SignUpStateImpl(
    private val signInMethod: SignInMethod,
    private val signUpAttributes: List<AuthUserAttributeKey>,
    private val passwordCriteria: PasswordCriteria,
    private val signUpForm: FormData,
    private val onSubmit: suspend (username: String, password: String, attributes: List<AuthUserAttribute>) -> Unit,
    private val onMoveTo: (step: AuthenticatorInitialStep) -> Unit
) : BaseStateImpl(), SignUpState {

    init {
        val formData = buildForm {
            // First add all fields required by configuration in the standard order
            fieldForSignInMethod(signInMethod)
            password(validator = FieldValidators.password(passwordCriteria))
            confirmPassword()
            signUpAttributes.forEach { attribute ->
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
                signInMethod,
                signUpAttributes
            )
        }
        formData.fields.forEach { form.add(it) }
    }

    override val step = AuthenticatorStep.SignUp

    override fun moveTo(step: AuthenticatorInitialStep) = onMoveTo(step)

    override suspend fun signUp() = doSubmit {
        val username = form.getTrimmed(signInMethod.toFieldKey())!!
        val password = form.getTrimmed(FieldKey.Password)!!
        val attributes = form.getUserAttributes()
        onSubmit(username, password, attributes)
    }
}
