/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modifications from https://gist.github.com/zokipirlo/dc7ae3a6bfc1c6e6b3e369fabb50704c/0ddcec943d32c77285f87341a1b12caaaa105720
 * See: https://stackoverflow.com/a/63528509/1289034
 * See: https://groups.google.com/a/android.com/g/camerax-developers/c/pxUHvlDta54
 */

package com.amplifyframework.ui.liveness.camera;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.Preview;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

final class OpenGLRenderer {
    private static final String TAG = "OpenGLRenderer";
    private static final boolean DEBUG = false;

    static {
        System.loadLibrary("liveness_opengl_renderer_jni");
    }

    private static final AtomicInteger RENDERER_COUNT = new AtomicInteger(0);
    private final SingleThreadHandlerExecutor mExecutor =
            new SingleThreadHandlerExecutor(
                    String.format(Locale.US, "GLRenderer-%03d", RENDERER_COUNT.incrementAndGet()),
                    Process.THREAD_PRIORITY_DEFAULT); // Use UI thread priority (DEFAULT)
    private SurfaceTexture mPreviewTexture;
    private Size mPreviewSize;

    // Vectors defining the 'up' direction for the 4 angles we're interested in. These are based
    // off our world-space coordinate system (sensor coordinates), where the origin (0, 0) is in
    // the upper left of the image, and rotations are clockwise (left-handed coordinates).
    private static final float[] DIRECTION_UP_ROT_0 = {0f, -1f, 0f, 0f};
    private static final float[] DIRECTION_UP_ROT_90 = {1f, 0f, 0f, 0f};
    private static final float[] DIRECTION_UP_ROT_180 = {0f, 1f, 0f, 0f};
    private static final float[] DIRECTION_UP_ROT_270 = {-1f, 0f, 0f, 0f};

    private long mNativeContext = 0;
    private long mAdditionalContext = 0;

    private Transformation mOutputTransformation;
    private Transformation mAdditionalTransformation;

    private boolean mIsShutdown = false;
    private int mNumOutstandingSurfaces = 0;
    private Pair<Executor, Consumer<Long>> mFrameUpdateListener;

    // A combination of the model, view and projection transform matrices.
    private Size requestSize;

    private int lastRendered = 0;

    private boolean flipVertical = false;

    public OpenGLRenderer() {}

    @MainThread
    public void attachInputPreview(@NonNull Preview preview) {
        preview.setSurfaceProvider(
                mExecutor,
                surfaceRequest -> {
                    if (mIsShutdown) {
                        surfaceRequest.willNotProvideSurface();
                        return;
                    }
                    if (mNativeContext == 0) {
                        mNativeContext = initContext();
                    }
                    if (mNativeContext == 0) {
                        if (DEBUG) {
                            Log.d(TAG, "!!! error initing context");
                        }
                        surfaceRequest.willNotProvideSurface();
                        return;
                    }
                    requestSize = surfaceRequest.getResolution();
                    SurfaceTexture surfaceTexture = resetPreviewTexture(requestSize);
                    Surface inputSurface = new Surface(surfaceTexture);
                    mNumOutstandingSurfaces++;
                    surfaceRequest.provideSurface(
                            inputSurface,
                            mExecutor,
                            result -> {
                                inputSurface.release();
                                surfaceTexture.release();
                                if (surfaceTexture == mPreviewTexture) {
                                    mPreviewTexture = null;
                                }
                                mNumOutstandingSurfaces--;
                                doShutdownExecutorIfNeeded();
                            });
                });
    }

