/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.annotation.WorkerThread
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Amplify
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

@WorkerThread
@InternalAmplifyApi
class LivenessWebMMuxerImpl: LivenessWebMMuxer {

    private val logger = Amplify.Logging.forNamespace("Liveness")

    private var muxer: MediaMuxer? = null // set when muxer is started
    private var videoTrack: Int = -1 // set when muxer is started
    private var currentVideoStartTime = 0L // set at the start of each chunk
    private var currentBytePosition = 0L // random access file position
    private var lastChunkNotificationTimestamp = 0L // start at 0 to be ready to notify

    private var tempOutputFile: File? = null
    private var muxerRandomAccessFile: RandomAccessFile? = null
    private var sendMuxedSegment: SendMuxedSegmentHandler? = null

    override fun start(
        context: Context,
        mediaFormat: MediaFormat,
        sendMuxedSegmentHandler: SendMuxedSegmentHandler
    ) {
        tempOutputFile = createTempOutputFile(context)
        muxerRandomAccessFile = RandomAccessFile(tempOutputFile, "r")
        sendMuxedSegment = sendMuxedSegmentHandler

        muxer = MediaMuxer(
            tempOutputFile.toString(),
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        ).apply {
            videoTrack = addTrack(mediaFormat)
            start()
            currentVideoStartTime = System.currentTimeMillis()
        }
    }

    /*
    Attempt to notify listener that chunked data is available if minimum chunk interval has exceeded
    Write new frame to muxer
     */
    @WorkerThread
    override fun write(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (System.currentTimeMillis() - lastChunkNotificationTimestamp >= MIN_CHUNK_DELAY_MILLIS) {
            if (notifyChunk()) {
                lastChunkNotificationTimestamp = System.currentTimeMillis()
                currentVideoStartTime = System.currentTimeMillis()
            }
        }

        try {
            muxer?.writeSampleData(videoTrack, byteBuf, bufferInfo) ?: throw IllegalStateException("Muxer not started")
        } catch (e: Exception) {
            // writeSampleData can throw for various reasons, such as an empty byte buffer.
            // If this happens, we discard the frame, in hopes that future frames are valid.
            logger.error("Failed to write encoded chunk to muxer", e)
        }
    }

    @WorkerThread
    override fun stop() {
        try {
            muxer?.stop()
            muxer?.release()
            muxer = null
        } catch (e: Exception) {
            // don't crash if muxer encounters internal error
        }

        // send partial chunk
        notifyChunk()
        muxerRandomAccessFile?.close()
        tempOutputFile?.delete()
    }

    /**
     * We are sending the muxed output file in chunks. Each time this method is called,
     * we attempt to get the new bytes of the file we have not yet provided to the callback.
     * Once we provide the new bytes, we update the current byte position so the next update
     * only sends new byte data.
     * @return true if chunk notified
     */
    private fun notifyChunk(): Boolean {
        return try {
            muxerRandomAccessFile?.let { raf ->
                try {
                    val sizeToRead = raf.length() - currentBytePosition

                    // don't attempt to send chunk if no update available,
                    // or if the first chunk hasn't accumulated enough data
                    if (sizeToRead <= 0 || (currentBytePosition == 0L && sizeToRead < 10_000)) {
                        return false
                    }

                    val chunkByteArray = ByteArray(sizeToRead.toInt())
                    raf.seek(currentBytePosition)
                    raf.read(chunkByteArray)
                    currentBytePosition += sizeToRead

                    val sendMuxedSegmentHandler = sendMuxedSegment
                    if (sendMuxedSegmentHandler != null) {
                        sendMuxedSegmentHandler(chunkByteArray, currentVideoStartTime)
                        true
                    } else {
                        logger.error("sendMuxedSegmentHandler unexpectedly null")
                        return false
                    }
                } catch (e: Exception) {
                    // failed to access muxer file
                    false
                }
            } ?: false
        } catch (e: Exception) {
            // process possibly stopped
            false
        }
    }

    private fun createTempOutputFile(context: Context): File {
        return File(
            File(context.cacheDir, "amplify_liveness_temp").apply {
                if (exists()) {
                    deleteRecursively()
                }

                if (!exists()) {
                    mkdir()
                }
            },
            "${System.currentTimeMillis()}"
        ).apply {
            createNewFile()
        }
    }

    companion object {
        const val MIN_CHUNK_DELAY_MILLIS = 100L // Minimum time between chunk notifications
    }
}
