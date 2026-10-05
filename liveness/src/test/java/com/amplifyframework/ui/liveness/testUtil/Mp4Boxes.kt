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

package com.amplifyframework.ui.liveness.testUtil

import java.nio.ByteBuffer

private const val BOX_HEADER_SIZE = 8
private const val BASE_DATA_OFFSET_PRESENT = 0x000001
private const val FIRST_SAMPLE_FLAGS_PRESENT = 0x000004
private const val DURATION_SIZE_AND_FLAGS_PRESENT = 0x000700
private const val FRAGMENT_HEADER_SIZE = 16
private const val TRACK_HEADER_SIZE = 24
private const val RUN_CONTENT_SIZE = 12
private const val BYTES_PER_SAMPLE = 12

internal fun box(type: String, content: ByteArray): ByteArray =
    ByteBuffer.allocate(BOX_HEADER_SIZE + content.size)
        .putInt(BOX_HEADER_SIZE + content.size)
        .put(type.toByteArray(Charsets.US_ASCII))
        .put(content)
        .array()

internal fun fileType(majorBrand: String, vararg compatibleBrands: String): ByteArray = box(
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
 *
 * The remaining parameters build the shapes the muxer does not produce, so that a rewriter can be
 * held to rejecting them.
 */
internal fun movieFragment(
    sampleDurations: List<Int>,
    baseDataOffset: Long = 0,
    sampleSize: Int = 8,
    tracks: Int = 1,
    firstSampleFlags: Boolean = false,
    runsPerTrack: Int = 1,
    sampleDataOffset: Int? = null,
    extraFragmentBoxes: List<ByteArray> = emptyList(),
    extraTrackBoxes: List<ByteArray> = emptyList()
): ByteArray {
    val firstSampleFlagsSize = if (firstSampleFlags) 4 else 0
    val runContentSize = RUN_CONTENT_SIZE + firstSampleFlagsSize + sampleDurations.size * BYTES_PER_SAMPLE
    val runSize = BOX_HEADER_SIZE + runContentSize
    val trackSize = BOX_HEADER_SIZE + TRACK_HEADER_SIZE + runsPerTrack * runSize +
        extraTrackBoxes.sumOf { it.size }
    val fragmentSize = BOX_HEADER_SIZE + FRAGMENT_HEADER_SIZE + tracks * trackSize +
        extraFragmentBoxes.sumOf { it.size }

    // The samples follow the fragment, after the header of the box that holds them
    val dataOffset = sampleDataOffset ?: (fragmentSize + BOX_HEADER_SIZE)

    val fragmentHeader = box("mfhd", ByteBuffer.allocate(8).putInt(0).putInt(1).array())
    val trackHeader = box(
        "tfhd",
        ByteBuffer.allocate(16).putInt(BASE_DATA_OFFSET_PRESENT).putInt(1).putLong(baseDataOffset).array()
    )
    val trackRun = box(
        "trun",
        ByteBuffer.allocate(runContentSize).apply {
            val flags = BASE_DATA_OFFSET_PRESENT or DURATION_SIZE_AND_FLAGS_PRESENT or
                if (firstSampleFlags) FIRST_SAMPLE_FLAGS_PRESENT else 0
            putInt(1 shl 24 or flags)
            putInt(sampleDurations.size)
            putInt(dataOffset)
            if (firstSampleFlags) putInt(0)
            sampleDurations.forEach { putInt(it).putInt(sampleSize).putInt(0) }
        }.array()
    )

    val trackFragment = box("traf", (listOf(trackHeader) + List(runsPerTrack) { trackRun } + extraTrackBoxes).join())

    return box("moof", (listOf(fragmentHeader) + List(tracks) { trackFragment } + extraFragmentBoxes).join())
}

private fun List<ByteArray>.join(): ByteArray {
    val joined = ByteArray(sumOf { it.size })
    var at = 0
    forEach {
        it.copyInto(joined, at)
        at += it.size
    }
    return joined
}

/** A movie fragment followed by the box holding its sample data. */
internal fun chunkOf(fragment: ByteArray, sampleBytes: Int = 64): ByteArray =
    fragment + box("mdat", ByteArray(sampleBytes) { it.toByte() })

/**
 * Records in every movie fragment of a chunk the position it sits at, as the muxer does when it writes
 * the fragment to a file. Call this once a chunk is fully assembled, since a fragment's position
 * depends on whatever precedes it.
 */
internal fun atFileOffset(chunk: ByteArray, fileOffset: Long = 0): ByteArray {
    val positioned = chunk.copyOf()
    var at = 0
    while (at + BOX_HEADER_SIZE <= positioned.size) {
        val size = ByteBuffer.wrap(positioned, at, 4).int
        if (String(positioned, at + 4, 4, Charsets.US_ASCII) == "moof") {
            forEachTrackHeader(positioned, at, at + size) { header ->
                ByteBuffer.wrap(positioned).putLong(header + BOX_HEADER_SIZE + 8, fileOffset + at)
            }
        }
        at += size
    }
    return positioned
}

private fun forEachTrackHeader(chunk: ByteArray, start: Int, end: Int, action: (Int) -> Unit) {
    var at = start + BOX_HEADER_SIZE
    while (at + BOX_HEADER_SIZE <= end) {
        val size = ByteBuffer.wrap(chunk, at, 4).int
        if (String(chunk, at + 4, 4, Charsets.US_ASCII) == "traf") {
            var inner = at + BOX_HEADER_SIZE
            while (inner + BOX_HEADER_SIZE <= at + size) {
                val innerSize = ByteBuffer.wrap(chunk, inner, 4).int
                if (String(chunk, inner + 4, 4, Charsets.US_ASCII) == "tfhd") action(inner)
                inner += innerSize
            }
        }
        at += size
    }
}

/**
 * Finds boxes by type at any depth, so that a rewritten chunk can be inspected without knowing the
 * size of the boxes that contain them.
 */
internal class Mp4Reader(private val chunk: ByteArray) {

    class Box(val offset: Int, val size: Int, private val chunk: ByteArray) {
        val contentOffset get() = offset + BOX_HEADER_SIZE
        val endOffset get() = offset + size
        val flags get() = readInt(contentOffset) and 0xFFFFFF
        val decodeTime get() = ByteBuffer.wrap(chunk, contentOffset + 4, 8).long
        val dataOffset get() = readInt(contentOffset + 8)
        private fun readInt(index: Int) = ByteBuffer.wrap(chunk, index, 4).int
    }

    fun box(type: String): Box = requireNotNull(find(type, 0, chunk.size)) { "No $type box in chunk" }

    fun boxes(type: String): List<Box> = buildList {
        var from = 0
        while (true) {
            val next = find(type, from, chunk.size) ?: break
            add(next)
            from = next.endOffset
        }
    }

    fun bytes(box: Box): ByteArray = chunk.copyOfRange(box.offset, box.endOffset)

    fun brands(): List<String> {
        val fileType = box("ftyp")
        val start = fileType.contentOffset + 8 // after the major brand and minor version
        return (start until fileType.endOffset step 4).map { String(chunk, it, 4, Charsets.US_ASCII) }
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
