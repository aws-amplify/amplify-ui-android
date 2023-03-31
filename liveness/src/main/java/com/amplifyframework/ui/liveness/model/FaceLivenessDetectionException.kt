/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.liveness.model

open class FaceLivenessDetectionException(
    val message: String,
    val recoverySuggestion: String = "Retry the face liveness check.",
    val throwable: Throwable? = null
) {
    class SessionNotFoundException(
        message: String = "Session not found.",
        recoverySuggestion: String = "Enter a valid session ID.",
        throwable: Throwable? = null
    ) : FaceLivenessDetectionException(message, recoverySuggestion, throwable)

    class AccessDeniedException(
        message: String = "Not authorized to perform a face liveness check.",
        recoverySuggestion: String = "Valid credentials are required for the face liveness check.",
        throwable: Throwable? = null
    ) : FaceLivenessDetectionException(message, recoverySuggestion, throwable)

    class CameraPermissionDeniedException(
        message: String = "Camera permissions have not been granted.",
        recoverySuggestion: String = "Prompt the user to grant camera permission.",
        throwable: Throwable? = null
    ) : FaceLivenessDetectionException(message, recoverySuggestion, throwable)

    class SessionTimedOutException(
        message: String = "Session timed out.",
        recoverySuggestion: String = "Retry the face liveness check and prompt the user" +
            " to follow the on screen instructions.",
        throwable: Throwable? = null
    ) : FaceLivenessDetectionException(message, recoverySuggestion, throwable)

    class UserCancelledException(
        message: String = "User cancelled the face liveness check.",
        recoverySuggestion: String = "Retry the face liveness check.",
        throwable: Throwable? = null
    ) : FaceLivenessDetectionException(message, recoverySuggestion, throwable)
}
