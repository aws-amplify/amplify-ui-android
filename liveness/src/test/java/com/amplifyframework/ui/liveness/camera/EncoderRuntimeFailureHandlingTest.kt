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

package com.amplifyframework.ui.liveness.camera

import android.media.MediaCodec
import com.amplifyframework.logging.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class EncoderRuntimeFailureHandlingTest {

    private lateinit var callback: EncoderCallback
    private lateinit var mockOnError: (MediaCodec.CodecException) -> Unit
    private lateinit var mockLogger: Logger

    @Before
    fun createCallback() {
        val mockHandleFrame = mockk<(Int, MediaCodec.BufferInfo) -> Unit>()
        mockOnError = mockk<(MediaCodec.CodecException) -> Unit>(relaxed = true)
        mockLogger = mockk<Logger>(relaxed = true)
        callback = EncoderCallback(mockHandleFrame, mockOnError, mockLogger)
    }

    @Test
    fun `callback handles transient errors without calling onEncoderError`() {
        val transientError = mockk<MediaCodec.CodecException>(relaxed = true) {
            every { isTransient } returns true
        }
        callback.onError(mockk(), transientError)

        verify(exactly = 0) { mockOnError(any()) }
        verify { mockLogger.warn(any(), transientError) }
    }

    @Test
    fun `callback handles non-transient errors by calling onEncoderError`() {
        val fatalError = mockk<MediaCodec.CodecException>(relaxed = true) {
            every { isTransient } returns false
        }
        callback.onError(mockk(), fatalError)

        verify(exactly = 1) { mockOnError(fatalError) }
        verify { mockLogger.error(any(), fatalError) }
    }
}
