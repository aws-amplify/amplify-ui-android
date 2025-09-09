package com.amplifyframework.ui.authenticator.ui

import com.amplifyframework.auth.result.AuthWebAuthnCredential
import com.amplifyframework.ui.authenticator.testUtil.AuthenticatorUiTest
import com.amplifyframework.ui.authenticator.testUtil.mockPasskeyCreatedState
import com.amplifyframework.ui.authenticator.ui.robots.passkeyCreated
import com.amplifyframework.ui.testing.ScreenshotTest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class PasskeyCreatedTest : AuthenticatorUiTest() {

    @Test
    fun `title is Passkey created successfully`() {
        setContent {
            PasskeyCreated(state = mockPasskeyCreatedState())
        }
        passkeyCreated {
            hasTitle("Passkey created successfully!")
        }
    }

    @Test
    fun `button is Continue`() {
        setContent {
            PasskeyCreated(state = mockPasskeyCreatedState())
        }
        passkeyCreated {
            hasContinueButton("Continue")
        }
    }

    @Test
    fun `clicking continue calls done`() {
        val onDone = mockk<suspend () -> Unit>(relaxed = true)
        setContent {
            PasskeyCreated(state = mockPasskeyCreatedState(onDone = onDone))
        }
        passkeyCreated {
            clickContinueButton()
        }
        coVerify { onDone() }
    }

    @Test
    fun `displays existing passkeys when present`() {
        val passkey = mockk<AuthWebAuthnCredential> {
            every { friendlyName } returns "Test Passkey"
        }
        setContent {
            PasskeyCreated(state = mockPasskeyCreatedState(passkeys = listOf(passkey)))
        }
        passkeyCreated {
            hasPasskeyText("Existing Passkeys")
            hasPasskeyText("Test Passkey")
        }
    }

    @Test
    @ScreenshotTest
    fun `with one passkey`() {
        val passkey = mockk<AuthWebAuthnCredential> {
            every { friendlyName } returns "Test Passkey"
        }
        setContent {
            PasskeyCreated(state = mockPasskeyCreatedState(passkeys = listOf(passkey)))
        }
    }

    @Test
    @ScreenshotTest
    fun `with multiple passkeys`() {
        val passkeys = listOf(
            mockk<AuthWebAuthnCredential> { every { friendlyName } returns "Test Passkey 1" },
            mockk<AuthWebAuthnCredential> { every { friendlyName } returns "Test Passkey 2" }
        )
        setContent {
            PasskeyCreated(state = mockPasskeyCreatedState(passkeys = passkeys))
        }
    }
}
