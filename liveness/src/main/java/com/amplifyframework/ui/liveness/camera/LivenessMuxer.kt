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
import android.media.MediaFormat
import androidx.annotation.WorkerThread
import com.amplifyframework.annotations.InternalAmplifyApi
import java.nio.ByteBuffer

@InternalAmplifyApi
typealias SendMuxedSegmentHandler = (bytes: ByteArray, timestamp: Long) -> Unit

@InternalAmplifyApi
sealed interface LivenessMuxer {

    @WorkerThread
    fun start(
        context: Context,
        mediaFormat: MediaFormat,
        sendMuxedSegmentHandler: SendMuxedSegmentHandler
    )

    @WorkerThread
    fun write(byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    @WorkerThread
    fun stop()
}

@InternalAmplifyApi
interface LivenessFragmentedMp4Muxer : LivenessMuxer

@InternalAmplifyApi
interface LivenessWebMMuxer: LivenessMuxer