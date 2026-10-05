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

import com.amplifyframework.ui.liveness.testUtil.Mp4Reader
import com.amplifyframework.ui.liveness.testUtil.box
import com.amplifyframework.ui.liveness.testUtil.chunkOf
import com.amplifyframework.ui.liveness.testUtil.fileType
import com.amplifyframework.ui.liveness.testUtil.movieFragment
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class Mp4FragmentRewriterTest {

    private val rewriter = Mp4FragmentRewriter()

    @Test
    fun `locates sample data relative to the fragment`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100, 100, 100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.box("tfhd").flags shouldBe DEFAULT_BASE_IS_MOOF
        val fragment = rewritten.box("moof")
        fragment.offset + rewritten.box("trun").dataOffset shouldBe rewritten.box("mdat").contentOffset
    }

    @Test
    fun `states the decode time of the first fragment as zero`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100, 100, 100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.box("tfdt").decodeTime shouldBe 0
    }

    @Test
    fun `accumulates the decode time across fragments`() {
        val chunks = listOf(300, 400, 500).map { chunkOf(movieFragment(sampleDurations = listOf(it, it))) }

        val decodeTimes = chunks.map { Mp4Reader(rewriter.rewrite(it)).box("tfdt").decodeTime }

        decodeTimes shouldContainExactly listOf(0L, 600L, 1400L)
    }

    @Test
    fun `locates sample data of every fragment in a chunk holding more than one`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100))) +
            chunkOf(movieFragment(sampleDurations = listOf(200)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        val fragments = rewritten.boxes("moof")
        val samples = rewritten.boxes("trun")
        val payloads = rewritten.boxes("mdat")
        fragments.indices.map { fragments[it].offset + samples[it].dataOffset } shouldContainExactly
            payloads.map { it.contentOffset }
    }

    @Test
    fun `leaves the sample data untouched`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100, 100)))
        val original = Mp4Reader(chunk).let { it.bytes(it.box("mdat")) }

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.bytes(rewritten.box("mdat")).toList() shouldContainExactly original.toList()
    }

    @Test
    fun `copies boxes it does not need to change`() {
        val movie = box("moov", ByteArray(12) { it.toByte() })
        val chunk = movie + chunkOf(movieFragment(sampleDurations = listOf(100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.bytes(rewritten.box("moov")).toList() shouldContainExactly movie.toList()
    }

    @Test
    fun `declares the brand that permits fragment relative offsets`() {
        val chunk = fileType("isom", "isom", "iso2", "mp41") +
            chunkOf(movieFragment(sampleDurations = listOf(100)))

        val rewritten = Mp4Reader(rewriter.rewrite(chunk))

        rewritten.brands() shouldContainExactly listOf("isom", "iso2", "mp41", "iso5")
    }

    @Test
    fun `does not repeat a brand that is already declared`() {
        val chunk = fileType("isom", "isom", "iso5") + chunkOf(movieFragment(sampleDurations = listOf(100)))

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
        val readable = chunkOf(movieFragment(sampleDurations = listOf(100)))

        rewriter.rewrite(ByteArray(40) { 1 })

        rewriter.rewrite(readable).toList() shouldContainExactly readable.toList()
    }

    @Test
    fun `sends a fragment holding an unexpected box unchanged`() {
        val chunk = chunkOf(
            movieFragment(
                sampleDurations = listOf(100),
                extraFragmentBoxes = listOf(box("free", ByteArray(4)))
            )
        )

        rewriter.rewrite(chunk).toList() shouldContainExactly chunk.toList()
    }

    @Test
    fun `sends a track fragment holding an unexpected box unchanged`() {
        val chunk = chunkOf(
            movieFragment(
                sampleDurations = listOf(100),
                extraTrackBoxes = listOf(box("sdtp", ByteArray(4)))
            )
        )

        rewriter.rewrite(chunk).toList() shouldContainExactly chunk.toList()
    }

    @Test
    fun `sends a fragment covering more than one track unchanged`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100), tracks = 2))

        rewriter.rewrite(chunk).toList() shouldContainExactly chunk.toList()
    }

    @Test
    fun `sends a track fragment holding more than one run of samples unchanged`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100), runsPerTrack = 2))

        rewriter.rewrite(chunk).toList() shouldContainExactly chunk.toList()
    }

    @Test
    fun `sends samples that single out the first of them unchanged`() {
        val chunk = chunkOf(movieFragment(sampleDurations = listOf(100, 100), firstSampleFlags = true))

        rewriter.rewrite(chunk).toList() shouldContainExactly chunk.toList()
    }

    private companion object {
        const val DEFAULT_BASE_IS_MOOF = 0x020000
    }
}
