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

package com.amplifyframework.ui.sample.liveness

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.annotation.WorkerThread
import androidx.media3.common.util.MediaFormatUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.FragmentedMp4Muxer
import androidx.media3.muxer.Muxer.TrackToken
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.liveness.camera.LivenessFragmentedMp4Muxer
import com.amplifyframework.ui.liveness.camera.SendMuxedSegmentHandler
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

@OptIn(InternalAmplifyApi::class)
@UnstableApi
@WorkerThread
internal class LivenessFragmentedMp4MuxerImpl: LivenessFragmentedMp4Muxer {

    private val logger = Amplify.Logging.forNamespace("Liveness")

    private var muxer: FragmentedMp4Muxer? = null
    private var videoTrackToken: TrackToken? = null
    private var firstKeyframeReceived = false
    private var currentVideoStartTime = 0L // set at the start of each chunk
    private var currentBytePosition = 0L // random access file position

    private var tempOutputFile: File? = null
    private var muxerRandomAccessFile: RandomAccessFile? = null
    private var sendMuxedSegment: SendMuxedSegmentHandler? = null

    override fun start(
        context: Context,
        mediaFormat: MediaFormat,
        sendMuxedSegmentHandler: SendMuxedSegmentHandler
    ) {

        val tempOutputFile = createTempOutputFile(context)
        this.tempOutputFile = tempOutputFile
        muxerRandomAccessFile = RandomAccessFile(tempOutputFile, "r")
        sendMuxedSegment = sendMuxedSegmentHandler

        muxer = FragmentedMp4Muxer.Builder(tempOutputFile.outputStream())
            /*
            Segments aren't too heavy in size. Would rather have more segments than delays in sending data.
            Seeing no data available on some notifyChunk() flushes when duration matches keyframe interval
            I believe segments are only created after keyframes so setting this number low may have minimal impact.
            Liveness library currently has 1 second keyframes, so 500ms if half
            */
            .setFragmentDurationMs(500)
            .build().apply {
                videoTrackToken = addTrack(MediaFormatUtil.createFormatFromMediaFormat(mediaFormat))
            }
    }

    /*
    Write new frame to muxer and attempt to notify listener of new chunk available
     */
    @WorkerThread
    override fun write(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        try {
            val muxer = muxer ?: throw IllegalStateException("Muxer not initialized")
            val videoTrackToken = videoTrackToken ?: throw IllegalStateException("Video track not initialized")
            muxer.writeSampleData(videoTrackToken, byteBuf, bufferInfo)
        } catch (e: Exception) {
            // writeSampleData can throw for various reasons, such as an empty byte buffer.
            // If this happens, we discard the frame, in hopes that future frames are valid.
            logger.error("Failed to write encoded chunk to muxer", e)
        }

        // Track first keyframe received and return after. We don't want to send chunk with a single frame
        if (bufferInfo.isKeyFrame()) {
            if (!firstKeyframeReceived) {
                firstKeyframeReceived = true
            } else {
                // The mp4 muxer creates a segment for the previous chunk on each keyframe receipt
                notifyChunk()
            }
            return
        }
    }

    @WorkerThread
    override fun stop() {
        try {
            muxer?.close()
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
                    if (sizeToRead <= 0 || (currentBytePosition == 0L && sizeToRead < 100)) {
                        return false
                    }

                    val chunkByteArray = ByteArray(sizeToRead.toInt())
                    raf.seek(currentBytePosition)
                    raf.read(chunkByteArray)
                    currentBytePosition += sizeToRead

                    val sendMuxedSegment = sendMuxedSegment
                    if (sendMuxedSegment != null) {
                        sendMuxedSegment(chunkByteArray, currentVideoStartTime)
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

    private fun MediaCodec.BufferInfo.isKeyFrame() =
        flags.and(MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
}
