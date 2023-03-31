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
import androidx.annotation.WorkerThread
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

@WorkerThread
internal class LivenessMuxer(
    private val tempOutputFile: File,
    outputFormat: MediaFormat,
    private val onMuxedSegment: OnMuxedSegment
) {
    private val muxerRandomAccessFile = RandomAccessFile(
        tempOutputFile.apply { createNewFile() },
        "r"
    )

    private val videoTrack: Int
    private var currentVideoStartTime: Long
    private var currentBytePosition = 0L
    private var lastChunkNotificationTimestamp = 0L // start ready to notify

    private val muxer = MediaMuxer(
        tempOutputFile.toString(),
        MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
    ).apply {
        videoTrack = addTrack(outputFormat)
        start()
        currentVideoStartTime = System.currentTimeMillis()
    }

    /*
    Attempt to notify listener that chunked data is available if minimum chunk interval has exceeded
    Write new frame to muxer
     */
    @WorkerThread
    fun write(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (System.currentTimeMillis() - lastChunkNotificationTimestamp >= MIN_CHUNK_DELAY_MILLIS) {
            if (notifyChunk()) {
                lastChunkNotificationTimestamp = System.currentTimeMillis()
                currentVideoStartTime = System.currentTimeMillis()
            }
        }

        muxer.writeSampleData(videoTrack, byteBuf, bufferInfo)
    }

    @WorkerThread
    fun stop() {
        try {
            muxer.stop()
            muxer.release()
        } catch (e: Exception) {
            // don't crash if muxer encounters internal error
        }

        // send partial chunk
        notifyChunk()
        muxerRandomAccessFile.close()
        tempOutputFile.delete()
    }

    /**
     * We are sending the muxed output file in chunks. Each time this method is called,
     * we attempt to get the new bytes of the file we have not yet provided to the callback.
     * Once we provide the new bytes, we update the current byte position so the next update
     * only sends new byte data.
     * @return true if chunk notified
     */
    private fun notifyChunk(): Boolean {
        try {
            muxerRandomAccessFile.apply {
                try {
                    val sizeToRead = length() - currentBytePosition

                    // don't attempt to send chunk if no update available,
                    // or if the first chunk hasn't accumulated enough data
                    if (sizeToRead <= 0 || (currentBytePosition == 0L && sizeToRead < 10_000)) {
                        return false
                    }

                    val chunkByteArray = ByteArray(sizeToRead.toInt())
                    seek(currentBytePosition)
                    read(chunkByteArray)
                    currentBytePosition += sizeToRead

                    onMuxedSegment(chunkByteArray, currentVideoStartTime)
                    return true
                } catch (e: Exception) {
                    // failed to access muxer file
                    return false
                }
            }
        } catch (e: Exception) {
            // process possibly stopped
            return false
        }
    }

    companion object {
        const val MIN_CHUNK_DELAY_MILLIS = 100L // Minimum time between chunk notifications
    }
}
