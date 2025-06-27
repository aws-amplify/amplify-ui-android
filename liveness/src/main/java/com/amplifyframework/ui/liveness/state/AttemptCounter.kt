/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.ui.liveness.state

class AttemptCounter {

    fun getCount() = attemptCount

    fun countAttempt() {
        val timestamp = System.currentTimeMillis()
        if (timestamp - latestAttemptTimeStamp > ATTEMPT_COUNT_RESET_INTERVAL_MS) {
            // Reset interval has lapsed so reset the attemptCount
            attemptCount = 0
        }

        attemptCount += 1
        latestAttemptTimeStamp = timestamp
    }

    companion object {
        const val ATTEMPT_COUNT_RESET_INTERVAL_MS = 300_000L
        var attemptCount = 0
        var latestAttemptTimeStamp: Long = System.currentTimeMillis()
    }
}
