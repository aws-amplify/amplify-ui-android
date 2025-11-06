package com.amplifyframework.ui.authenticator.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Allows us to get the Activity reference from Compose LocalContext
 */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> null
}
