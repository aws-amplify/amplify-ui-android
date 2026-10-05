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

import com.amplifyframework.core.Amplify
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Rewrites each movie fragment so that it can be read without the chunks that came before it.
 *
 * The fragments we are given describe themselves in terms of the whole file: the track fragment
 * header locates sample data by an absolute file position, and no fragment records the time it
 * begins at. A reader handed one chunk on its own therefore cannot find that chunk's sample data,
 * and cannot place it on the timeline without having counted every earlier fragment.
 *
 * This replaces the absolute position with one relative to the enclosing fragment, and adds the
 * fragment's decode time, accumulated from the sample durations already seen. The brand that permits
 * a relative position is declared alongside it. As long as every chunk is rewritten, the result is
 * also correct when the chunks are concatenated, because the relative position resolves to the same
 * bytes and the decode time matches the duration of the fragments it follows.
 *
 * Only the layout the muxer actually writes is rewritten. Anything else is rejected so that it takes
 * the fallback rather than being rewritten into something wrong, which means a fragment this does not
 * recognise stops the rewriting for the rest of the file. Every fragment in a file has the same
 * layout, so a layout that is rejected is rejected from the first chunk, before any chunk has been
 * rewritten.
 *
 * Instances are stateful and must be used for a single output file, from one thread.
 */
internal class Mp4FragmentRewriter {

    private val logger = Amplify.Logging.forNamespace("Liveness")

    private var decodeTime = 0L
    private var enabled = true

    /**
     * @param chunk the bytes to rewrite.
     * @param fileOffset where the chunk begins in the file it was read from, which is what the
     *   positions a fragment holds are measured from.
     */
    fun rewrite(chunk: ByteArray, fileOffset: Long): ByteArray {
        if (!enabled) return chunk

        return try {
            rewriteBoxes(chunk, fileOffset)
        } catch (e: Exception) {
            /*
            A reader that takes one chunk at a time is no worse off for a chunk left alone than it
            would be for a chunk whose decode time does not follow on from the last one. A reader that
            joins the chunks is worse off either way, because the chunks already rewritten have grown
            and an absolute position no longer reaches the samples it names.
             */
            logger.error("Unable to rewrite movie fragment, sending remaining chunks unchanged", e)
            enabled = false
            chunk
        }
    }

    private fun rewriteBoxes(chunk: ByteArray, fileOffset: Long): ByteArray {
        val output = ByteArrayOutputStream(chunk.size + CHUNK_GROWTH)
        forEachBox(chunk, 0, chunk.size) { box ->
            when (box.type) {
                TYPE_MOOF -> output.write(rewriteFragment(chunk, box, fileOffset + box.offset))
                TYPE_FTYP -> output.write(rewriteFileType(chunk, box))
                else -> output.write(chunk, box.offset, box.size)
            }
        }
        return output.toByteArray()
    }

    /**
     * Declares the brand under which a fragment is allowed to locate its sample data relative to
     * itself. Without it a reader is entitled to read the file as an earlier revision, in which the
     * flag that asks for a relative position has no meaning.
     */
    private fun rewriteFileType(chunk: ByteArray, fileType: Box): ByteArray {
        val brandsOffset = fileType.contentOffset + FTYP_BRANDS_OFFSET
        val brandsSize = fileType.endOffset - brandsOffset
        check(brandsSize >= 0 && brandsSize % BRAND_SIZE == 0) { "File type box holds a partial brand" }

        val brands = (0 until brandsSize / BRAND_SIZE).map {
            String(chunk, brandsOffset + it * BRAND_SIZE, BRAND_SIZE, Charsets.US_ASCII)
        }
        if (BRAND_FRAGMENT_RELATIVE_OFFSETS in brands) {
            return chunk.copyOfRange(fileType.offset, fileType.endOffset)
        }

        val size = fileType.size + BRAND_SIZE
        return ByteBuffer.allocate(size).apply {
            putInt(size)
            putType(TYPE_FTYP)
            put(chunk, fileType.contentOffset, fileType.size - BOX_HEADER_SIZE)
            putType(BRAND_FRAGMENT_RELATIVE_OFFSETS)
        }.array()
    }

    /**
     * @param position where the fragment begins in the file, which is what it measures the position of
     *   its sample data from.
     */
    private fun rewriteFragment(chunk: ByteArray, fragment: Box, position: Long): ByteArray {
        var header: ByteArray? = null
        val tracks = mutableListOf<Box>()

        forEachBox(chunk, fragment.contentOffset, fragment.endOffset) { box ->
            when (box.type) {
                TYPE_MFHD -> header = chunk.copyOfRange(box.offset, box.endOffset)
                TYPE_TRAF -> tracks.add(box)
                else -> error("Fragment holds an unexpected ${box.type} box")
            }
        }

        val fragmentHeader = checkNotNull(header) { "Fragment is missing its header" }

        /*
        A decode time belongs to one track and is counted in that track's own timescale, so a
        fragment covering more than one track needs a decode time for each of them.
         */
        check(tracks.size == 1) { "Fragment covers ${tracks.size} tracks" }

        val rewritten = rewriteTrack(chunk, tracks.single(), fragment, position)

        val contentSize = fragmentHeader.size + rewritten.bytes.size
        val bytes = ByteBuffer.allocate(BOX_HEADER_SIZE + contentSize).apply {
            putInt(BOX_HEADER_SIZE + contentSize)
            putType(TYPE_MOOF)
            put(fragmentHeader)
            put(rewritten.bytes)
        }.array()

        decodeTime += rewritten.duration

        return bytes
    }

