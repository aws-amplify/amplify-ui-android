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
import com.amplifyframework.ui.liveness.testUtil.Mp4Reader
import com.amplifyframework.ui.liveness.testUtil.TestMuxer
import com.amplifyframework.ui.liveness.testUtil.atFileOffset
import com.amplifyframework.ui.liveness.testUtil.chunkOf
import com.amplifyframework.ui.liveness.testUtil.movieFragment
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
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

    /*
    TestMuxer writes the bytes it is given straight to the output file, so supplying complete movie
    fragments as sample data puts a readable fragment in the file without a real muxer.
     */
    @Test
    fun `sends rewritten fragments`() {
        val file = folder.newFile()
        val chunk = slot<ByteArray>()

        muxer.start(outputFile = file, mediaFormat = mockk(), onMuxedSegment = onMuxedSegment)

        // The fragments record where they land in the file, which is what the rewrite is checked against
        val first = chunkOf(movieFragment(sampleDurations = listOf(100, 100)))
        val second = chunkOf(movieFragment(sampleDurations = listOf(100, 100)))
        val written = atFileOffset(first + second)

        muxer.write(buffer(written, 0, first.size), bufferInfo(isKeyFrame = true))
        muxer.write(buffer(written, first.size, written.size), bufferInfo(isKeyFrame = true))

        verify { onMuxedSegment.invoke(capture(chunk), any()) }
        val sent = Mp4Reader(chunk.captured)
        sent.boxes("tfdt").map { it.decodeTime } shouldContainExactly listOf(0L, 200L)
        sent.boxes("tfhd").map { it.flags } shouldContainExactly listOf(DEFAULT_BASE_IS_MOOF, DEFAULT_BASE_IS_MOOF)
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

    private fun buffer(bytes: ByteArray, from: Int, to: Int): ByteBuffer =
        ByteBuffer.allocate(to - from).put(bytes, from, to - from).also { it.flip() }

    private companion object {
        const val DEFAULT_BASE_IS_MOOF = 0x020000
    }

    private fun randomData(numBytes: Int = 100): ByteBuffer {
        val bytes = Random.nextBytes(numBytes)
        return ByteBuffer.allocate(numBytes).also {
            it.put(bytes)
            it.flip()
        }
    }
}
