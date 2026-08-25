/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.exceptions.AccessDeniedException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessSessionInterruptedException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessSessionNotFoundException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessSessionTimeoutException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessUnsupportedChallengeTypeException

/**
 * Translates a failure from the predictions layer into the exception the host app receives, paired with whether the
 * client still needs to stop the liveness session. The session only needs stopping when the socket is expected to
 * still be open; failures reported by the service arrive after it has already closed.
 */
internal fun PredictionsException.toFaceLivenessDetectionException(): Pair<FaceLivenessDetectionException, Boolean> =
    when (this) {
        is AccessDeniedException ->
            FaceLivenessDetectionException.AccessDeniedException(throwable = this) to false
        is FaceLivenessSessionInterruptedException ->
            FaceLivenessDetectionException.SessionInterruptedException(throwable = this) to false
        is FaceLivenessSessionNotFoundException ->
            FaceLivenessDetectionException.SessionNotFoundException(throwable = this) to false
        is FaceLivenessSessionTimeoutException ->
            FaceLivenessDetectionException.SessionTimedOutException(throwable = this) to false
        is FaceLivenessUnsupportedChallengeTypeException ->
            FaceLivenessDetectionException.UnsupportedChallengeTypeException(throwable = this) to true
        else -> FaceLivenessDetectionException(
            message ?: "Unknown error.",
            recoverySuggestion,
            this
        ) to false
    }
