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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import com.amplifyframework.ui.liveness.util.rotationDegrees

@SuppressLint("ViewConstructor", "Recycle")
internal class PreviewTextureView(
    context: Context,
    renderer: OpenGLRenderer
) : TextureView(context) {

    private var surface: Surface? = null

    init {
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surface = Surface(surfaceTexture).also {
                    renderer.attachPreviewSurface(
                        it,
                        Size(width, height),
                        this@PreviewTextureView.display.rotationDegrees()
                    )
                }
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surface = Surface(surfaceTexture).also {
                    renderer.attachPreviewSurface(
                        it,
                        Size(width, height),
                        display.rotationDegrees()
                    )
                }
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                val surfaceToDestroy = surface
                surface = null
                renderer.detachPreviewSurface().addListener({
                    surfaceToDestroy?.release()
                    surfaceTexture.release()
                }, ContextCompat.getMainExecutor(context))
                return false
            }
        }
    }
}
