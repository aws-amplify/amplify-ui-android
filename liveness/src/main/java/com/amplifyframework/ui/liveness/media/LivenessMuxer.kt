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
import android.media.MediaMuxer
import androidx.annotation.WorkerThread
import androidx.media3.common.util.MediaFormatUtil
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.FragmentedMp4Muxer
import androidx.media3.muxer.Muxer
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.liveness.camera.OnMuxedSegment
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

internal interface LivenessMuxer {
    @WorkerThread
    fun start(outputFile: File, mediaFormat: MediaFormat, onMuxedSegment: OnMuxedSegment)

    @WorkerThread
    fun write(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    @WorkerThread
    fun stop()

    companion object {
        fun create(format: VideoCodec): LivenessMuxer = when (format) {
            VideoCodec.H264 -> Mp4Muxer()
            VideoCodec.VP8, VideoCodec.VP9 -> WebMMuxer()
        }
    }
}

internal class WebMMuxer : LivenessMuxer {
    private val logger = Amplify.Logging.forNamespace("Liveness")

    private var muxer: MediaMuxer? = null // set when muxer is started
    private var videoTrack: Int = -1 // set when muxer is started
    private var currentVideoStartTime = 0L // set at the start of each chunk
    private var currentBytePosition = 0L // random access file position
    private var lastChunkNotificationTimestamp = 0L // start at 0 to be ready to notify

    private var tempOutputFile: File? = null
    private var muxerRandomAccessFile: RandomAccessFile? = null
    private var sendMuxedSegment: OnMuxedSegment? = null

    override fun start(outputFile: File, mediaFormat: MediaFormat, onMuxedSegment: OnMuxedSegment) {
        tempOutputFile = outputFile
        muxerRandomAccessFile = RandomAccessFile(tempOutputFile, "r")
        sendMuxedSegment = onMuxedSegment

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

    override fun stop() {
        try {
            muxer?.stop()
            muxer?.release()
            muxer = null
        } catch (_: Exception) {
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
                } catch (_: Exception) {
                    // failed to access muxer file
                    false
                }
            } ?: false
        } catch (_: Exception) {
            // process possibly stopped
            false
        }
    }

    companion object {
        const val MIN_CHUNK_DELAY_MILLIS = 100L // Minimum time between chunk notifications
    }
}

internal class Mp4Muxer(private val createMediaMuxer: (outputStream: FileOutputStream) -> Muxer = ::createMediaMuxer) :
    LivenessMuxer {

    private val logger = Amplify.Logging.forNamespace("Liveness")

    private var muxer: Muxer? = null
    private var videoTrackToken: Int? = null
    private var firstKeyframeReceived = false
    private var currentVideoStartTime = 0L // set at the start of each chunk
    private var currentBytePosition = 0L // random access file position

    private var tempOutputFile: File? = null
    private var muxerRandomAccessFile: RandomAccessFile? = null
    private var sendMuxedSegment: OnMuxedSegment? = null

    override fun start(outputFile: File, mediaFormat: MediaFormat, onMuxedSegment: OnMuxedSegment) {
        this.tempOutputFile = outputFile
        muxerRandomAccessFile = RandomAccessFile(outputFile, "r")
        sendMuxedSegment = onMuxedSegment

        muxer = createMediaMuxer(outputFile.outputStream()).apply {
            videoTrackToken = addTrack(MediaFormatUtil.createFormatFromMediaFormat(mediaFormat))
            currentVideoStartTime = System.currentTimeMillis()
        }
    }

    /*
    Write new frame to muxer and attempt to notify listener of new chunk available
     */
    override fun write(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        try {
            val muxer = muxer ?: throw IllegalStateException("Muxer not initialized")
            val trackId = videoTrackToken ?: throw IllegalStateException("Video track not initialized")
            muxer.writeSampleData(trackId, byteBuf, bufferInfo.toMedia3())
        } catch (e: Exception) {
            // writeSampleData can throw for various reasons, such as an empty byte buffer.
            // If this happens, we discard the frame, in hopes that future frames are valid.
            logger.error("Failed to write encoded chunk to muxer", e)
        }

        // Track first keyframe received and return after. We don't want to send chunk with a single frame
        if (bufferInfo.isKeyFrame()) {
            if (!firstKeyframeReceived) {
                firstKeyframeReceived = true
            } else if (notifyChunk()) {
                // The mp4 muxer creates a segment for the previous chunk on each keyframe receipt
                currentVideoStartTime = System.currentTimeMillis()
            }
        }
    }

    override fun stop() {
        try {
            muxer?.close()
            muxer = null
        } catch (_: Exception) {
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
            } ?: false
        } catch (e: Exception) {
            logger.warn("Unable to send muxed segment", e)
            false
        }
    }

    private fun MediaCodec.BufferInfo.isKeyFrame() = flags.and(MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

    private fun MediaCodec.BufferInfo.toMedia3() = BufferInfo(
        presentationTimeUs,
        size,
        flags
    )

    companion object {
        private fun createMediaMuxer(outputStream: FileOutputStream): Muxer =
            FragmentedMp4Muxer.Builder(outputStream.channel)
                /*
                Segments aren't too heavy in size. Would rather have more segments than delays in sending data.
                Seeing no data available on some notifyChunk() flushes when duration matches keyframe interval
                I believe segments are only created after keyframes so setting this number low may have minimal impact.
                Liveness library currently has 1 second keyframes, so 500ms if half
                 */
                .setFragmentDurationMs(500)
                .build()
    }
}
