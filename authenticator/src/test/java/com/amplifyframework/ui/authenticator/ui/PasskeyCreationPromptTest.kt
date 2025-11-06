package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.ui.authenticator.PasskeyCreationPromptState.Action
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockPasskeyCreationPromptState
import com.amplifyframework.ui.authenticator.ui.robots.passkeyCreationPrompt
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Test

class PasskeyCreationPromptTest : AuthenticatorUiTest() {

    @Test
    fun `title is Sign in faster with Passkey`() {
        setContent {
            PasskeyPrompt(state = mockPasskeyCreationPromptState())
        }
        passkeyCreationPrompt {
            hasTitle("Sign in faster with Passkey")
        }
    }

    @Test
    fun `has create passkey button`() {
        setContent {
            PasskeyPrompt(state = mockPasskeyCreationPromptState())
        }
        passkeyCreationPrompt {
            hasCreatePasskeyButton("Create a Passkey")
        }
    }

    @Test
    fun `has skip passkey button`() {
        setContent {
            PasskeyPrompt(state = mockPasskeyCreationPromptState())
        }
        passkeyCreationPrompt {
            hasSkipPasskeyButton("Continue without a Passkey")
        }
    }

    @Test
    fun `clicking create passkey calls createPasskey`() {
        val onSubmit = mockk<suspend () -> Unit>(relaxed = true)
        setContent {
            PasskeyPrompt(state = mockPasskeyCreationPromptState(onSubmit = onSubmit))
        }
        passkeyCreationPrompt {
            clickCreatePasskeyButton()
        }
        coVerify { onSubmit() }
    }

    @Test
    fun `clicking skip calls skip`() {
        val onSkip = mockk<suspend () -> Unit>(relaxed = true)
        setContent {
            PasskeyPrompt(state = mockPasskeyCreationPromptState(onSkip = onSkip))
        }
        passkeyCreationPrompt {
            clickSkipPasskeyButton()
        }
        coVerify { onSkip() }
    }

    @Test
    @ScreenshotTest
    fun `default state`() {
        setContent {
            PasskeyPrompt(state = mockPasskeyCreationPromptState())
        }
    }

    @Test
    @ScreenshotTest
    fun `creating passkey`() {
        setContent {
            PasskeyPrompt(
                state = mockPasskeyCreationPromptState(action = Action.CreatePasskey())
            )
        }
    }

    @Test
    @ScreenshotTest
    fun `skipping passkey creation`() {
        setContent {
            PasskeyPrompt(
                state = mockPasskeyCreationPromptState(action = Action.Skip())
            )
        }
    }
}
