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

package com.amplifyframework.ui.authenticator.forms

import com.amplifyframework.auth.AuthUserAttributeKey

/**
 * An [FieldKey] uniquely identifies a particular field in a form.
 */
abstract class FieldKey internal constructor() {

    /**
     * Key for field to enter a username.
     */
    object Username : FieldKey()

    /**
     * Key for field to enter a password.
     */
    object Password : FieldKey()

    /**
     * Key for field to re-enter a password.
     */
    object ConfirmPassword : FieldKey()

    /**
     * Key for field to enter a confirmation code.
     */
    object ConfirmationCode : FieldKey()

    /**
     * Key for field to select a verification attribute on the VerifyUser step.
     */
    object VerificationAttribute : FieldKey()

    /**
     * A [UserAttributeKey] is an [FieldKey] for a field that maps to an
     * [AuthUserAttributeKey] in Amplify.
     */
    abstract class UserAttributeKey internal constructor(val attributeKey: AuthUserAttributeKey) :
        FieldKey()

    /**
     * Key for field for setting the built-in email attribute.
     */
    object Email : UserAttributeKey(AuthUserAttributeKey.email())

    /**
     * Key for field for setting the built-in phone number attribute.
     */
    object PhoneNumber : UserAttributeKey(AuthUserAttributeKey.phoneNumber())

    /**
     * Key for field for setting the built-in birthdate attribute.
     */
    object Birthdate : UserAttributeKey(AuthUserAttributeKey.birthdate())

    /**
     * Key for field for setting the built-in family name attribute.
     */
    object FamilyName : UserAttributeKey(AuthUserAttributeKey.familyName())

    /**
     * Key for field for setting the built-in given name attribute.
     */
    object GivenName : UserAttributeKey(AuthUserAttributeKey.givenName())

    /**
     * Key for field for setting the built-in middle name attribute.
     */
    object MiddleName : UserAttributeKey(AuthUserAttributeKey.middleName())

    /**
     * Key for field for setting the built-in name attribute.
     */
    object Name : UserAttributeKey(AuthUserAttributeKey.name())

    /**
     * Key for field for setting the built-in nickname attribute.
     */
    object Nickname : UserAttributeKey(AuthUserAttributeKey.nickname())

    /**
     * Key for field for setting the built-in preferred username attribute.
     */
    object PreferredUsername : UserAttributeKey(AuthUserAttributeKey.preferredUsername())

    /**
     * Key for field for setting the built-in profile attribute.
     */
    object Profile : UserAttributeKey(AuthUserAttributeKey.profile())

    /**
     * Key for field for setting the built-in website attribute.
     */
    object Website : UserAttributeKey(AuthUserAttributeKey.website())

    /**
     * Key for a field that sets a custom attribute.
     * @param attribute The attribute name. This should match the name of a custom attribute configured in your
     *                  Auth backend.
     */
    data class Custom(val attribute: String) : UserAttributeKey(AuthUserAttributeKey.custom(attribute))
}