    private fun rewriteTrack(chunk: ByteArray, track: Box, fragment: Box, position: Long): RewrittenTrack {
        var header: Box? = null
        var run: Box? = null

        forEachBox(chunk, track.contentOffset, track.endOffset) { box ->
            when (box.type) {
                TYPE_TFHD -> header = box
                TYPE_TRUN -> {
                    check(run == null) { "Track fragment holds more than one run of samples" }
                    run = box
                }
                TYPE_TFDT -> error("Track fragment already states its decode time")
                else -> error("Track fragment holds an unexpected ${box.type} box")
            }
        }

        val trackHeader = checkNotNull(header) { "Track fragment is missing its header" }
        val trackRun = checkNotNull(run) { "Track fragment contains no samples" }

        val headerFlags = chunk.readInt(trackHeader.contentOffset) and FLAG_MASK
        check(headerFlags == TFHD_FLAG_BASE_DATA_OFFSET) {
            "Track fragment header holds more than an absolute position: $headerFlags"
        }
        // Holding nothing but an absolute position is what makes the growth of the rewrite a constant
        check(trackHeader.size == TFHD_BOX_SIZE + BASE_DATA_OFFSET_SIZE) {
            "Track fragment header is ${trackHeader.size} bytes"
        }
        val trackId = chunk.readInt(trackHeader.contentOffset + Int.SIZE_BYTES)

        /*
        Dropping the absolute position only leaves the sample data where it was if the position being
        dropped is the one the fragment starts at, since that is what the rewrite measures from.
         */
        val baseDataOffset = chunk.readLong(trackHeader.contentOffset + 2 * Int.SIZE_BYTES)
        check(baseDataOffset == position) {
            "Sample data is measured from $baseDataOffset rather than from the fragment at $position"
        }

        val samples = readSamples(chunk, trackRun)

        /*
        Moving the sample data further from the start of the fragment is only correct if it follows the
        fragment, rather than sitting before it.
         */
        check(samples.dataOffset >= fragment.size) {
            "Sample data at ${samples.dataOffset} is not beyond the ${fragment.size} byte fragment"
        }

        val content = ByteBuffer.allocate(TFHD_BOX_SIZE + TFDT_BOX_SIZE + trackRun.size).apply {
            // Locate the sample data relative to the enclosing fragment instead of the whole file
            putInt(TFHD_BOX_SIZE)
            putType(TYPE_TFHD)
            putInt(TFHD_FLAG_DEFAULT_BASE_IS_MOOF)
            putInt(trackId)

            putInt(TFDT_BOX_SIZE)
            putType(TYPE_TFDT)
            putInt(TFDT_VERSION_64_BIT shl VERSION_SHIFT)
            putLong(decodeTime)

            put(chunk, trackRun.offset, trackRun.size)

            // The sample data has moved further from the fragment, so its recorded offset moves too
            putInt(TFHD_BOX_SIZE + TFDT_BOX_SIZE + TRUN_DATA_OFFSET, samples.dataOffset + TRACK_GROWTH)
        }.array()

        val bytes = ByteBuffer.allocate(BOX_HEADER_SIZE + content.size).apply {
            putInt(BOX_HEADER_SIZE + content.size)
            putType(TYPE_TRAF)
            put(content)
        }.array()

        return RewrittenTrack(bytes = bytes, duration = samples.duration)
    }

    private fun readSamples(chunk: ByteArray, run: Box): Samples {
        val flags = chunk.readInt(run.contentOffset) and FLAG_MASK
        check(flags and TRUN_FLAG_DATA_OFFSET != 0) { "Samples do not record their position" }
        check(flags and TRUN_FLAG_SAMPLE_DURATION != 0) { "Samples do not record their duration" }
        /*
        Flags held for the first sample alone sit between the position and the samples, which would
        move every field that follows them.
         */
        check(flags and TRUN_FLAG_FIRST_SAMPLE_FLAGS == 0) { "Samples single out the first of them" }

        val count = chunk.readInt(run.contentOffset + Int.SIZE_BYTES)
        val dataOffset = chunk.readInt(run.offset + TRUN_DATA_OFFSET)

        val bytesPerSample = TRUN_SAMPLE_FIELDS.count { flags and it != 0 } * Int.SIZE_BYTES
        val firstSample = run.offset + TRUN_DATA_OFFSET + Int.SIZE_BYTES
        // An exact fit is what shows that the fields counted from the flags are the fields written
        check(count >= 0 && firstSample + count.toLong() * bytesPerSample == run.endOffset.toLong()) {
            "$count samples of $bytesPerSample bytes do not fill their box"
        }

        var duration = 0L
        for (sample in 0 until count) {
            // The duration is the first field of each sample whenever it is present
            duration += chunk.readInt(firstSample + sample * bytesPerSample).toUInt().toLong()
        }

        return Samples(dataOffset = dataOffset, duration = duration)
    }

