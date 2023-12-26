package com.amplifyframework.ui.liveness.util

internal enum class WebSocketCloseCode(val code: Int) {
    TIMEOUT(4001),
    CANCELED(4003),
    RUNTIME_ERROR(4005),
    DISPOSED(4008)
}
