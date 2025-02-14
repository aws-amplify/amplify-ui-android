package com.amplifyframework.ui.authenticator.ui.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.testing.ComposeTest

fun ComposeTest.passkeyCreated(func: PasskeyCreatedRobot.() -> Unit) = PasskeyCreatedRobot(composeTestRule).func()

class PasskeyCreatedRobot(rule: ComposeTestRule) : ScreenLevelRobot(rule) {
    fun hasContinueButton(expected: String) = assertExists(TestTags.ContinueButton, expected)
    fun hasPasskeyText(text: String) = assertExists(text)

    fun clickContinueButton() = clickOnTag(TestTags.ContinueButton)

}