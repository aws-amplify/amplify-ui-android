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

package com.amplifyframework.ui.liveness.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowSurface

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    manifest = Config.NONE,
    shadows = [
        ShadowMediaCodec::class,
        ShadowSurface::class
    ]
)
class EncoderCreationFailureHandlingTest {

    private lateinit var tempDir: File
    private lateinit var mockCodec: MediaCodec
    private lateinit var mockOnError: (Exception) -> Unit

    @Before
    fun setupMocks() {
        tempDir = Files.createTempDirectory("livenessVideoEncoder").toFile()

        mockkStatic(MediaCodec::class)
        mockkStatic(MediaFormat::class)

        mockCodec = mockk<MediaCodec>(relaxed = true)
        val mockSurface = mockk<Surface>(relaxed = true)
        val mockFormat = mockk<MediaFormat>(relaxed = true)
        mockOnError = mockk<(Exception) -> Unit>(relaxed = true)

        every { MediaCodec.createEncoderByType(any()) } returns mockCodec
        every { mockCodec.createInputSurface() } returns mockSurface
        every { mockCodec.outputFormat } returns mockFormat
    }

    @After
    fun cleanup() {
        unmockkStatic(MediaCodec::class)
        unmockkStatic(MediaFormat::class)
    }

    @Test
    fun `muxer creation failure calls error callback on third attempt`() {
        val errorSlot = slot<Exception>()

        val failingMuxerFactory: (VideoCodec) -> LivenessMuxer = {
            throw RuntimeException("Muxer creation failed")
        }

        val encoder = LivenessVideoEncoder(
            videoCodec = VideoCodec.VP8,
            outputFile = tempDir,
            width = 640,
            height = 480,
            bitrate = 1,
            keyframeInterval = 1,
            frameRate = 1,
            onMuxedSegment = { _, _ -> }, onEncoderError = { }, onMuxerError = mockOnError,
            muxerFactory = failingMuxerFactory
        )

        repeat(2) { encoder.createMuxer() }
        verify(exactly = 0) { mockOnError(any()) }

        encoder.createMuxer()
        verify(exactly = 1) { mockOnError(capture(errorSlot)) }

        runBlocking { encoder.stop(); encoder.destroy() }
        tempDir.deleteRecursively()
    }

    @Test
    fun `muxer creation success does not call error callback`() {
        val successMuxerFactory: (VideoCodec) -> LivenessMuxer = {
            mockk<LivenessMuxer>(relaxed = true)
        }

        val encoder = LivenessVideoEncoder(
            videoCodec = VideoCodec.VP8,
            outputFile = tempDir,
            width = 640,
            height = 480,
            bitrate = 1,
            keyframeInterval = 1,
            frameRate = 1,
            onMuxedSegment = { _, _ -> },
            onEncoderError = { },
            onMuxerError = mockOnError,
            muxerFactory = successMuxerFactory
        )

        repeat(3) { encoder.createMuxer() }
        verify(exactly = 0) { mockOnError(any()) }

        runBlocking { encoder.stop(); encoder.destroy() }
        tempDir.deleteRecursively()
    }
}
