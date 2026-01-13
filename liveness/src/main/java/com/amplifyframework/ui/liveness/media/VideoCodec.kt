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

import android.media.MediaFormat

/**
 * An enumeration of the supported video formats in Liveness.
 */
sealed interface VideoCodec {
    data object VP8 : VideoCodec

    data object VP9 : VideoCodec

    data object H264 : VideoCodec
}

internal val VideoCodec.mimeType: String
    get() = when (this) {
        VideoCodec.H264 -> MediaFormat.MIMETYPE_VIDEO_AVC
        VideoCodec.VP8 -> MediaFormat.MIMETYPE_VIDEO_VP8
        VideoCodec.VP9 -> MediaFormat.MIMETYPE_VIDEO_VP9
    }
