package com.amplifyframework.ui.authenticator.ui.robots

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.amplifyframework.ui.authenticator.forms.FieldKey
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.testing.ComposeTest

fun ComposeTest.passwordResetConfirm(func: PasswordResetConfirmRobot.() -> Unit) = PasswordResetConfirmRobot(composeTestRule).func()

class PasswordResetConfirmRobot(rule: ComposeTestRule) : ScreenLevelRobot(rule) {
    fun hasSubmitButton(expected: String) = assertExists(TestTags.PasswordResetButton, expected)
    fun hasPasswordContentType(contentType: ContentType) = hasContentType(FieldKey.Password, contentType)
    fun hasConfirmPasswordContentType(contentType: ContentType) = hasContentType(FieldKey.ConfirmPassword, contentType)

    fun setConfirmationCode(value: String) = setFieldContent(FieldKey.ConfirmationCode, value)
    fun setPassword(value: String) = setFieldContent(FieldKey.Password, value)
    fun setConfirmPassword(value: String) = setFieldContent(FieldKey.ConfirmPassword, value)
    fun clickBackToSignIn() = clickOnTag(TestTags.BackToSignInButton)
}
