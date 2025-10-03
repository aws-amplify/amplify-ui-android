package com.amplifyframework.ui.authenticator.enums

internal enum class SignInSource {
    // Standard sign in
    SignIn,

    // Automatic sign in after completing sign up
    SignUp,

    // Signed in outside of Authenticator
    External
}