    private fun forEachBox(chunk: ByteArray, start: Int, end: Int, action: (Box) -> Unit) {
        var position = start
        while (position + BOX_HEADER_SIZE <= end) {
            val declaredSize = chunk.readInt(position).toUInt().toLong()
            val type = String(chunk, position + Int.SIZE_BYTES, TYPE_SIZE, Charsets.US_ASCII)

            val size = if (declaredSize == EXTENDED_SIZE) {
                check(position + EXTENDED_SIZE_HEADER_SIZE <= end) { "Truncated $type box" }
                chunk.readLong(position + BOX_HEADER_SIZE)
            } else {
                declaredSize
            }
            check(size >= BOX_HEADER_SIZE && position + size <= end) { "Truncated $type box" }

            action(Box(offset = position, size = size.toInt(), type = type))
            position += size.toInt()
        }
        check(position == end) { "Found ${end - position} bytes after the last box" }
    }

    private fun ByteArray.readInt(index: Int) = ByteBuffer.wrap(this, index, Int.SIZE_BYTES).int

    private fun ByteArray.readLong(index: Int) = ByteBuffer.wrap(this, index, Long.SIZE_BYTES).long

    private fun ByteBuffer.putType(type: String): ByteBuffer = put(type.toByteArray(Charsets.US_ASCII))

    private class Box(val offset: Int, val size: Int, val type: String) {
        val contentOffset get() = offset + BOX_HEADER_SIZE
        val endOffset get() = offset + size
    }

    private class RewrittenTrack(val bytes: ByteArray, val duration: Long)

    private class Samples(val dataOffset: Int, val duration: Long)

    private companion object {
        const val TYPE_FTYP = "ftyp"
        const val TYPE_MOOF = "moof"
        const val TYPE_MFHD = "mfhd"
        const val TYPE_TRAF = "traf"
        const val TYPE_TFHD = "tfhd"
        const val TYPE_TFDT = "tfdt"
        const val TYPE_TRUN = "trun"

        const val TYPE_SIZE = 4
        const val BOX_HEADER_SIZE = 8
        const val EXTENDED_SIZE_HEADER_SIZE = 16

        // A declared size of one means the real size follows the box type as a 64 bit value
        const val EXTENDED_SIZE = 1L

        const val FLAG_MASK = 0xFFFFFF
        const val VERSION_SHIFT = 24

        // A file type box records a major brand and a minor version before its compatible brands
        const val FTYP_BRANDS_OFFSET = 2 * Int.SIZE_BYTES
        const val BRAND_SIZE = 4
        const val BRAND_FRAGMENT_RELATIVE_OFFSETS = "iso5"

        const val TFHD_BOX_SIZE = 16
        const val TFHD_FLAG_BASE_DATA_OFFSET = 0x000001
        const val TFHD_FLAG_DEFAULT_BASE_IS_MOOF = 0x020000
        const val BASE_DATA_OFFSET_SIZE = Long.SIZE_BYTES

        const val TFDT_BOX_SIZE = 20
        const val TFDT_VERSION_64_BIT = 1

        // A track run records its version and flags, then the sample count, then the data offset
        const val TRUN_DATA_OFFSET = BOX_HEADER_SIZE + 2 * Int.SIZE_BYTES
        const val TRUN_FLAG_DATA_OFFSET = 0x000001
        const val TRUN_FLAG_FIRST_SAMPLE_FLAGS = 0x000004
        const val TRUN_FLAG_SAMPLE_DURATION = 0x000100

        /*
        What a track fragment grows by: the decode time it gains less the absolute position it loses.
        The sample data moves away from the start of its fragment by the same amount.
         */
        const val TRACK_GROWTH = TFDT_BOX_SIZE - BASE_DATA_OFFSET_SIZE

        // What a chunk holding one fragment grows by, counting the brand added to its file type box
        const val CHUNK_GROWTH = BRAND_SIZE + TRACK_GROWTH

        /**
         * The per sample fields of a track run, in the order they are written, so that the number of
         * fields present can be counted from the flags.
         */
        val TRUN_SAMPLE_FIELDS = intArrayOf(
            TRUN_FLAG_SAMPLE_DURATION,
            0x000200, // sample size
            0x000400, // sample flags
            0x000800 // sample composition time offset
        )
    }
}
