package com.amplifyframework.ui.authenticator.util

import android.os.Build

// Facade for android.os.Build to facilitate testing
internal class OsBuild {
    val sdkInt: Int
        get() = Build.VERSION.SDK_INT
}
