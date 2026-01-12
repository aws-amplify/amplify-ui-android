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

package com.amplifyframework.ui.liveness.media

import android.media.MediaCodec
import androidx.media3.common.util.MediaFormatUtil
import com.amplifyframework.ui.liveness.camera.OnMuxedSegment
import com.amplifyframework.ui.liveness.testUtil.TestMuxer
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.nio.ByteBuffer
import kotlin.random.Random
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class Mp4MuxerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var testMuxer: TestMuxer
    private val muxer = Mp4Muxer(
        createMediaMuxer = { stream -> TestMuxer(stream).also { testMuxer = it } }
    )

    private val onMuxedSegment = mockk<OnMuxedSegment>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(MediaFormatUtil::class)
        every { MediaFormatUtil.createFormatFromMediaFormat(any()) } returns mockk()
    }

    @After
    fun teardown() {
        unmockkStatic(MediaFormatUtil::class)
    }

    @Test
    fun `does not send segment on first keyframe`() {
        val file = folder.newFile()

        muxer.start(
            outputFile = file,
            mediaFormat = mockk(),
            onMuxedSegment = onMuxedSegment
        )

        muxer.write(randomData(), bufferInfo(isKeyFrame = true))

        verify(exactly = 0) {
            onMuxedSegment.invoke(any(), any())
        }
    }

    @Test
    fun `does not send segment on non-keyframe`() {
        val file = folder.newFile()

        muxer.start(
            outputFile = file,
            mediaFormat = mockk(),
            onMuxedSegment = onMuxedSegment
        )

        muxer.write(randomData(), bufferInfo(isKeyFrame = true))
        muxer.write(randomData(), bufferInfo(isKeyFrame = false))
        muxer.write(randomData(), bufferInfo(isKeyFrame = false))

        verify(exactly = 0) {
            onMuxedSegment.invoke(any(), any())
        }
    }

    @Test
    fun `sends segment after subsequent keyframes`() {
        val file = folder.newFile()

        muxer.start(
            outputFile = file,
            mediaFormat = mockk(),
            onMuxedSegment = onMuxedSegment
        )

        muxer.write(randomData(), bufferInfo(isKeyFrame = true))
        muxer.write(randomData(), bufferInfo(isKeyFrame = false))
        muxer.write(randomData(), bufferInfo(isKeyFrame = false))
        muxer.write(randomData(), bufferInfo(isKeyFrame = true))
        muxer.write(randomData(), bufferInfo(isKeyFrame = false))
        muxer.write(randomData(), bufferInfo(isKeyFrame = true))

        verify(exactly = 2) {
            onMuxedSegment.invoke(any(), any())
        }
    }

    @Test
    fun `closes media muxer on stop`() {
        val file = folder.newFile()
        muxer.start(
            outputFile = file,
            mediaFormat = mockk(),
            onMuxedSegment = onMuxedSegment
        )

        muxer.stop()

        testMuxer.closed.shouldBeTrue()
    }

    private fun bufferInfo(isKeyFrame: Boolean = false) = MediaCodec.BufferInfo().apply {
        if (isKeyFrame) flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
    }

    private fun randomData(numBytes: Int = 100): ByteBuffer {
        val bytes = Random.nextBytes(numBytes)
        return ByteBuffer.allocate(numBytes).also {
            it.put(bytes)
            it.flip()
        }
    }
}
