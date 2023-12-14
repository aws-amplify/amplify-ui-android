package com.amplifyframework.ui.liveness.util

enum class ErrorCode(val code: Int) {
    SUCCESS(1000),
    TIMEOUT(4001),
    CANCELED(4003),
    RUNTIME_ERROR(4005),
    DISPOSED(4008)
}
