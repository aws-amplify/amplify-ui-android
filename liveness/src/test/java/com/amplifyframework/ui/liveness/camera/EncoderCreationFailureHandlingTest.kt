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
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaMuxer
import org.robolectric.shadows.ShadowSurface
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, shadows = [
    ShadowMediaCodec::class,
    ShadowMediaMuxer::class,
    ShadowSurface::class
])
class EncoderCreationFailureHandlingTest {

    private fun setupMocks(shouldThrow: Boolean = true): Triple<File, MediaCodec, (Exception) -> Unit> {
        val tempDir = Files.createTempDirectory("livenessVideoEncoder").toFile()
        
        mockkStatic(MediaCodec::class)
        mockkStatic(MediaFormat::class)
        mockkConstructor(MediaMuxer::class)

        val mockCodec = mockk<MediaCodec>(relaxed = true)
        val mockSurface = mockk<Surface>(relaxed = true)
        val mockOnError = mockk<(Exception) -> Unit>(relaxed = true)

        every { MediaCodec.createEncoderByType(any()) } returns mockCodec
        every { mockCodec.createInputSurface() } returns mockSurface
        every { mockCodec.outputFormat } returns MediaFormat().apply { setInteger(MediaFormat.KEY_HEIGHT, 1) }
        
        if (shouldThrow) {
            every { anyConstructed<MediaMuxer>().addTrack(any()) } throws RuntimeException("addTrack failed")
        } else {
            every { anyConstructed<MediaMuxer>().addTrack(any()) } returns 0
        }

        return Triple(tempDir, mockCodec, mockOnError)
    }

    private fun cleanup() {
        unmockkStatic(MediaCodec::class)
        unmockkStatic(MediaFormat::class)
        unmockkConstructor(MediaMuxer::class)
    }

    @Test
    fun `muxer creation failure calls error callback on third attempt`() {
        val (tempDir, _, mockOnError) = setupMocks(shouldThrow = true)
        val errorSlot = slot<Exception>()

        try {
            val encoder = LivenessVideoEncoder.create(
                cacheDir = tempDir, width = 640, height = 480, bitrate = 1,
                keyframeInterval = 1, framerate = 1,
                onMuxedSegment = { _, _ -> }, onEncoderError = { }, onMuxerError = mockOnError
            )!!

            repeat(2) { encoder.createMuxer() }
            verify(exactly = 0) { mockOnError(any()) }

            encoder.createMuxer()
            verify(exactly = 1) { mockOnError(capture(errorSlot)) }

            runBlocking { encoder.stop(); encoder.destroy() }
        } finally {
            tempDir.deleteRecursively()
            cleanup()
        }
    }

    @Test
    fun `muxer creation success does not call error callback`() {
        val (tempDir, _, mockOnError) = setupMocks(shouldThrow = false)

        try {
            val encoder = LivenessVideoEncoder.create(
                cacheDir = tempDir, width = 640, height = 480, bitrate = 1,
                keyframeInterval = 1, framerate = 1,
                onMuxedSegment = { _, _ -> }, onEncoderError = { }, onMuxerError = mockOnError
            )!!

            repeat(3) { encoder.createMuxer() }
            verify(exactly = 0) { mockOnError(any()) }
            
            runBlocking { encoder.stop(); encoder.destroy() }
        } finally {
            tempDir.deleteRecursively()
            cleanup()
        }
    }
}
