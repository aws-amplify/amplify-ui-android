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
import android.media.MediaFormat
import com.amplifyframework.ui.liveness.camera.OnMuxedSegment
import com.amplifyframework.ui.liveness.testUtil.Mp4Reader
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Holds [Mp4Muxer] to the layout it rewrites while driving the muxer it actually uses, rather than
 * fragments built by hand. A muxer that writes a layout the rewriter does not recognise sends its
 * chunks on unchanged, which costs nothing at build time and shows up only as a liveness check that
 * scores worse, so these assertions are what turns that into a failing build.
 *
 * Robolectric is needed because the muxer reaches for the Android framework while writing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class Mp4MuxerFragmentLayoutTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val chunks = mutableListOf<ByteArray>()
    private val onMuxedSegment: OnMuxedSegment = { bytes, _ -> chunks.add(bytes) }

    @Test
    fun `every chunk locates its own sample data`() {
        record(seconds = 3)

        chunks.size shouldBeGreaterThan 1
        chunks.forEach { chunk ->
            val read = Mp4Reader(chunk)
            val fragments = read.boxes("moof")
            val samples = read.boxes("trun")
            val payloads = read.boxes("mdat")

            fragments shouldHaveSize payloads.size
            fragments.indices.map { fragments[it].offset + samples[it].dataOffset } shouldContainExactly
                payloads.map { it.contentOffset }
        }
    }

    @Test
    fun `every fragment states its decode time`() {
        record(seconds = 3)

        val fragments = chunks.flatMap { chunk ->
            val read = Mp4Reader(chunk)
            read.boxes("tfdt").zip(read.boxes("trun")) { time, run ->
                Fragment(decodeTime = time.decodeTime, duration = run.sampleDurations.sum())
            }
        }

        fragments.first().decodeTime shouldBe 0L
        fragments.windowed(2).forEach { (earlier, later) ->
            // Each fragment begins where the one before it ended
            later.decodeTime shouldBe earlier.decodeTime + earlier.duration
        }
    }

    @Test
    fun `every fragment measures its sample data from itself`() {
        record(seconds = 3)

        val trackHeaders = chunks.flatMap { Mp4Reader(it).boxes("tfhd") }

        trackHeaders.size shouldBeGreaterThan 1
        trackHeaders.forEach { it.flags shouldBe DEFAULT_BASE_IS_MOOF }
    }

    @Test
    fun `the first chunk declares the brand that permits it`() {
        record(seconds = 3)

        Mp4Reader(chunks.first()).brands() shouldContainExactly listOf("isom", "iso2", "mp41", "iso5")
    }

    @Test
    fun `the chunks joined together locate their sample data`() {
        record(seconds = 3)

        val joined = Mp4Reader(chunks.reduce { all, chunk -> all + chunk })
        val fragments = joined.boxes("moof")
        val samples = joined.boxes("trun")
        val payloads = joined.boxes("mdat")

        fragments shouldHaveSize payloads.size
        fragments.indices.map { fragments[it].offset + samples[it].dataOffset } shouldContainExactly
            payloads.map { it.contentOffset }
    }

    /**
     * Encodes nothing, but feeds the muxer the sample data and timing an encoder would, so that the
     * muxer lays out fragments as it does for a liveness check.
     */
    private fun record(seconds: Int) {
        val muxer = Mp4Muxer()
        muxer.start(folder.newFile(), videoFormat(), onMuxedSegment)

        repeat(seconds * FRAMES_PER_SECOND) { frame ->
            val keyFrame = frame % FRAMES_PER_SECOND == 0
            val sample = sample(keyFrame)
            muxer.write(
                sample,
                MediaCodec.BufferInfo().apply {
                    set(
                        0,
                        sample.remaining(),
                        frame.toLong() * 1_000_000 / FRAMES_PER_SECOND,
                        if (keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    )
                }
            )
        }

        muxer.stop()
    }

    private fun videoFormat() = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 480, 640).apply {
        setInteger(MediaFormat.KEY_FRAME_RATE, FRAMES_PER_SECOND)
        // The muxer reads the profile and level out of the sequence parameter set, so it has to be real
        setByteBuffer("csd-0", ByteBuffer.wrap(START_CODE + SEQUENCE_PARAMETER_SET))
        setByteBuffer("csd-1", ByteBuffer.wrap(START_CODE + PICTURE_PARAMETER_SET))
    }

    /** A sample the muxer can split into units, holding bytes that cannot be mistaken for a unit. */
    private fun sample(keyFrame: Boolean): ByteBuffer {
        val header = if (keyFrame) IDR_SLICE else NON_IDR_SLICE
        val bytes = START_CODE + byteArrayOf(header) + ByteArray(SAMPLE_BYTES) { 0xAA.toByte() }
        return ByteBuffer.allocate(bytes.size).put(bytes).also { it.flip() }
    }

    private class Fragment(val decodeTime: Long, val duration: Long)

    private companion object {
        const val DEFAULT_BASE_IS_MOOF = 0x020000
        const val FRAMES_PER_SECOND = 30
        const val SAMPLE_BYTES = 256

        const val IDR_SLICE = 0x65.toByte()
        const val NON_IDR_SLICE = 0x41.toByte()
        val START_CODE = byteArrayOf(0, 0, 0, 1)

        /*
        Taken from a recorded liveness check, because the muxer parses the sequence parameter set and
        rejects bytes that do not describe a stream.
         */
        val SEQUENCE_PARAMETER_SET = byteArrayOf(
            0x67, 0x64, 0x00, 0x1e, 0xac.toByte(), 0xb4.toByte(), 0x0f, 0x02,
            0x8d.toByte(), 0x35, 0x06, 0x06, 0x06, 0x07, 0x8a.toByte(), 0x15, 0x50
        )
        val PICTURE_PARAMETER_SET = byteArrayOf(0x68, 0xee.toByte(), 0x0d, 0x8b.toByte())
    }
}
