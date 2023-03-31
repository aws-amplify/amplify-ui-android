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

package com.amplifyframework.ui.sample.liveness

import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object LivenessSampleBackend {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createSession(): String {
        val request = RestOptions.builder()
            .addPath("/liveness/create")
            .build()

        return Amplify.API.post(request).data.asJSONObject()["sessionId"] as String
    }

    suspend fun getLivenessSessionResults(sessionId: String): LivenessSessionResult {
        val request = RestOptions.builder()
            .addPath("/liveness/$sessionId")
            .build()

        val result = Amplify.API.get(request)
        return json.decodeFromString(result.data.asString())
    }
}

@Serializable
data class LivenessSessionResult(
    val confidenceScore: Float,
    val isLive: Boolean,
    val auditImageBytes: String
)