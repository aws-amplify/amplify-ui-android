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

package com.amplifyframework.ui.authenticator.forms

import com.amplifyframework.auth.AuthUserAttributeKey

/**
 * An [FieldKey] uniquely identifies a particular field in a form.
 */
abstract class FieldKey internal constructor() {
    object Username : FieldKey()
    object Password : FieldKey()
    object ConfirmPassword : FieldKey()
    object ConfirmationCode : FieldKey()
    object VerificationAttribute : FieldKey()

    /**
     * A [UserAttributeKey] is an [FieldKey] for a field that maps to an
     * [AuthUserAttributeKey] in Amplify.
     */
    abstract class UserAttributeKey internal constructor(val attributeKey: AuthUserAttributeKey) :
        FieldKey()

    // Fields supported out-of-the-box
    object Email : UserAttributeKey(AuthUserAttributeKey.email())
    object PhoneNumber : UserAttributeKey(AuthUserAttributeKey.phoneNumber())
    object Birthdate : UserAttributeKey(AuthUserAttributeKey.birthdate())
    object FamilyName : UserAttributeKey(AuthUserAttributeKey.familyName())
    object GivenName : UserAttributeKey(AuthUserAttributeKey.givenName())
    object MiddleName : UserAttributeKey(AuthUserAttributeKey.middleName())
    object Name : UserAttributeKey(AuthUserAttributeKey.name())
    object Nickname : UserAttributeKey(AuthUserAttributeKey.nickname())
    object PreferredUsername : UserAttributeKey(AuthUserAttributeKey.preferredUsername())
    object Profile : UserAttributeKey(AuthUserAttributeKey.profile())

    /**
     * The default website field.
     */
    object Website : UserAttributeKey(AuthUserAttributeKey.website())

    /**
     * Key for a custom attribute.
     * @param attribute The attribute name. This should match the name of a custom attribute configured in your
     *                  Auth backend.
     */
    data class Custom(val attribute: String) : UserAttributeKey(AuthUserAttributeKey.custom(attribute))
}
