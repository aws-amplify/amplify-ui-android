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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.Test

class SessionErrorMappingTest {

    @Test
    fun `an interrupted session maps to SessionInterruptedException`() {
        val error = mockk<FaceLivenessSessionInterruptedException>(relaxed = true)

        val (exception, stopLivenessSession) = error.toFaceLivenessDetectionException()

        exception.shouldBeInstanceOf<FaceLivenessDetectionException.SessionInterruptedException>()
        exception.throwable shouldBe error
        stopLivenessSession shouldBe false
    }

    @Test
    fun `access denied maps to AccessDeniedException`() {
        val error = mockk<AccessDeniedException>(relaxed = true)

        val (exception, stopLivenessSession) = error.toFaceLivenessDetectionException()

        exception.shouldBeInstanceOf<FaceLivenessDetectionException.AccessDeniedException>()
        exception.throwable shouldBe error
        stopLivenessSession shouldBe false
    }

    @Test
    fun `a missing session maps to SessionNotFoundException`() {
        val error = mockk<FaceLivenessSessionNotFoundException>(relaxed = true)

        val (exception, stopLivenessSession) = error.toFaceLivenessDetectionException()

        exception.shouldBeInstanceOf<FaceLivenessDetectionException.SessionNotFoundException>()
        exception.throwable shouldBe error
        stopLivenessSession shouldBe false
    }

    @Test
    fun `a session timeout maps to SessionTimedOutException`() {
        val error = mockk<FaceLivenessSessionTimeoutException>(relaxed = true)

        val (exception, stopLivenessSession) = error.toFaceLivenessDetectionException()

        exception.shouldBeInstanceOf<FaceLivenessDetectionException.SessionTimedOutException>()
        exception.throwable shouldBe error
        stopLivenessSession shouldBe false
    }

    @Test
    fun `an unsupported challenge type maps to UnsupportedChallengeTypeException and stops the session`() {
        val error = mockk<FaceLivenessUnsupportedChallengeTypeException>(relaxed = true)

        val (exception, stopLivenessSession) = error.toFaceLivenessDetectionException()

        exception.shouldBeInstanceOf<FaceLivenessDetectionException.UnsupportedChallengeTypeException>()
        exception.throwable shouldBe error
        stopLivenessSession shouldBe true
    }

    @Test
    fun `an unclassified error keeps its message and recovery suggestion`() {
        val error = PredictionsException("Something went wrong.", "Try turning it off and on again.")

        val (exception, stopLivenessSession) = error.toFaceLivenessDetectionException()

        exception::class shouldBe FaceLivenessDetectionException::class
        exception.message shouldBe "Something went wrong."
        exception.recoverySuggestion shouldBe "Try turning it off and on again."
        exception.throwable shouldBe error
        stopLivenessSession shouldBe false
    }
}
