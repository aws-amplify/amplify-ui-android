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

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.WorkerThread
import com.amplifyframework.ui.liveness.util.isKeyFrame
import java.io.File

internal class LivenessVideoEncoder private constructor(
    width: Int,
    height: Int,
    bitrate: Int,
    private val frameRate: Int,
    private val keyframeInterval: Int,
    private val outputFile: File,
    private val onMuxedSegment: OnMuxedSegment
) {

    companion object {

        const val TAG = "LivenessVideoEncoder"
        const val LOGGING_ENABLED = false
        const val MIME_TYPE = "video/x-vnd.on2.vp8"

        fun create(
            context: Context,
            width: Int,
            height: Int,
            bitrate: Int,
            framerate: Int,
            keyframeInterval: Int,
            onMuxedSegment: OnMuxedSegment
        ): LivenessVideoEncoder? {
            return try {
                LivenessVideoEncoder(
                    width,
                    height,
                    bitrate,
                    framerate,
                    keyframeInterval,
                    createTempOutputFile(context),
                    onMuxedSegment
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun createTempOutputFile(context: Context) = File(
            File(
                context.cacheDir,
                "amplify_liveness_temp"
            ).apply {
                if (exists()) {
                    deleteRecursively()
                }

                if (!exists()) {
                    mkdir()
                }
            },
            "${System.currentTimeMillis()}"
        )
    }

    private val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
        setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyframeInterval)
    }

    private val encoderHandler = Handler(HandlerThread(TAG).apply { start() }.looper)

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE).apply {
        configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        setCallback(
            object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    handleFrame(index, info)
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                }
            },
            encoderHandler
        )
    }
    val inputSurface = encoder.createInputSurface()

    private var encoding = false
    private var livenessMuxer: LivenessMuxer? = null

    init {
        encoder.start()
    }

    /*
    Older versions of Android do not request KEY_I_FRAME_INTERVAL with WebM.
    We must manually request a new keyframe at a desired interval
     */
    var framesSinceSyncRequest = 0

    @WorkerThread

    fun handleFrame(outputBufferId: Int, info: MediaCodec.BufferInfo) {
        try {
            encoder.getOutputBuffer(outputBufferId)?.let { byteBuffer ->
                if (encoding) {
                    if (LOGGING_ENABLED) {
                        Log.d(
                            TAG,
                            "current time: ${System.currentTimeMillis()}, " +
                                "presentation time: ${info.presentationTimeUs}, " +
                                "isKeyFrame: ${info.isKeyFrame()}"
                        )
                    }

                    if (info.isKeyFrame()) {
                        if (livenessMuxer == null) {
                            livenessMuxer = LivenessMuxer(
                                outputFile,
                                encoder.outputFormat,
                                onMuxedSegment
                            )
                        }
                        framesSinceSyncRequest = 0 // reset keyframe request on keyframe receipt
                    } else {
                        framesSinceSyncRequest += 1

                        /*
                        Older versions of Android do not request KEY_I_FRAME_INTERVAL with WebM.
                        We manually request a new keyframe when we have processed the expected
                        number of frames before our next expected keyframe.
                         */
                        if (framesSinceSyncRequest >= (frameRate * keyframeInterval)) {
                            encoder.setParameters(
                                Bundle().apply {
                                    putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                                }
                            )
                            framesSinceSyncRequest = 0 // reset keyframe request
                        }
                    }
                    livenessMuxer?.write(byteBuffer, info)
                }
            }
            encoder.releaseOutputBuffer(outputBufferId, false)
        } catch (e: IllegalStateException) {
            // may have already been released by stop()
        }
    }

    fun start() {
        encoderHandler.post {
            if (!encoding) {
                if (LOGGING_ENABLED) {
                    Log.d(TAG, "Starting to encode")
                }
                encoding = true
                encoder.setParameters(
                    Bundle().apply {
                        putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                    }
                )
            } else {
                Log.e(TAG, "Encoding already started")
            }
        }
    }

    fun stop(onComplete: () -> Unit) {
        encoderHandler.post {
            encoding = false
            livenessMuxer?.stop()
            livenessMuxer = null
            if (LOGGING_ENABLED) {
                Log.i(TAG, "Stopping encoder")
            }
            onComplete()
        }
    }

    fun destroy() {
        encoderHandler.post {
            if (LOGGING_ENABLED) {
                Log.i(TAG, "Destroying encoder")
            }
            try {
                livenessMuxer?.stop()
                livenessMuxer = null
            } catch (e: Exception) {
                // muxer likely already stopped
            }
            try {
                encoder.stop()
            } catch (e: Exception) {
                // may already be stopped
            }
            encoder.release()
        }
    }
}
