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

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import org.junit.Test

class Mp4FragmentRewriterTest {

    private val rewriter = Mp4FragmentRewriter()

    @Test
    fun `locates sample data relative to the fragment`() {
        val chunk = chunk(fragment(sampleDurations = listOf(100, 100, 100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.box("tfhd").flags shouldBe DEFAULT_BASE_IS_MOOF
        val fragment = rewritten.box("moof")
        fragment.offset + rewritten.box("trun").dataOffset shouldBe rewritten.box("mdat").contentOffset
    }

    @Test
    fun `states the decode time of the first fragment as zero`() {
        val chunk = chunk(fragment(sampleDurations = listOf(100, 100, 100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.box("tfdt").decodeTime shouldBe 0
    }

    @Test
    fun `accumulates the decode time across fragments`() {
        val chunks = listOf(300, 400, 500).map { chunk(fragment(sampleDurations = listOf(it, it))) }

        val decodeTimes = chunks.map { Mp4Reader(rewriter.rewrite(it)).box("tfdt").decodeTime }

        decodeTimes shouldContainExactly listOf(0L, 600L, 1400L)
    }

    @Test
    fun `leaves the sample data untouched`() {
        val chunk = chunk(fragment(sampleDurations = listOf(100, 100)))
        val original = Mp4Reader(chunk).let { it.bytes(it.box("mdat")) }

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.bytes(rewritten.box("mdat")).toList() shouldContainExactly original.toList()
    }

    @Test
    fun `copies boxes it does not need to change`() {
        val movie = box("moov", ByteArray(12) { it.toByte() })
        val chunk = movie + chunk(fragment(sampleDurations = listOf(100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.bytes(rewritten.box("moov")).toList() shouldContainExactly movie.toList()
    }

    @Test
    fun `declares the brand that permits fragment relative offsets`() {
        val chunk = fileType("isom", "isom", "iso2", "mp41") + chunk(fragment(sampleDurations = listOf(100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.brands() shouldContainExactly listOf("isom", "iso2", "mp41", "iso5")
    }

    @Test
    fun `does not repeat a brand that is already declared`() {
        val chunk = fileType("isom", "isom", "iso5") + chunk(fragment(sampleDurations = listOf(100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.brands() shouldContainExactly listOf("isom", "iso5")
    }

    @Test
    fun `sends a chunk it cannot read unchanged`() {
        val chunk = ByteArray(40) { 1 }

        rewriter.rewrite(chunk).toList() shouldContainExactly chunk.toList()
    }

    @Test
    fun `stops rewriting once a chunk cannot be read`() {
        val readable = chunk(fragment(sampleDurations = listOf(100)))

        rewriter.rewrite(ByteArray(40) { 1 })

        rewriter.rewrite(readable).toList() shouldContainExactly readable.toList()
    }

    private fun chunk(fragment: ByteArray, sampleBytes: Int = 64) =
        fragment + box("mdat", ByteArray(sampleBytes) { it.toByte() })

    private fun fileType(majorBrand: String, vararg compatibleBrands: String) = box(
        "ftyp",
        ByteBuffer.allocate(8 + compatibleBrands.size * 4).apply {
            put(majorBrand.toByteArray(Charsets.US_ASCII))
            putInt(0x020000) // minor version
            compatibleBrands.forEach { put(it.toByteArray(Charsets.US_ASCII)) }
        }.array()
    )

    /**
     * Builds a movie fragment in the shape the muxer produces: a track fragment header holding an
     * absolute position, and a track run recording a duration, size and flags for every sample.
     */
    private fun fragment(sampleDurations: List<Int>, baseDataOffset: Long = 5_000, sampleSize: Int = 8): ByteArray {
        val runSize = BOX_HEADER_SIZE + RUN_CONTENT_SIZE + sampleDurations.size * BYTES_PER_SAMPLE
        val trackSize = BOX_HEADER_SIZE + TRACK_HEADER_SIZE + runSize
        val fragmentSize = BOX_HEADER_SIZE + FRAGMENT_HEADER_SIZE + trackSize

        // The samples follow the fragment, after the header of the box that holds them
        val dataOffset = fragmentSize + BOX_HEADER_SIZE

        val fragmentHeader = box("mfhd", ByteBuffer.allocate(8).putInt(0).putInt(1).array())
        val trackHeader = box(
            "tfhd",
            ByteBuffer.allocate(16).putInt(BASE_DATA_OFFSET_PRESENT).putInt(1).putLong(baseDataOffset).array()
        )
        val trackRun = box(
            "trun",
            ByteBuffer.allocate(RUN_CONTENT_SIZE + sampleDurations.size * BYTES_PER_SAMPLE).apply {
                putInt(1 shl 24 or BASE_DATA_OFFSET_PRESENT or DURATION_SIZE_AND_FLAGS_PRESENT)
                putInt(sampleDurations.size)
                putInt(dataOffset)
                sampleDurations.forEach { putInt(it).putInt(sampleSize).putInt(0) }
            }.array()
        )

        return box("moof", fragmentHeader + box("traf", trackHeader + trackRun))
    }

    private fun box(type: String, content: ByteArray) = ByteBuffer.allocate(BOX_HEADER_SIZE + content.size)
        .putInt(BOX_HEADER_SIZE + content.size)
        .put(type.toByteArray(Charsets.US_ASCII))
        .put(content)
        .array()

    /**
     * Finds boxes by type at any depth, so that a rewritten chunk can be inspected without knowing
     * the size of the boxes that contain them.
     */
    private class Mp4Reader(private val chunk: ByteArray) {

        class Box(val offset: Int, val size: Int, private val chunk: ByteArray) {
            val contentOffset get() = offset + BOX_HEADER_SIZE
            val flags get() = readInt(contentOffset) and 0xFFFFFF
            val decodeTime get() = ByteBuffer.wrap(chunk, contentOffset + 4, 8).long
            val dataOffset get() = readInt(contentOffset + 8)
            private fun readInt(index: Int) = ByteBuffer.wrap(chunk, index, 4).int
        }

        fun box(type: String): Box = requireNotNull(find(type, 0, chunk.size)) { "No $type box in chunk" }

        fun bytes(box: Box): ByteArray = chunk.copyOfRange(box.offset, box.offset + box.size)

        fun brands(): List<String> {
            val fileType = box("ftyp")
            val start = fileType.contentOffset + 8 // after the major brand and minor version
            return (start until fileType.offset + fileType.size step 4).map {
                String(chunk, it, 4, Charsets.US_ASCII)
            }
        }

        private fun find(type: String, start: Int, end: Int): Box? {
            var position = start
            while (position + BOX_HEADER_SIZE <= end) {
                val size = ByteBuffer.wrap(chunk, position, 4).int
                val boxType = String(chunk, position + 4, 4, Charsets.US_ASCII)
                if (boxType == type) return Box(position, size, chunk)
                if (boxType in CONTAINERS) {
                    find(type, position + BOX_HEADER_SIZE, position + size)?.let { return it }
                }
                position += size
            }
            return null
        }

        private companion object {
            val CONTAINERS = setOf("moof", "traf")
        }
    }

    private companion object {
        const val BOX_HEADER_SIZE = 8
        const val FRAGMENT_HEADER_SIZE = 16
        const val TRACK_HEADER_SIZE = 24
        const val RUN_CONTENT_SIZE = 12
        const val BYTES_PER_SAMPLE = 12

        const val BASE_DATA_OFFSET_PRESENT = 0x000001
        const val DURATION_SIZE_AND_FLAGS_PRESENT = 0x000700
        const val DEFAULT_BASE_IS_MOOF = 0x020000
    }
}
