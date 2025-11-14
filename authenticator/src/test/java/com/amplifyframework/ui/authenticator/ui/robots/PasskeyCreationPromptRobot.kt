package com.amplifyframework.ui.authenticator.ui.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.amplifyframework.ui.authenticator.ui.TestTags
import com.amplifyframework.ui.testing.ComposeTest

fun ComposeTest.passkeyCreationPrompt(func: PasskeyCreationPromptRobot.() -> Unit) =
    PasskeyCreationPromptRobot(composeTestRule).func()

class PasskeyCreationPromptRobot(rule: ComposeTestRule) : ScreenLevelRobot(rule) {
    fun hasCreatePasskeyButton(expected: String) = assertExists(TestTags.CreatePasskeyButton, expected)
    fun hasSkipPasskeyButton(expected: String) = assertExists(TestTags.SkipPasskeyButton, expected)
    fun clickCreatePasskeyButton() = clickOnTag(TestTags.CreatePasskeyButton)
    fun clickSkipPasskeyButton() = clickOnTag(TestTags.SkipPasskeyButton)
    fun hasPromptText(text: String) = assertExists(text)
}
