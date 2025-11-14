package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.ui.authenticator.PromptToCreatePasskeyState.Action
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
            PromptToCreatePasskey(state = mockPasskeyCreationPromptState())
        }
        passkeyCreationPrompt {
            hasTitle("Sign in faster with Passkey")
        }
    }

    @Test
    fun `has create passkey button`() {
        setContent {
            PromptToCreatePasskey(state = mockPasskeyCreationPromptState())
        }
        passkeyCreationPrompt {
            hasCreatePasskeyButton("Create a Passkey")
        }
    }

    @Test
    fun `has skip passkey button`() {
        setContent {
            PromptToCreatePasskey(state = mockPasskeyCreationPromptState())
        }
        passkeyCreationPrompt {
            hasSkipPasskeyButton("Continue without a Passkey")
        }
    }

    @Test
    fun `clicking create passkey calls createPasskey`() {
        val onSubmit = mockk<suspend () -> Unit>(relaxed = true)
        setContent {
            PromptToCreatePasskey(state = mockPasskeyCreationPromptState(onSubmit = onSubmit))
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
            PromptToCreatePasskey(state = mockPasskeyCreationPromptState(onSkip = onSkip))
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
            PromptToCreatePasskey(state = mockPasskeyCreationPromptState())
        }
    }

    @Test
    @ScreenshotTest
    fun `creating passkey`() {
        setContent {
            PromptToCreatePasskey(
                state = mockPasskeyCreationPromptState(action = Action.CreatePasskey())
            )
        }
    }

    @Test
    @ScreenshotTest
    fun `skipping passkey creation`() {
        setContent {
            PromptToCreatePasskey(
                state = mockPasskeyCreationPromptState(action = Action.Skip())
            )
        }
    }
}
