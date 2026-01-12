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

import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Muxer
import io.kotest.matchers.ints.shouldBeLessThan
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * A fake muxer for testing that simply writes all samples directly to the outputStream
 */
class TestMuxer(private val outputStream: OutputStream) : Muxer {
    private var tracks = 0

    var closed: Boolean = false

    override fun addTrack(format: Format): Int = tracks++

    override fun writeSampleData(trackId: Int, byteBuffer: ByteBuffer, bufferInfo: BufferInfo) {
        trackId shouldBeLessThan tracks
        if (!closed) {
            outputStream.write(byteBuffer.toByteArray())
        }
    }

    override fun addMetadataEntry(metadataEntry: Metadata.Entry) {
        // no-op
    }

    override fun close() {
        closed = true
    }

    fun ByteBuffer.toByteArray(): ByteArray {
        val bytes = ByteArray(remaining())
        get(bytes)
        return bytes
    }
}
