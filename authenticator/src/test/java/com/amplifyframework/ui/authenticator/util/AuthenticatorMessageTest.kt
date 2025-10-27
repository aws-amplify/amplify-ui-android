package com.amplifyframework.ui.authenticator.util

import android.content.Context
import com.amplifyframework.auth.AuthException
import com.amplifyframework.ui.authenticator.R
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.reflect.KClass
import org.junit.Test

class AuthenticatorMessageTest {

    @Test
    fun `unknown error message returns resource string if defined`() {
        val context = mockk<Context> {
            every { packageName } returns "package"
            every {
                resources.getIdentifier("amplify_ui_authenticator_error_missing_configuration", "string", "package")
            } returns 42
            every { getString(42) } returns "override string"
        }

        val message = UnknownErrorMessage(MissingConfigurationException())
        message.message(context, mutableMapOf()) shouldBe "override string"
    }

    @Test
    fun `unknown error message caches resource string if defined`() {
        val cache = mutableMapOf<KClass<out AuthException>, String>()
        val context = mockk<Context> {
            every { packageName } returns "package"
            every {
                resources.getIdentifier("amplify_ui_authenticator_error_missing_configuration", "string", "package")
            } returns 42
            every { getString(42) } returns "override string"
        }

        // Call multiple times
        val message = UnknownErrorMessage(MissingConfigurationException())
        message.message(context, cache) shouldBe "override string"
        cache.shouldHaveSize(1)
        message.message(context, cache) shouldBe "override string"

        // Resource should have only be read once
        verify(exactly = 1) {
            context.getString(42)
        }
    }

    @Test
    fun `unknown error message returns default message if resource string not defined`() {
        val context = mockk<Context> {
            every { packageName } returns "package"
            every {
                resources.getIdentifier("amplify_ui_authenticator_error_missing_configuration", "string", "package")
            } returns 0
            every { getString(R.string.amplify_ui_authenticator_error_unknown) } returns "default string"
        }

        val message = UnknownErrorMessage(MissingConfigurationException())
        message.message(context, mutableMapOf()) shouldBe "default string"
    }
}