    public void attachOutputSurface(
            @NonNull Surface surface, @NonNull Size surfaceSize, int surfaceRotationDegrees) {
        try {
            if (DEBUG) {
                Log.d(TAG, "!!! attachOutputSurface " + surface);
            }
            mExecutor.execute(
                    () -> {
                        if (mIsShutdown) {
                            return;
                        }
                        if (mNativeContext == 0) {
                            mNativeContext = initContext();
                        }
                        if (mNativeContext == 0) {
                            if (DEBUG) {
                                Log.d(TAG, "!!! error initing context");
                            }
                            return;
                        }
                        mOutputTransformation = new Transformation();
                        if (DEBUG) {
                            Log.d(TAG, "!!! setWindowSurface mNativeContext" + mNativeContext);
                        }
                        if (setWindowSurface(mNativeContext, surface)) {
                            if (surfaceRotationDegrees != mOutputTransformation.mSurfaceRotationDegrees
                                    || !Objects.equals(surfaceSize, mOutputTransformation.mSurfaceSize)) {
                                mOutputTransformation.mMvpDirty = true;
                            }
                            mOutputTransformation.mSurfaceRotationDegrees = surfaceRotationDegrees;
                            mOutputTransformation.mSurfaceSize = surfaceSize;
                        } else {
                            mOutputTransformation.mSurfaceSize = null;
                        }
                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    public void attachPreviewSurface(
            @NonNull Surface surface, @NonNull Size surfaceSize, int surfaceRotationDegrees) {
        try {
            if (DEBUG) {
                Log.d(TAG, "!!! attachPreviewSurface " + surface);
            }
            mExecutor.execute(
                    () -> {
                        if (mIsShutdown) {
                            return;
                        }
                        if (mAdditionalContext == 0) {
                            mAdditionalContext = initAdditionalContext(mNativeContext);
                        }
                        if (mAdditionalContext == 0) {
                            if (DEBUG) {
                                Log.d(TAG, "!!! error initing mAdditionalContext");
                            }
                            return;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "!!! setWindowSurface mAdditionalContext mNativeContext" + mNativeContext);
                        }
                        mAdditionalTransformation = new Transformation();
                        if (setWindowSurface(mAdditionalContext, surface)) {
                            if (surfaceRotationDegrees != mAdditionalTransformation.mSurfaceRotationDegrees
                                    || !Objects.equals(surfaceSize, mAdditionalTransformation.mSurfaceSize)) {
                                mAdditionalTransformation.mMvpDirty = true;
                            }
                            mAdditionalTransformation.mSurfaceRotationDegrees = surfaceRotationDegrees;
                            mAdditionalTransformation.mSurfaceSize = surfaceSize;
                        } else {
                            mAdditionalTransformation.mSurfaceSize = null;
                        }
                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    /**
     * Sets a listener to receive updates when a frame has been drawn to the output {@link Surface}.
     *
     * <p>Frame updates include the timestamp of the latest drawn frame.
     *
     * @param executor Executor used to call the listener.
     * @param listener Listener which receives updates in the form of a timestamp (in nanoseconds).
     */
    public void setFrameUpdateListener(@NonNull Executor executor, @NonNull Consumer<Long> listener) {
        try {
            mExecutor.execute(() -> mFrameUpdateListener = new Pair<>(executor, listener));
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    public void invalidateSurface(int surfaceRotationDegrees) {
        try {
            mExecutor.execute(
                    () -> {
                        if (surfaceRotationDegrees != mOutputTransformation.mSurfaceRotationDegrees) {
                            mOutputTransformation.mMvpDirty = true;
                        }
                        mOutputTransformation.mSurfaceRotationDegrees = surfaceRotationDegrees;

                        if (mAdditionalTransformation != null) {
                            if (surfaceRotationDegrees != mAdditionalTransformation.mSurfaceRotationDegrees) {
                                mAdditionalTransformation.mMvpDirty = true;
                            }
                            mAdditionalTransformation.mSurfaceRotationDegrees = surfaceRotationDegrees;
                        }

                        if (mPreviewTexture != null && !mIsShutdown) {
                            renderLatest();
                        }
                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    public void invalidateOutputSurface(int surfaceRotationDegrees) {
        try {
            mExecutor.execute(
                    () -> {
                        if (surfaceRotationDegrees != mOutputTransformation.mSurfaceRotationDegrees) {
                            mOutputTransformation.mMvpDirty = true;
                        }
                        mOutputTransformation.mSurfaceRotationDegrees = surfaceRotationDegrees;
                        if (mPreviewTexture != null && !mIsShutdown) {
                            renderLatest();
                        }
                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    public void invalidateAdditionalSurface(int surfaceRotationDegrees) {
        if (mAdditionalTransformation == null) {
            return;
        }
        try {
            mExecutor.execute(
                    () -> {
                        if (surfaceRotationDegrees != mAdditionalTransformation.mSurfaceRotationDegrees) {
                            mAdditionalTransformation.mMvpDirty = true;
                        }
                        mAdditionalTransformation.mSurfaceRotationDegrees = surfaceRotationDegrees;
                        if (mPreviewTexture != null && !mIsShutdown) {
                            renderLatest();
                        }
                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    public void setFlipVertical(boolean flip) {
        flipVertical = flip;
    }

    /**
     * Detach the current output surface from the renderer.
     *
     * @return A {@link ListenableFuture} that signals detach from the renderer. Some devices may
     * not be able to handle the surface being released while still attached to an EGL context.
     * It should be safe to release resources associated with the output surface once this future
     * has completed.
     */
    public ListenableFuture<Void> detachOutputSurface() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            try {
                mExecutor.execute(
                        () -> {
                            if (!mIsShutdown) {
                                setWindowSurface(mNativeContext, null);
                                mOutputTransformation.mSurfaceSize = null;
                            }
                            completer.set(null);
                        });
            } catch (RejectedExecutionException e) {
                // Renderer is shutting down. Can notify that the surface is detached.
                completer.set(null);
            }
            return "detachOutputSurface [" + this + "]";
        });
    }

    public ListenableFuture<Void> detachPreviewSurface() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            try {
                mExecutor.execute(
                        () -> {
                            if (!mIsShutdown) {
                                setWindowSurface(mAdditionalContext, null);
                                mAdditionalTransformation.mSurfaceSize = null;
                            }
                            completer.set(null);
                        });
            } catch (RejectedExecutionException e) {
                // Renderer is shutting down. Can notify that the surface is detached.
                completer.set(null);
            }
            return "detachOutputSurface [" + this + "]";
        });
    }

    void shutdown() {
        try {
            mExecutor.execute(
                    () -> {
                        if (!mIsShutdown) {
                            if (mNativeContext != 0) {
                                closeContext(mNativeContext);
                            }
                            if (mAdditionalContext != 0) {
                                closeAdditionalContext(mAdditionalContext);
                            }
                            mNativeContext = 0;
                            mAdditionalContext = 0;
                            mIsShutdown = true;
                        }
                        doShutdownExecutorIfNeeded();
                    });
        } catch (RejectedExecutionException e) {
            // Renderer already shutting down. Ignore.
        }
    }

    @WorkerThread
    private void doShutdownExecutorIfNeeded() {
        if (mIsShutdown && mNumOutstandingSurfaces == 0) {
            mFrameUpdateListener = null;
            mExecutor.shutdown();
        }
    }

    @WorkerThread
    @NonNull
    private SurfaceTexture resetPreviewTexture(@NonNull Size size) {
        if (mPreviewTexture != null) {
            mPreviewTexture.detachFromGLContext();
        }
        mPreviewTexture = new SurfaceTexture(getTexName(mNativeContext));
        mPreviewTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        mPreviewTexture.setOnFrameAvailableListener(
                surfaceTexture -> {
                    if (surfaceTexture == mPreviewTexture && !mIsShutdown) {
                        if (DEBUG) {
                            Log.d(TAG, "setOnFrameAvailableListener");
                        }
                        makeCurrent(mNativeContext);
                        surfaceTexture.updateTexImage();
                        renderLatest();
                    }
                },
                mExecutor.getHandler());
        if (!Objects.equals(size, mPreviewSize)) {
            if (mOutputTransformation != null) {
                mOutputTransformation.mMvpDirty = true;
            }
        }
        mPreviewSize = size;
        return mPreviewTexture;
    }

    @WorkerThread
    private void renderLatest() {
        if (DEBUG) {
            Log.d(TAG, "renderLatest start");
        }
        if (mOutputTransformation == null) {
            return;
        }
        // Get the timestamp so we can pass it along to the output surface (not strictly necessary)
        long timestampNs = mPreviewTexture.getTimestamp();

        // Get texture transform from surface texture (transform to natural orientation).
        // This will be used to transform texture coordinates in the fragment shader.
        mPreviewTexture.getTransformMatrix(mOutputTransformation.mTextureTransform);
        if (mAdditionalTransformation != null) {
            mPreviewTexture.getTransformMatrix(mAdditionalTransformation.mTextureTransform);
        }
        // Check whether the texture's rotation has changed so we can update the MVP matrix.
        int textureRotationDegrees = getTextureRotationDegrees(mOutputTransformation);
        if (textureRotationDegrees != mOutputTransformation.mTextureRotationDegrees) {
            mOutputTransformation.mMvpDirty = true;
        }
        mOutputTransformation.mTextureRotationDegrees = textureRotationDegrees;

        if (mAdditionalTransformation != null) {
            int deg = getTextureRotationDegrees(mAdditionalTransformation);
            if (deg != mAdditionalTransformation.mTextureRotationDegrees) {
                mAdditionalTransformation.mMvpDirty = true;
            }
            mAdditionalTransformation.mTextureRotationDegrees = deg;
        }

        if (mOutputTransformation.mMvpDirty) {
            updateMvpTransform(mOutputTransformation, false);
        }

        boolean success;
        if (mOutputTransformation.mSurfaceSize != null) {
            setViewPort(mOutputTransformation.mSurfaceSize.getWidth(), mOutputTransformation.mSurfaceSize.getHeight());
            success = renderTexture(mNativeContext, timestampNs, mOutputTransformation.mMvpTransform,
                    (mOutputTransformation.mMvpDirty || lastRendered != 1), mOutputTransformation.mTextureTransform, getTexName(mNativeContext));
            mOutputTransformation.mMvpDirty = false;
            lastRendered = 1;
        } else {
            success = false;
        }

        if (mAdditionalTransformation != null && mAdditionalTransformation.mSurfaceSize != null) {
            if (mAdditionalTransformation.mMvpDirty) {
                updateMvpTransform(mAdditionalTransformation, true);
            }
            makeCurrent(mAdditionalContext);
            setViewPort(mAdditionalTransformation.mSurfaceSize.getWidth(), mAdditionalTransformation.mSurfaceSize.getHeight());
            renderTexture(mAdditionalContext, timestampNs, mAdditionalTransformation.mMvpTransform,
                    (mAdditionalTransformation.mMvpDirty || lastRendered != 2), mAdditionalTransformation.mTextureTransform, getTexName(mAdditionalContext));
            lastRendered = 2;
            mAdditionalTransformation.mMvpDirty = false;
        }

        if (success && mFrameUpdateListener != null) {
            Executor executor = Objects.requireNonNull(mFrameUpdateListener.first);
            Consumer<Long> listener = Objects.requireNonNull(mFrameUpdateListener.second);
            try {
                if (DEBUG) {
                    Log.d(TAG, "renderLatest mFrameUpdateListener");
                }
                executor.execute(() -> listener.accept(timestampNs));
            } catch (RejectedExecutionException e) {
                // Unable to send frame update. Ignore.
            }
        }

        if (DEBUG) {
            Log.d(TAG, "renderLatest done");
        }
    }

    /**
     * Calculates the rotation of the source texture between the sensor coordinate space and
     * the device's 'natural' orientation.
     *
     * <p>A required transform matrix is passed along with each texture update and is retrieved by
     * {@link SurfaceTexture#getTransformMatrix(float[])}.
     *
     * <pre>{@code
     *        TEXTURE FROM SENSOR:
     * ^
     * |                  +-----------+
     * |          .#######|###        |
     * |           *******|***        |
     * |   ....###########|## ####. / |         Sensor may be rotated relative
     * |  ################|## #( )#.  |         to the device's 'natural'
     * |       ###########|## ######  |         orientation.
     * |  ################|## #( )#*  |
     * |   ****###########|## ####* \ |
     * |           .......|...        |
     * |          *#######|###        |
     * |                  +-----------+
     * +-------------------------------->
     *                                               TRANSFORMED IMAGE:
     *                 | |                   ^
     *                 | |                   |         .            .
     *                 | |                   |         \\ ........ //
     *   Transform matrix from               |         ##############
     *   SurfaceTexture#getTransformMatrix() |       ###(  )####(  )###
     *   performs scale/crop/rotate on       |      ####################
     *   image from sensor to produce        |     ######################
     *   image in 'natural' orientation.     | ..  ......................  ..
     *                 | |                   |#### ###################### ####
     *                 | +-------\           |#### ###################### ####
     *                 +---------/           |#### ###################### ####
     *                                       +-------------------------------->
     * }</pre>
     *
     * <p>The transform matrix is a 4x4 affine transform matrix that operates on standard normalized
     * texture coordinates which are in the range of [0,1] for both s and t dimensions. Before
     * the transform is applied, the texture may have dimensions that are larger than the
     * dimensions of the SurfaceTexture we provided in order to accommodate hardware limitations.
     *
     * <p>For this method we are only interested in the rotation component of the transform
     * matrix, so the calculations avoid the scaling and translation components.
     */
    @WorkerThread
    private int getTextureRotationDegrees(Transformation transformation) {
        // The final output image should have the requested dimensions AFTER applying the
        // transform matrix, but width and height may be swapped. We know that the transform
        // matrix from SurfaceTexture#getTransformMatrix() is an affine transform matrix that
        // will only rotate in 90 degree increments, so we only need to worry about the rotation
        // component.
        //
        // We can test this by using an test vector of [s, t, p, q] = [0, 1, 0, 0]. Using 'q = 0'
        // will ignore the translation component of the matrix. We will only need to check if the
        // 's' component becomes a scaled version of the 't' component and the 't' component
        // becomes 0.
        Matrix.multiplyMV(transformation.mTempVec, 0, transformation.mTextureTransform, 0, DIRECTION_UP_ROT_0, 0);
        // Calculate the normalized vector and round to integers so we can do integer comparison.
        // Normalizing the vector removes the effects of the scaling component of the
        // transform matrix. Once normalized, we can round and do integer comparison.
        float length = Matrix.length(transformation.mTempVec[0], transformation.mTempVec[1], 0);
        int s = Math.round(transformation.mTempVec[0] / length);
        int t = Math.round(transformation.mTempVec[1] / length);
        if (s == 0 && t == 1) {
            //       (0,1)                               (0,1)
            //    +----^----+          0 deg          +----^----+
            //    |    |    |        Rotation         |    |    |
            //    |    +    |         +----->         |    +    |
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 0;
        } else if (s == 1 && t == 0) {
            //       (0,1)
            //    +----^----+         90 deg          +---------+
            //    |    |    |        Rotation         |         |
            //    |    +    |         +----->         |    +---->(1,0)
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 90;
        } else if (s == 0 && t == -1) {
            //       (0,1)
            //    +----^----+         180 deg         +---------+
            //    |    |    |        Rotation         |  (0,0)  |
            //    |    +    |         +----->         |    +    |
            //    |  (0,0)  |                         |    |    |
            //    +---------+                         +----v----+
            //                                           (0,-1)
            return 180;
        } else if (s == -1 && t == 0) {
            //       (0,1)
            //    +----^----+         270 deg         +---------+
            //    |    |    |        Rotation         |         |
            //    |    +    |         +----->   (-1,0)<----+    |
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 270;
        }
        throw new RuntimeException(String.format("Unexpected texture transform matrix. Expected "
                + "test vector [0, 1] to rotate to [0,1], [1, 0], [0, -1] or [-1, 0], but instead "
                + "was [%d, %d].", s, t));
    }

    /**
     * Derives the model crop rect from the texture and output surface dimensions, applying a
     * 'center-crop' transform.
     *
     * <p>Because the camera sensor (or crop of the camera sensor) may have a different
     * aspect ratio than the ViewPort that is meant to display it, we want to fit the image
     * from the camera so the entire ViewPort is filled. This generally requires scaling the input
     * texture and cropping pixels from either the width or height. We call this transform
     * 'center-crop' and is equivalent to {@link android.widget.ImageView.ScaleType#CENTER_CROP}.
     */
    @WorkerThread
    private void extractPreviewCropFromPreviewSizeAndSurface(Transformation transformation) {
        // Swap the dimensions of the surface we are drawing the texture onto if rotating the
        // texture to the surface orientation requires a 90 degree or 270 degree rotation.
        if (transformation.mSurfaceSize != null) {
            int viewPortRotation = getViewPortRotation(transformation);
            if (viewPortRotation == 90 || viewPortRotation == 270) {
                // Width and height swapped
                transformation.mCropRect = new RectF(0, 0, transformation.mSurfaceSize.getHeight(), transformation.mSurfaceSize.getWidth());
            } else {
                transformation.mCropRect = new RectF(0, 0, transformation.mSurfaceSize.getWidth(), transformation.mSurfaceSize.getHeight());
            }
            android.graphics.Matrix centerCropMatrix = new android.graphics.Matrix();
            RectF previewSize = new RectF(0, 0, mPreviewSize.getWidth(), mPreviewSize.getHeight());
            centerCropMatrix.setRectToRect(transformation.mCropRect, previewSize,
                    android.graphics.Matrix.ScaleToFit.CENTER);
            centerCropMatrix.mapRect(transformation.mCropRect);
        }

    }

    /**
     * Returns the relative rotation between the sensor coordinates and the ViewPort in
     * world-space coordinates.
     *
     * <p>This is the angle the sensor needs to be rotated, clockwise, in order to be upright in
     * the viewport coordinates.
     */
    @WorkerThread
    private int getViewPortRotation(Transformation transformation) {
        // Note that since the rotation defined by Surface#ROTATION_*** are positive when the
        // device is rotated in a counter-clockwise direction and our world-space coordinates
        // define positive angles in the clockwise direction, we add the two together to get the
        // total angle required.
        return (transformation.mTextureRotationDegrees + transformation.mSurfaceRotationDegrees) % 360;
    }

    /**
     * Updates the matrix used to transform the model into the correct dimensions within the
     * world-space.
     *
     * <p>In order to draw the camera frames to screen, we use a flat rectangle in our
     * world-coordinate space. The world coordinates match the preview buffer coordinates with
     * the origin (0,0) in the upper left corner of the image. Defining the world space in this
     * way allows subsequent models to be positioned according to buffer coordinates.
     * Note this different than standard OpenGL coordinates; this is a left-handed coordinate
     * system, and requires using glFrontFace(GL_CW) before drawing.
     * <pre>{@code
     *             Standard coordinates:                   Our coordinate system:
     *
     *                      | +y                                  ________+x
     *                      |                                   /|
     *                      |                                  / |
     *                      |________+x                     +z/  |
     *                     /                                     | +y
     *                    /
     *                   /+z
     * }</pre>
     * <p>Our model is initially a square with vertices in the range (-1,-1 - 1,1). It is
     * rotated, scaled and translated to match the dimensions of preview with the origin in the
     * upper left corner.
     *
     * <p>Example for a preview with dimensions 1920x1080:
     * <pre>{@code
     *                (-1,-1)    (1,-1)
     *                   +---------+        Model
     *                   |         |        Transform          (0,0)         (1920,0)
     * Unscaled Model -> |    +    |         ---\                +----------------+
     *                   |         |         ---/                |                |      Scaled/
     *                   +---------+                             |                | <-- Translated
     *                (-1,1)     (1,1)                           |                |       Model
     *                                                           +----------------+
     *                                                         (0,1080)      (1920,1080)
     * }</pre>
     */
    @WorkerThread
    private void updateModelTransform(Transformation transformation, boolean isPreview) {
        // Remove the rotation to the device 'natural' orientation so our world space will be in
        // sensor coordinates.
        Matrix.setRotateM(transformation.mTempMatrix, 0, -transformation.mTextureRotationDegrees, 0.0f, 0.0f, 1.0f);
        Matrix.setIdentityM(transformation.mTempMatrix, 16);
        // Translate to the upper left corner of the quad so we are in buffer space
        Matrix.translateM(transformation.mTempMatrix, 16, mPreviewSize.getWidth() / 2f,
                mPreviewSize.getHeight() / 2f, 0);
        // Scale the vertices so that our world space units are pixels equal in size to the
        // pixels of the buffer sent from the camera.
        Matrix.scaleM(transformation.mTempMatrix, 16, mPreviewSize.getWidth() / 2f, mPreviewSize.getHeight() / 2f,
                1f);

        if (flipVertical) {
            if (transformation.mSurfaceRotationDegrees == 90 || transformation.mSurfaceRotationDegrees == 270) {
//                if (!isPreview) { // mirror only on stream, not preview
                Matrix.scaleM(transformation.mTempMatrix, 16, -1f, 1f, 1f);
//                }
            } else {
                Matrix.scaleM(transformation.mTempMatrix, 16, 1f, -1f, 1f);
            }
        }

        Matrix.multiplyMM(transformation.mModelTransform, 0, transformation.mTempMatrix, 16, transformation.mTempMatrix, 0);
        if (DEBUG) {
            printMatrix("ModelTransform", transformation.mModelTransform, 0);
        }
    }

    /**
     * The view transform defines the position and orientation of the camera within our world-space.
     *
     * <p>This brings us from world-space coordinates to view (camera) space.
     *
     * <p>This matrix is defined by a camera position, a gaze point, and a vector that represents
     * the "up" direction. Because we are using an orthogonal projection, we always place the
     * camera directly in front of the gaze point and 1 unit away on the z-axis for convenience.
     * We have defined our world coordinates in a way where we will be looking at the front of
     * the model rectangle if our camera is placed on the positive z-axis and we gaze towards
     * the negative z-axis.
     */
    @WorkerThread
    private void updateViewTransform(Transformation transformation) {
        // Apply the rotation of the ViewPort and look at the center of the image
        float[] upVec = DIRECTION_UP_ROT_0;
        switch (getViewPortRotation(transformation)) {
            case 0:
                upVec = DIRECTION_UP_ROT_0;
                break;
            case 90:
                upVec = DIRECTION_UP_ROT_90;
                break;
            case 180:
                upVec = DIRECTION_UP_ROT_180;
                break;
            case 270:
                upVec = DIRECTION_UP_ROT_270;
                break;
        }
        Matrix.setLookAtM(transformation.mViewTransform, 0,
                transformation.mCropRect.centerX(), transformation.mCropRect.centerY(), 1, // Camera position
                transformation.mCropRect.centerX(), transformation.mCropRect.centerY(), 0, // Point to look at
                upVec[0], upVec[1], upVec[2] // Up direction
        );
        if (DEBUG) {
            printMatrix("ViewTransform", transformation.mViewTransform, 0);
        }
    }

    /**
     * The projection matrix will map from the view space to normalized device coordinates (NDC)
     * which OpenGL is expecting.
     *
     * <p>Our view is meant to only show the pixels defined by the model crop rect, so our
     * orthogonal projection matrix will depend on the preview crop rect dimensions.
     *
     * <p>The projection matrix can be thought of as a cube which has sides that align with the
     * edges of the ViewPort and the near/far sides can be adjusted as needed. In our case, we
     * set the near side to match the camera position and the far side to match the model's
     * position on the z-axis, 1 unit away.
     */
    @WorkerThread
    private void updateProjectionTransform(Transformation transformation) {
        float viewPortWidth = transformation.mCropRect.width();
        float viewPortHeight = transformation.mCropRect.height();
        if (viewPortWidth == 0) {
            if (DEBUG) {
                Log.d(TAG, "ProjectionTransform: Nothing to do because of width 0");
            }
            return;
        }
        if (viewPortHeight == 0) {
            if (DEBUG) {
                Log.d(TAG, "ProjectionTransform: Nothing to do because of height 0");
            }
            return;
        }
        // Since projection occurs after rotation of the camera, in order to map directly to model
        // coordinates we need to take into account the surface rotation.
        int viewPortRotation = getViewPortRotation(transformation);
        if (viewPortRotation == 90 || viewPortRotation == 270) {
            viewPortWidth = transformation.mCropRect.height();
            viewPortHeight = transformation.mCropRect.width();
        }
        Matrix.orthoM(transformation.mProjectionTransform, 0,
                /*left=*/-viewPortWidth / 2f, /*right=*/viewPortWidth / 2f,
                /*bottom=*/viewPortHeight / 2f, /*top=*/-viewPortHeight / 2f,
                /*near=*/0, /*far=*/1);
        if (DEBUG) {
            printMatrix("ProjectionTransform", transformation.mProjectionTransform, 0);
        }
    }

    /**
     * The MVP is the combination of model, view and projection transforms that take us from the
     * world space to normalized device coordinates (NDC) which OpenGL uses to display images
     * with the correct dimensions on an EGL surface.
     */
    @WorkerThread
    private void updateMvpTransform(Transformation transformation, boolean isPreview) {
//        if (mPreviewCropRect == null) {
        extractPreviewCropFromPreviewSizeAndSurface(transformation);
//        }
        if (DEBUG) {
            Log.d(TAG, String.format("Model dimensions: %s, Crop rect: %s", mPreviewSize,
                    transformation.mCropRect));
        }
        updateModelTransform(transformation, isPreview);
        updateViewTransform(transformation);
        updateProjectionTransform(transformation);
        Matrix.multiplyMM(transformation.mTempMatrix, 0, transformation.mViewTransform, 0, transformation.mModelTransform, 0);
        if (DEBUG) {
            // Print the model-view matrix (without projection)
            printMatrix("MVTransform", transformation.mTempMatrix, 0);
        }
        Matrix.multiplyMM(transformation.mMvpTransform, 0, transformation.mProjectionTransform, 0, transformation.mTempMatrix, 0);
        if (DEBUG) {
            printMatrix("MVPTransform", transformation.mMvpTransform, 0);
        }
    }

    private static void printMatrix(String label, float[] matrix, int offset) {
        Log.d(TAG, String.format("%s:\n"
                        + "%.4f %.4f %.4f %.4f\n"
                        + "%.4f %.4f %.4f %.4f\n"
                        + "%.4f %.4f %.4f %.4f\n"
                        + "%.4f %.4f %.4f %.4f\n", label,
                matrix[offset], matrix[offset + 4], matrix[offset + 8], matrix[offset + 12],
                matrix[offset + 1], matrix[offset + 5], matrix[offset + 9], matrix[offset + 13],
                matrix[offset + 2], matrix[offset + 6], matrix[offset + 10], matrix[offset + 14],
                matrix[offset + 3], matrix[offset + 7], matrix[offset + 11], matrix[offset + 15]));
    }

    @WorkerThread
    private static native long initContext();

    private static native long initAdditionalContext(long nativeContext);

    @WorkerThread
    private static native boolean setWindowSurface(long nativeContext, @Nullable Surface surface);

    @WorkerThread
    private static native int getTexName(long nativeContext);

    @WorkerThread
    private static native boolean makeCurrent(long nativeContext);

    @WorkerThread
    private static native void setViewPort(int width, int height);

    @WorkerThread
    private static native boolean renderTexture(
            long nativeContext,
            long timestampNs,
            @NonNull float[] mvpTransform,
            boolean mvpDirty,
            @NonNull float[] textureTransform,
            int texName);

    @WorkerThread
    private static native void closeContext(long nativeContext);
    @WorkerThread
    private static native void closeAdditionalContext(long nativeContext);

    private static final class SingleThreadHandlerExecutor implements Executor {
        private final String mThreadName;
        private final HandlerThread mHandlerThread;
        private final Handler mHandler;

        SingleThreadHandlerExecutor(@NonNull String threadName, int priority) {
            this.mThreadName = threadName;
            mHandlerThread = new HandlerThread(threadName, priority);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }

        @NonNull
        Handler getHandler() {
            return mHandler;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (!mHandler.post(command)) {
                throw new RejectedExecutionException(mThreadName + " is shutting down.");
            }
        }

        boolean shutdown() {
            return mHandlerThread.quitSafely();
        }
    }

    private final class Transformation {
        RectF mCropRect;
        int mTextureRotationDegrees;

        // Transform retrieved by SurfaceTexture.getTransformMatrix
        private final float[] mTextureTransform = new float[16];
        // The Model represent the surface we are drawing on. In 3D, it is a flat rectangle.
        private final float[] mModelTransform = new float[16];
        private final float[] mViewTransform = new float[16];
        private final float[] mProjectionTransform = new float[16];
        // A combination of the model, view and projection transform matrices.
        private final float[] mMvpTransform = new float[16];
        private boolean mMvpDirty = true;
        private Size mSurfaceSize = null;
        private int mSurfaceRotationDegrees = 0;

        private float[] mTempVec = new float[4];
        private float[] mTempMatrix = new float[32]; // 2 concatenated matrices for calculations
    }
}
