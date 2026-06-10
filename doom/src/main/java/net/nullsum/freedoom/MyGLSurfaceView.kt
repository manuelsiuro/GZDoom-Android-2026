@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

/*
 * Copyright (C) 2008 The Android Open Source Project
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

package net.nullsum.freedoom

import android.content.Context
import android.opengl.GLDebugHelper
import android.opengl.GLSurfaceView.EGLConfigChooser
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.Writer
import java.lang.ref.WeakReference
import java.util.ArrayList
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying OpenGL rendering.
 *
 * See the original Android documentation for usage details. This is a
 * faithful, Honeycomb-era implementation kept intact on purpose.
 */
open class MyGLSurfaceView : SurfaceView, SurfaceHolder.Callback {

    private val mThisWeakRef = WeakReference(this)
    private var mGLThread: GLThread? = null
    private var mRenderer: Renderer? = null
    private var mDetached = false
    private var mEGLConfigChooser: EGLConfigChooser? = null
    private var mEGLContextFactory: EGLContextFactory? = null
    private var mEGLWindowSurfaceFactory: EGLWindowSurfaceFactory? = null
    private var mGLWrapper: GLWrapper? = null
    private var mDebugFlags = 0
    private var mEGLContextClientVersion = 0
    private var mPreserveEGLContextOnPause = false

    /**
     * Standard View constructor. In order to render something, you
     * must call [setRenderer] to register a renderer.
     */
    constructor(context: Context?) : super(context) {
        init()
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call [setRenderer] to register a renderer.
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            mGLThread?.requestExitAndWait()
        } finally {
            // no super.finalize() in Kotlin; Object.finalize() is final from Kotlin's view.
        }
    }

    private fun init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        val holder = holder
        holder.addCallback(this)
        // setFormat is done by SurfaceView in SDK 2.3 and newer. Uncomment
        // this statement if back-porting to 2.2 or older:
        // holder.setFormat(PixelFormat.RGB_565);
        //
        // setType is not needed for SDK 2.0 or newer. Uncomment this
        // statement if back-porting this code to older SDKs.
        // holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    /**
     * Set the glWrapper. See [GLWrapper.wrap].
     */
    fun setGLWrapper(glWrapper: GLWrapper?) {
        mGLWrapper = glWrapper
    }

    /**
     * Get the current value of the debug flags.
     */
    fun getDebugFlags(): Int {
        return mDebugFlags
    }

    /**
     * Set the debug flags to a new value.
     */
    fun setDebugFlags(debugFlags: Int) {
        mDebugFlags = debugFlags
    }

    /**
     * @return true if the EGL context will be preserved when paused
     */
    fun getPreserveEGLContextOnPause(): Boolean {
        return mPreserveEGLContextOnPause
    }

    /**
     * Control whether the EGL context is preserved when the MyGLSurfaceView is paused and
     * resumed.
     */
    fun setPreserveEGLContextOnPause(preserveOnPause: Boolean) {
        mPreserveEGLContextOnPause = preserveOnPause
    }

    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     */
    fun setRenderer(renderer: Renderer?) {
        checkRenderThreadState()
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = SimpleEGLConfigChooser(true)
        }
        if (mEGLContextFactory == null) {
            mEGLContextFactory = DefaultContextFactory()
        }
        if (mEGLWindowSurfaceFactory == null) {
            mEGLWindowSurfaceFactory = DefaultWindowSurfaceFactory()
        }
        mRenderer = renderer
        mGLThread = GLThread(mThisWeakRef)
        mGLThread!!.start()
    }

    /**
     * Install a custom EGLContextFactory.
     */
    fun setEGLContextFactory(factory: EGLContextFactory?) {
        checkRenderThreadState()
        mEGLContextFactory = factory
    }

    /**
     * Install a custom EGLWindowSurfaceFactory.
     */
    fun setEGLWindowSurfaceFactory(factory: EGLWindowSurfaceFactory?) {
        checkRenderThreadState()
        mEGLWindowSurfaceFactory = factory
    }

    /**
     * Install a custom EGLConfigChooser.
     */
    fun setEGLConfigChooser(configChooser: EGLConfigChooser?) {
        checkRenderThreadState()
        mEGLConfigChooser = configChooser
    }

    /**
     * Install a config chooser which will choose a config
     * as close to 16-bit RGB as possible, with or without an optional depth
     * buffer as close to 16-bits as possible.
     */
    fun setEGLConfigChooser(needDepth: Boolean) {
        setEGLConfigChooser(SimpleEGLConfigChooser(needDepth))
    }

    /**
     * Install a config chooser which will choose a config
     * with at least the specified depthSize and stencilSize,
     * and exactly the specified redSize, greenSize, blueSize and alphaSize.
     */
    fun setEGLConfigChooser(
        redSize: Int, greenSize: Int, blueSize: Int,
        alphaSize: Int, depthSize: Int, stencilSize: Int
    ) {
        setEGLConfigChooser(
            ComponentSizeChooser(
                redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize
            )
        )
    }

    /**
     * Inform the default EGLContextFactory and default EGLConfigChooser
     * which EGLContext client version to pick.
     *
     * @param version The EGLContext client version to choose. Use 2 for OpenGL ES 2.0
     */
    fun setEGLContextClientVersion(version: Int) {
        checkRenderThreadState()
        mEGLContextClientVersion = version
    }

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    fun getRenderMode(): Int {
        return mGLThread!!.getRenderMode()
    }

    /**
     * Set the rendering mode.
     */
    fun setRenderMode(renderMode: Int) {
        mGLThread!!.setRenderMode(renderMode)
    }
    // ----------------------------------------------------------------------

    /**
     * Request that the renderer render a frame.
     */
    fun requestRender() {
        mGLThread!!.requestRender()
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of MyGLSurfaceView.
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        mGLThread!!.surfaceCreated()
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of MyGLSurfaceView.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return
        mGLThread!!.surfaceDestroyed()
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of MyGLSurfaceView.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        mGLThread!!.onWindowResize(w, h)
    }

    /**
     * Inform the view that the activity is paused.
     */
    fun onPause() {
        mGLThread!!.onPause()
    }

    /**
     * Inform the view that the activity is resumed.
     */
    fun onResume() {
        mGLThread!!.onResume()
    }

    /**
     * Queue a runnable to be run on the GL rendering thread.
     */
    fun queueEvent(r: Runnable?) {
        mGLThread!!.queueEvent(r)
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of MyGLSurfaceView.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =$mDetached")
        }
        if (mDetached && mRenderer != null) {
            var renderMode = RENDERMODE_CONTINUOUSLY
            if (mGLThread != null) {
                renderMode = mGLThread!!.getRenderMode()
            }
            mGLThread = GLThread(mThisWeakRef)
            if (renderMode != RENDERMODE_CONTINUOUSLY) {
                mGLThread!!.setRenderMode(renderMode)
            }
            mGLThread!!.start()
        }
        mDetached = false
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of MyGLSurfaceView.
     */
    override fun onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow")
        }
        mGLThread?.requestExitAndWait()
        mDetached = true
        super.onDetachedFromWindow()
    }

    fun appPausing() {
        mGLThread!!.forceSurfaceDestory()
    }

    fun setupSurface(): Boolean {
        return mGLThread!!.forceSurfaceCreation()
    }

    fun swapBuffers() {
        mGLThread!!.swapBuffers()
    }

    private fun checkRenderThreadState() {
        if (mGLThread != null) {
            throw IllegalStateException(
                "setRenderer has already been called for this instance."
            )
        }
    }

    /**
     * An interface used to wrap a GL interface.
     *
     * @see setGLWrapper
     */
    interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         *
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        fun wrap(gl: GL?): GL?
    }

    /**
     * A generic renderer interface.
     *
     * @see setRenderer
     */
    interface Renderer {
        /**
         * Called when the surface is created or recreated.
         */
        fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)

        /**
         * Called when the surface changed size.
         */
        fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)

        /**
         * Called to draw the current frame.
         */
        fun onDrawFrame(gl: GL10?)
    }

    /**
     * An interface for customizing the eglCreateContext and eglDestroyContext calls.
     *
     * @see setEGLContextFactory
     */
    interface EGLContextFactory {
        fun createContext(egl: EGL10?, display: EGLDisplay?, eglConfig: EGLConfig?): EGLContext?

        fun destroyContext(egl: EGL10?, display: EGLDisplay?, context: EGLContext?)
    }

    /**
     * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
     *
     * @see setEGLWindowSurfaceFactory
     */
    interface EGLWindowSurfaceFactory {
        /**
         * @return null if the surface cannot be constructed.
         */
        fun createWindowSurface(
            egl: EGL10?, display: EGLDisplay?, config: EGLConfig?,
            nativeWindow: Any?
        ): EGLSurface?

        fun destroySurface(egl: EGL10?, display: EGLDisplay?, surface: EGLSurface?)
    }

    private class DefaultWindowSurfaceFactory : EGLWindowSurfaceFactory {

        override fun createWindowSurface(
            egl: EGL10?, display: EGLDisplay?,
            config: EGLConfig?, nativeWindow: Any?
        ): EGLSurface? {
            var result: EGLSurface? = null
            try {
                result = egl!!.eglCreateWindowSurface(display, config, nativeWindow, null)
            } catch (e: IllegalArgumentException) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e(TAG, "eglCreateWindowSurface", e)
            }
            return result
        }

        override fun destroySurface(
            egl: EGL10?, display: EGLDisplay?,
            surface: EGLSurface?
        ) {
            egl!!.eglDestroySurface(display, surface)
        }
    }

    /**
     * An EGL helper class.
     */
    internal class EglHelper(private val mGLSurfaceViewWeakRef: WeakReference<MyGLSurfaceView>) {
        var mEgl: EGL10? = null
        var mEglDisplay: EGLDisplay? = null
        var mEglSurface: EGLSurface? = null
        var mEglConfig: EGLConfig? = null
        var mEglContext: EGLContext? = null

        /**
         * Initialize EGL for a given configuration spec.
         */
        fun start() {
            if (LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().id)
            }
            /*
             * Get an EGL instance
             */
            mEgl = EGLContext.getEGL() as EGL10

            /*
             * Get to the default display.
             */
            mEglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

            if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
                throw RuntimeException("eglGetDisplay failed")
            }

            /*
             * We can now initialize EGL for that display
             */
            val version = IntArray(2)
            if (!mEgl!!.eglInitialize(mEglDisplay, version)) {
                throw RuntimeException("eglInitialize failed")
            }
            val view = mGLSurfaceViewWeakRef.get()
            if (view == null) {
                mEglConfig = null
                mEglContext = null
            } else {
                mEglConfig = view.mEGLConfigChooser!!.chooseConfig(mEgl, mEglDisplay)

                /*
                 * Create an EGL context. We want to do this as rarely as we can, because an
                 * EGL context is a somewhat heavy object.
                 */
                mEglContext = view.mEGLContextFactory!!.createContext(mEgl, mEglDisplay, mEglConfig)
            }
            if (mEglContext == null || mEglContext === EGL10.EGL_NO_CONTEXT) {
                mEglContext = null
                throwEglException("createContext")
            }
            if (LOG_EGL) {
                Log.w("EglHelper", "createContext " + mEglContext + " tid=" + Thread.currentThread().id)
            }

            mEglSurface = null
        }

        /**
         * Create an egl surface for the current SurfaceHolder surface. If a surface
         * already exists, destroy it before creating the new surface.
         *
         * @return true if the surface was created successfully.
         */
        fun createSurface(): Boolean {
            if (LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().id)
            }
            /*
             * Check preconditions.
             */
            if (mEgl == null) {
                throw RuntimeException("egl not initialized")
            }
            if (mEglDisplay == null) {
                throw RuntimeException("eglDisplay not initialized")
            }
            if (mEglConfig == null) {
                throw RuntimeException("mEglConfig not initialized")
            }

            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            destroySurfaceImp()

            /*
             * Create an EGL surface we can render into.
             */
            val view = mGLSurfaceViewWeakRef.get()
            if (view != null) {
                mEglSurface = view.mEGLWindowSurfaceFactory!!.createWindowSurface(
                    mEgl,
                    mEglDisplay, mEglConfig, view.holder
                )
            } else {
                mEglSurface = null
            }

            if (mEglSurface == null || mEglSurface === EGL10.EGL_NO_SURFACE) {
                val error = mEgl!!.eglGetError()
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.")
                }
                return false
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            if (!mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                /*
                 * Could not make the context current, probably because the underlying
                 * SurfaceView surface has been destroyed.
                 */
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl!!.eglGetError())
                return false
            }

            return true
        }

        /**
         * Create a GL object for the current EGL context.
         */
        fun createGL(): GL? {

            var gl = mEglContext!!.gl
            val view = mGLSurfaceViewWeakRef.get()
            if (view != null) {
                if (view.mGLWrapper != null) {
                    gl = view.mGLWrapper!!.wrap(gl)
                }

                if ((view.mDebugFlags and (DEBUG_CHECK_GL_ERROR or DEBUG_LOG_GL_CALLS)) != 0) {
                    var configFlags = 0
                    var log: Writer? = null
                    if ((view.mDebugFlags and DEBUG_CHECK_GL_ERROR) != 0) {
                        configFlags = configFlags or GLDebugHelper.CONFIG_CHECK_GL_ERROR
                    }
                    if ((view.mDebugFlags and DEBUG_LOG_GL_CALLS) != 0) {
                        log = LogWriter()
                    }
                    gl = GLDebugHelper.wrap(gl, configFlags, log)
                }
            }
            return gl
        }

        /**
         * Display the current render surface.
         *
         * @return the EGL error code from eglSwapBuffers.
         */
        fun swap(): Int {
            if (!mEgl!!.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                return mEgl!!.eglGetError()
            }
            return EGL10.EGL_SUCCESS
        }

        fun destroySurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().id)
            }
            destroySurfaceImp()
        }

        private fun destroySurfaceImp() {
            if (mEglSurface != null && mEglSurface !== EGL10.EGL_NO_SURFACE) {
                mEgl!!.eglMakeCurrent(
                    mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT
                )
                val view = mGLSurfaceViewWeakRef.get()
                if (view != null) {
                    view.mEGLWindowSurfaceFactory!!.destroySurface(mEgl, mEglDisplay, mEglSurface)
                }
                mEglSurface = null
            }
        }

        fun finish() {
            if (LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().id)
            }
            if (mEglContext != null) {
                val view = mGLSurfaceViewWeakRef.get()
                if (view != null) {
                    view.mEGLContextFactory!!.destroyContext(mEgl, mEglDisplay, mEglContext)
                }
                mEglContext = null
            }
            if (mEglDisplay != null) {
                mEgl!!.eglTerminate(mEglDisplay)
                mEglDisplay = null
            }
        }

        private fun throwEglException(function: String) {
            throwEglException(function, mEgl!!.eglGetError())
        }

        companion object {
            fun throwEglException(function: String, error: Int) {
                val message = formatEglError(function, error)
                if (LOG_THREADS) {
                    Log.e(
                        "EglHelper", "throwEglException tid=" + Thread.currentThread().id + " "
                                + message
                    )
                }
                throw RuntimeException(message)
            }

            fun logEglErrorAsWarning(tag: String, function: String, error: Int) {
                Log.w(tag, formatEglError(function, error))
            }

            fun formatEglError(function: String, error: Int): String {
                //return function + " failed: " + EGLLogWrapper.getErrorString(error);
                return "$function failed: $function"
            }
        }
    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     *
     * All potentially blocking synchronization is done through the
     * sGLThreadManager object. This avoids multiple-lock ordering issues.
     */
    internal class GLThread(
        /**
         * Set once at thread construction time, nulled out when the parent view is garbage
         * called. This weak reference allows the MyGLSurfaceView to be garbage collected while
         * the GLThread is still alive.
         */
        private val mGLSurfaceViewWeakRef: WeakReference<MyGLSurfaceView>
    ) : Thread() {
        var mEglHelper: EglHelper? = null
        var gl: GL10? = null
        var createEglContext = false
        var createEglSurface = false
        var createGlInterface = false
        var lostEglContext = false
        var sizeChanged = false
        var wantRenderNotification = false
        var doRenderNotification = false
        var askedToReleaseEglContext = false
        var w = 0
        var h = 0
        var event: Runnable? = null

        // Once the thread is started, all accesses to the following member
        // variables are protected by the sGLThreadManager monitor
        private var mShouldExit = false
        var mExited = false
        private var mRequestPaused = false
        private var mPaused = false
        private var mHasSurface = false
        private var mSurfaceIsBad = false
        private var mWaitingForSurface = false
        private var mHaveEglContext = false
        private var mHaveEglSurface = false
        private var mFinishedCreatingEglSurface = false
        private var mShouldReleaseEglContext = false
        private var mWidth = 0
        private var mHeight = 0
        private var mRenderMode = RENDERMODE_CONTINUOUSLY
        private var mRequestRender = true
        private var mRenderComplete = false
        private val mEventQueue = ArrayList<Runnable>()
        private var mSizeChanged = true

        override fun run() {
            name = "GLThread $id"
            if (LOG_THREADS) {
                Log.i("GLThread", "starting tid=$id")
            }

            try {
                guardedRun()
            } catch (e: InterruptedException) {
                // fall thru and exit normally
            } finally {
                sGLThreadManager.threadExiting(this)
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private fun stopEglSurfaceLocked() {
            if (mHaveEglSurface) {
                mHaveEglSurface = false
                mEglHelper!!.destroySurface()
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private fun stopEglContextLocked() {
            if (mHaveEglContext) {
                mEglHelper!!.finish()
                mHaveEglContext = false
                sGLThreadManager.releaseEglContextLocked(this)
            }
        }

        fun setup(): Boolean {
            while (true) {
                synchronized(sGLThreadManager) {
                    while (true) {

                        // Update the pause state.
                        var pausing = false
                        if (mPaused != mRequestPaused) {
                            pausing = mRequestPaused
                            mPaused = mRequestPaused
                            monitorNotifyAll(sGLThreadManager)
                            if (LOG_PAUSE_RESUME) {
                                Log.i("GLThread", "mPaused is now $mPaused tid=$id")
                            }
                        }

                        // Do we need to give up the EGL context?
                        if (mShouldReleaseEglContext) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "releasing EGL context because asked to tid=$id")
                            }
                            stopEglSurfaceLocked()
                            stopEglContextLocked()
                            mShouldReleaseEglContext = false
                            askedToReleaseEglContext = true
                        }

                        // Have we lost the EGL context?
                        if (lostEglContext) {
                            stopEglSurfaceLocked()
                            stopEglContextLocked()
                            lostEglContext = false
                        }

                        // When pausing, release the EGL surface:
                        if (pausing && mHaveEglSurface) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "releasing EGL surface because paused tid=$id")
                            }
                            stopEglSurfaceLocked()
                        }

                        // When pausing, optionally release the EGL Context:
                        if (pausing && mHaveEglContext) {
                            val view = mGLSurfaceViewWeakRef.get()

                            val preserveEglContextOnPause = view != null && view.mPreserveEGLContextOnPause

                            if (LOG_SURFACE) {
                                Log.i(
                                    "GLThread", "preserveEglContextOnPause = "
                                            + preserveEglContextOnPause
                                            + " sGLThreadManager.shouldReleaseEGLContextWhenPausing() ="
                                            + sGLThreadManager.shouldReleaseEGLContextWhenPausing()
                                )
                            }
                            if (!preserveEglContextOnPause || sGLThreadManager.shouldReleaseEGLContextWhenPausing()) {
                                stopEglContextLocked()
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL context because paused tid=$id")
                                }
                            }
                        }

                        // When pausing, optionally terminate EGL:
                        if (pausing) {
                            if (sGLThreadManager.shouldTerminateEGLWhenPausing()) {
                                mEglHelper!!.finish()
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "terminating EGL because paused tid=$id")
                                }
                            }
                        }

                        // Have we lost the SurfaceView surface?
                        if ((!mHasSurface) && (!mWaitingForSurface)) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "noticed surfaceView surface lost tid=$id")
                            }
                            if (mHaveEglSurface) {
                                stopEglSurfaceLocked()
                            }
                            mWaitingForSurface = true
                            mSurfaceIsBad = false
                            monitorNotifyAll(sGLThreadManager)
                        }

                        // Have we acquired the surface view surface?
                        if (mHasSurface && mWaitingForSurface) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "noticed surfaceView surface acquired tid=$id")
                            }
                            mWaitingForSurface = false
                            monitorNotifyAll(sGLThreadManager)
                        }

                        if (doRenderNotification) {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "sending render notification tid=$id")
                            }
                            wantRenderNotification = false
                            doRenderNotification = false
                            mRenderComplete = true
                            monitorNotifyAll(sGLThreadManager)
                        }


                        // Ready to draw?
                        if (readyToDraw()) {

                            // If we don't have an EGL context, try to acquire one.
                            if (!mHaveEglContext) {
                                if (askedToReleaseEglContext) {
                                    askedToReleaseEglContext = false
                                } else if (sGLThreadManager.tryAcquireEglContextLocked(this)) {
                                    try {
                                        mEglHelper!!.start()
                                    } catch (t: RuntimeException) {
                                        sGLThreadManager.releaseEglContextLocked(this)
                                        throw t
                                    }
                                    mHaveEglContext = true
                                    createEglContext = true

                                    monitorNotifyAll(sGLThreadManager)
                                }
                            }

                            if (mHaveEglContext && !mHaveEglSurface) {
                                mHaveEglSurface = true
                                createEglSurface = true
                                createGlInterface = true
                                sizeChanged = true
                            }

                            if (mHaveEglSurface) {
                                if (mSizeChanged) {
                                    sizeChanged = true
                                    w = mWidth
                                    h = mHeight
                                    wantRenderNotification = true
                                    if (LOG_SURFACE) {
                                        Log.i(
                                            "GLThread",
                                            "noticing that we want render notification tid="
                                                    + id
                                        )
                                    }

                                    // Destroy and recreate the EGL surface.
                                    createEglSurface = true

                                    mSizeChanged = false
                                }
                                mRequestRender = false
                                monitorNotifyAll(sGLThreadManager)
                                break
                            }
                        }
                        try {
                            monitorWait(sGLThreadManager)
                        } catch (e: InterruptedException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }
                    }
                }


                if (event != null) {
                    event!!.run()
                    event = null
                    continue
                }

                if (createEglSurface) {
                    if (LOG_SURFACE) {
                        Log.w("GLThread", "egl createSurface")
                    }
                    if (mEglHelper!!.createSurface()) {
                        synchronized(sGLThreadManager) {
                            mFinishedCreatingEglSurface = true
                            monitorNotifyAll(sGLThreadManager)
                        }
                    } else {
                        synchronized(sGLThreadManager) {
                            mFinishedCreatingEglSurface = true
                            mSurfaceIsBad = true
                            monitorNotifyAll(sGLThreadManager)
                        }
                        continue
                    }
                    createEglSurface = false
                }

                if (createGlInterface) {
                    gl = mEglHelper!!.createGL() as GL10?

                    sGLThreadManager.checkGLDriver(gl)
                    createGlInterface = false
                }

                if (createEglContext) {
                    if (LOG_RENDERER) {
                        Log.w("GLThread", "onSurfaceCreated")
                    }
                    val view = mGLSurfaceViewWeakRef.get()
                    if (view != null) {
                        view.mRenderer!!.onSurfaceCreated(gl, mEglHelper!!.mEglConfig)
                    }
                    createEglContext = false
                }

                if (sizeChanged) {
                    if (LOG_RENDERER) {
                        Log.w("GLThread", "onSurfaceChanged($w, $h)")
                    }
                    val view = mGLSurfaceViewWeakRef.get()
                    if (view != null) {
                        view.mRenderer!!.onSurfaceChanged(gl, w, h)
                    }
                    sizeChanged = false
                }

                //Return true if can start drawing
                val view = mGLSurfaceViewWeakRef.get()
                return view != null
            }
        }

        fun swapBuffers() {
            //Log.i("GLThread", "swapBuffers");
            val swapError = mEglHelper!!.swap()
            when (swapError) {
                EGL10.EGL_SUCCESS -> {
                }
                EGL11.EGL_CONTEXT_LOST -> {
                    if (LOG_SURFACE) {
                        Log.i("GLThread", "egl context lost tid=$id")
                    }
                    lostEglContext = true
                }
                else -> {
                    // Other errors typically mean that the current surface is bad,
                    // probably because the SurfaceView surface has been destroyed,
                    // but we haven't been notified yet.
                    // Log the error to help developers understand why rendering stopped.
                    EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError)

                    synchronized(sGLThreadManager) {
                        mSurfaceIsBad = true
                        monitorNotifyAll(sGLThreadManager)
                    }
                }
            }

            if (wantRenderNotification) {
                doRenderNotification = true
            }
        }

        @Throws(InterruptedException::class)
        private fun guardedRun() {
            mEglHelper = EglHelper(mGLSurfaceViewWeakRef)
            mHaveEglContext = false
            mHaveEglSurface = false
            try {

                while (true) {
/*
                    synchronized (sGLThreadManager) {
                        while (true) {
                            if (mShouldExit) {
                                return;
                            }

                            if (!mEventQueue.isEmpty()) {
                                event = mEventQueue.remove(0);
                                break;
                            }
                            setup();

                            // By design, this is the only place in a GLThread thread where we wait().
                            if (LOG_THREADS) {
                                Log.i("GLThread", "waiting tid=" + getId()
                                        + " mHaveEglContext: " + mHaveEglContext
                                        + " mHaveEglSurface: " + mHaveEglSurface
                                        + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
                                        + " mPaused: " + mPaused
                                        + " mHasSurface: " + mHasSurface
                                        + " mSurfaceIsBad: " + mSurfaceIsBad
                                        + " mWaitingForSurface: " + mWaitingForSurface
                                        + " mWidth: " + mWidth
                                        + " mHeight: " + mHeight
                                        + " mRequestRender: " + mRequestRender
                                        + " mRenderMode: " + mRenderMode);
                            }
                            sGLThreadManager.wait();
                        }
                    } // end of synchronized(sGLThreadManager)
*/

                    setup()

                    if (LOG_RENDERER_DRAW_FRAME) {
                        Log.w("GLThread", "onDrawFrame tid=$id")
                    }
                    run {
                        val view = mGLSurfaceViewWeakRef.get()
                        if (view != null) {
                            view.mRenderer!!.onDrawFrame(gl)
                        }
                    }

/*
                    int swapError = mEglHelper.swap();
                    switch (swapError) {
                        case EGL10.EGL_SUCCESS:
                            break;
                        case EGL11.EGL_CONTEXT_LOST:
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "egl context lost tid=" + getId());
                            }
                            lostEglContext = true;
                            break;
                        default:
                            // Other errors typically mean that the current surface is bad,
                            // probably because the SurfaceView surface has been destroyed,
                            // but we haven't been notified yet.
                            // Log the error to help developers understand why rendering stopped.
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);

                            synchronized (sGLThreadManager) {
                                mSurfaceIsBad = true;
                                monitorNotifyAll(sGLThreadManager);
                            }
                            break;
                    }

                    if (wantRenderNotification) {
                        doRenderNotification = true;
                    }
*/
                }

            } finally {
                /*
                 * clean-up everything...
                 */
                synchronized(sGLThreadManager) {
                    stopEglSurfaceLocked()
                    stopEglContextLocked()
                }
            }
        }

        fun ableToDraw(): Boolean {
            return mHaveEglContext && mHaveEglSurface && readyToDraw()
        }

        private fun readyToDraw(): Boolean {
            return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                    && (mWidth > 0) && (mHeight > 0)
                    && (mRequestRender || (mRenderMode == RENDERMODE_CONTINUOUSLY))
        }

        fun getRenderMode(): Int {
            synchronized(sGLThreadManager) {
                return mRenderMode
            }
        }

        fun setRenderMode(renderMode: Int) {
            if (!((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTINUOUSLY))) {
                throw IllegalArgumentException("renderMode")
            }
            synchronized(sGLThreadManager) {
                mRenderMode = renderMode
                monitorNotifyAll(sGLThreadManager)
            }
        }

        fun requestRender() {
            synchronized(sGLThreadManager) {
                mRequestRender = true
                monitorNotifyAll(sGLThreadManager)
            }
        }

        fun surfaceCreated() {
            synchronized(sGLThreadManager) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated tid=$id")
                }
                mHasSurface = true
                mFinishedCreatingEglSurface = false
                monitorNotifyAll(sGLThreadManager)
                while (mWaitingForSurface
                    && !mFinishedCreatingEglSurface
                    && !mExited
                ) {
                    try {
                        monitorWait(sGLThreadManager)
                    } catch (e: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun surfaceDestroyed() {
            synchronized(sGLThreadManager) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed tid=$id")
                }
                mHasSurface = false
                monitorNotifyAll(sGLThreadManager)
                while ((!mWaitingForSurface) && (!mExited)) {
                    try {
                        monitorWait(sGLThreadManager)
                    } catch (e: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun onPause() {
            synchronized(sGLThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause tid=$id")
                }
                mRequestPaused = true
                monitorNotifyAll(sGLThreadManager)

                while ((!mExited) && (!mPaused)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onPause waiting for mPaused.")
                    }
                    try {
                        monitorWait(sGLThreadManager)
                    } catch (ex: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun onResume() {
            synchronized(sGLThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume tid=$id")
                }
                mRequestPaused = false
                mRequestRender = true
                mRenderComplete = false
                monitorNotifyAll(sGLThreadManager)

                while ((!mExited) && mPaused && (!mRenderComplete)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onResume waiting for !mPaused.")
                    }
                    try {
                        monitorWait(sGLThreadManager)
                    } catch (ex: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun onWindowResize(w: Int, h: Int) {
            synchronized(sGLThreadManager) {
                mWidth = w
                mHeight = h
                mSizeChanged = true
                mRequestRender = true
                mRenderComplete = false
                monitorNotifyAll(sGLThreadManager)

                // Wait for thread to react to resize and render a frame
                while (!mExited && !mPaused && !mRenderComplete
                    && ableToDraw()
                ) {
                    if (LOG_SURFACE) {
                        Log.i("Main thread", "onWindowResize waiting for render complete from tid=$id")
                    }
                    try {
                        monitorWait(sGLThreadManager)
                    } catch (ex: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized(sGLThreadManager) {
                mShouldExit = true
                monitorNotifyAll(sGLThreadManager)
                while (!mExited) {
                    try {
                        monitorWait(sGLThreadManager)
                    } catch (ex: InterruptedException) {
                        currentThread().interrupt()
                    }
                }
            }
        }

        fun requestReleaseEglContextLocked() {
            mShouldReleaseEglContext = true
            monitorNotifyAll(sGLThreadManager)
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         *
         * @param r the runnable to be run on the GL rendering thread.
         */
        fun queueEvent(r: Runnable?) {
            if (r == null) {
                throw IllegalArgumentException("r must not be null")
            }
            synchronized(sGLThreadManager) {
                mEventQueue.add(r)
                monitorNotifyAll(sGLThreadManager)
            }
        }

        // End of member variables protected by the sGLThreadManager monitor.

        fun forceSurfaceDestory() {
            //stopEglSurfaceLocked();
        }

        fun forceSurfaceCreation(): Boolean {
            //mEglHelper.createSurface();
            //mEglHelper.createGL();
            //Log.d("GLThread", "forceSurfaceCreation calling setup");
            val ret = setup()
            //Log.d("GLThread", "forceSurfaceCreation ..returned from setup");
            return ret
        }
    }

    internal class LogWriter : Writer() {

        private val mBuilder = StringBuilder()

        override fun close() {
            flushBuilder()
        }

        override fun flush() {
            flushBuilder()
        }

        override fun write(buf: CharArray, offset: Int, count: Int) {
            for (i in 0 until count) {
                val c = buf[offset + i]
                if (c == '\n') {
                    flushBuilder()
                } else {
                    mBuilder.append(c)
                }
            }
        }

        private fun flushBuilder() {
            if (mBuilder.isNotEmpty()) {
                Log.v("MyGLSurfaceView", mBuilder.toString())
                mBuilder.delete(0, mBuilder.length)
            }
        }
    }

    private class GLThreadManager {
        /**
         * This check was required for some pre-Android-3.0 hardware. Android 3.0 provides
         * support for hardware-accelerated views, therefore multiple EGL contexts are
         * supported on all Android 3.0+ EGL drivers.
         */
        private var mGLESVersionCheckComplete = false
        private var mGLESVersion = 0
        private var mGLESDriverCheckComplete = false
        private var mMultipleGLESContextsAllowed = false
        private var mLimitedGLESContexts = false
        private var mEglOwner: GLThread? = null

        @Synchronized
        fun threadExiting(thread: GLThread) {
            if (LOG_THREADS) {
                Log.i("GLThread", "exiting tid=" + thread.id)
            }
            thread.mExited = true
            if (mEglOwner === thread) {
                mEglOwner = null
            }
            monitorNotifyAll(this)
        }

        /*
         * Tries once to acquire the right to use an EGL
         * context. Does not block. Requires that we are already
         * in the sGLThreadManager monitor when this is called.
         *
         * @return true if the right to use an EGL context was acquired.
         */
        fun tryAcquireEglContextLocked(thread: GLThread): Boolean {
            if (mEglOwner === thread || mEglOwner == null) {
                mEglOwner = thread
                monitorNotifyAll(this)
                return true
            }
            checkGLESVersion()
            if (mMultipleGLESContextsAllowed) {
                return true
            }
            // Notify the owning thread that it should release the context.
            // TODO: implement a fairness policy. Currently
            // if the owning thread is drawing continuously it will just
            // reacquire the EGL context.
            if (mEglOwner != null) {
                mEglOwner!!.requestReleaseEglContextLocked()
            }
            return false
        }

        /*
         * Releases the EGL context. Requires that we are already in the
         * sGLThreadManager monitor when this is called.
         */
        fun releaseEglContextLocked(thread: GLThread) {
            if (mEglOwner === thread) {
                mEglOwner = null
            }
            monitorNotifyAll(this)
        }

        @Synchronized
        fun shouldReleaseEGLContextWhenPausing(): Boolean {
            // Release the EGL context when pausing even if
            // the hardware supports multiple EGL contexts.
            // Otherwise the device could run out of EGL contexts.
            return mLimitedGLESContexts
        }

        @Synchronized
        fun shouldTerminateEGLWhenPausing(): Boolean {
            checkGLESVersion()
            return !mMultipleGLESContextsAllowed
        }

        @Synchronized
        fun checkGLDriver(gl: GL10?) {
            if (!mGLESDriverCheckComplete) {
                checkGLESVersion()
                val renderer = gl!!.glGetString(GL10.GL_RENDERER)
                if (mGLESVersion < kGLES_20) {
                    mMultipleGLESContextsAllowed =
                        !renderer.startsWith(kMSM7K_RENDERER_PREFIX)
                    monitorNotifyAll(this)
                }
                mLimitedGLESContexts = !mMultipleGLESContextsAllowed
                if (LOG_SURFACE) {
                    Log.w(
                        TAG, "checkGLDriver renderer = \"" + renderer + "\" multipleContextsAllowed = "
                                + mMultipleGLESContextsAllowed
                                + " mLimitedGLESContexts = " + mLimitedGLESContexts
                    )
                }
                mGLESDriverCheckComplete = true
            }
        }

        private fun checkGLESVersion() {
            if (!mGLESVersionCheckComplete) {
                //mGLESVersion = SystemProperties.getInt("ro.opengles.version", ConfigurationInfo.GL_ES_VERSION_UNDEFINED);
                mGLESVersion = kGLES_20
                mMultipleGLESContextsAllowed = true
                if (LOG_SURFACE) {
                    Log.w(
                        TAG, "checkGLESVersion mGLESVersion =" +
                                " " + mGLESVersion + " mMultipleGLESContextsAllowed = " + true
                    )
                }
                mGLESVersionCheckComplete = true
            }
        }

        companion object {
            private const val kGLES_20 = 0x20000
            private const val kMSM7K_RENDERER_PREFIX = "Q3Dimension MSM7500 "
            private const val TAG = "GLThreadManager"
        }
    }

    private inner class DefaultContextFactory : EGLContextFactory {
        private val EGL_CONTEXT_CLIENT_VERSION = 0x3098

        override fun createContext(egl: EGL10?, display: EGLDisplay?, config: EGLConfig?): EGLContext? {
            val attrib_list = intArrayOf(
                EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                EGL10.EGL_NONE
            )

            return egl!!.eglCreateContext(
                display, config, EGL10.EGL_NO_CONTEXT,
                if (mEGLContextClientVersion != 0) attrib_list else null
            )
        }

        override fun destroyContext(
            egl: EGL10?, display: EGLDisplay?,
            context: EGLContext?
        ) {
            if (!egl!!.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:$display context: $context")
                if (LOG_THREADS) {
                    Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().id)
                }
                EglHelper.throwEglException("eglDestroyContext", egl.eglGetError())
            }
        }
    }

    private abstract inner class BaseConfigChooser(configSpec: IntArray) : EGLConfigChooser {
        var mConfigSpec: IntArray = filterConfigSpec(configSpec)

        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val num_config = IntArray(1)
            if (!egl.eglChooseConfig(
                    display, mConfigSpec, null, 0,
                    num_config
                )
            ) {
                throw IllegalArgumentException("eglChooseConfig failed")
            }

            val numConfigs = num_config[0]

            if (numConfigs <= 0) {
                throw IllegalArgumentException(
                    "No configs match configSpec"
                )
            }

            val configs = arrayOfNulls<EGLConfig>(numConfigs)
            if (!egl.eglChooseConfig(
                    display, mConfigSpec, configs, numConfigs,
                    num_config
                )
            ) {
                throw IllegalArgumentException("eglChooseConfig#2 failed")
            }
            val config = chooseConfig(egl, display, configs)
                ?: throw IllegalArgumentException("No config chosen")
            return config
        }

        abstract fun chooseConfig(
            egl: EGL10, display: EGLDisplay,
            configs: Array<EGLConfig?>
        ): EGLConfig?

        private fun filterConfigSpec(configSpec: IntArray): IntArray {
            if (mEGLContextClientVersion != 2) {
                return configSpec
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            val len = configSpec.size
            val newConfigSpec = IntArray(len + 2)
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
            newConfigSpec[len] = 4 /* EGL_OPENGL_ES2_BIT */
            newConfigSpec[len + 1] = EGL10.EGL_NONE
            return newConfigSpec
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    private open inner class ComponentSizeChooser(
        redSize: Int, greenSize: Int, blueSize: Int,
        alphaSize: Int, depthSize: Int, stencilSize: Int
    ) : BaseConfigChooser(
        intArrayOf(
            EGL10.EGL_RED_SIZE, redSize,
            EGL10.EGL_GREEN_SIZE, greenSize,
            EGL10.EGL_BLUE_SIZE, blueSize,
            EGL10.EGL_ALPHA_SIZE, alphaSize,
            EGL10.EGL_DEPTH_SIZE, depthSize,
            EGL10.EGL_STENCIL_SIZE, stencilSize,
            EGL10.EGL_NONE
        )
    ) {
        // Subclasses can adjust these values:
        var mRedSize: Int = redSize
        var mGreenSize: Int = greenSize
        var mBlueSize: Int = blueSize
        var mAlphaSize: Int = alphaSize
        var mDepthSize: Int = depthSize
        var mStencilSize: Int = stencilSize
        private val mValue: IntArray = IntArray(1)

        override fun chooseConfig(
            egl: EGL10, display: EGLDisplay,
            configs: Array<EGLConfig?>
        ): EGLConfig? {
            for (config in configs) {
                val d = findConfigAttrib(
                    egl, display, config,
                    EGL10.EGL_DEPTH_SIZE, 0
                )
                val s = findConfigAttrib(
                    egl, display, config,
                    EGL10.EGL_STENCIL_SIZE, 0
                )
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    val r = findConfigAttrib(
                        egl, display, config,
                        EGL10.EGL_RED_SIZE, 0
                    )
                    val g = findConfigAttrib(
                        egl, display, config,
                        EGL10.EGL_GREEN_SIZE, 0
                    )
                    val b = findConfigAttrib(
                        egl, display, config,
                        EGL10.EGL_BLUE_SIZE, 0
                    )
                    val a = findConfigAttrib(
                        egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0
                    )
                    if ((r == mRedSize) && (g == mGreenSize)
                        && (b == mBlueSize) && (a == mAlphaSize)
                    ) {
                        return config
                    }
                }
            }
            return null
        }

        private fun findConfigAttrib(
            egl: EGL10, display: EGLDisplay,
            config: EGLConfig?, attribute: Int, defaultValue: Int
        ): Int {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0]
            }
            return defaultValue
        }
    }

    /**
     * This class will choose a RGB_888 surface with
     * or without a depth buffer.
     */
    private inner class SimpleEGLConfigChooser(withDepthBuffer: Boolean) :
        ComponentSizeChooser(8, 8, 8, 0, if (withDepthBuffer) 16 else 0, 0)

    companion object {
        /**
         * The renderer only renders
         * when the surface is created, or when [requestRender] is called.
         */
        const val RENDERMODE_WHEN_DIRTY = 0

        /**
         * The renderer is called
         * continuously to re-render the scene.
         */
        const val RENDERMODE_CONTINUOUSLY = 1

        /**
         * Check glError() after every GL call and throw an exception if glError indicates
         * that an error has occurred.
         */
        const val DEBUG_CHECK_GL_ERROR = 1

        /**
         * Log GL calls to the system log at "verbose" level with tag "MyGLSurfaceView".
         */
        const val DEBUG_LOG_GL_CALLS = 2

        private const val TAG = "MyGLSurfaceView"
        private const val LOG_ATTACH_DETACH = false
        private const val LOG_THREADS = true
        private const val LOG_PAUSE_RESUME = true
        private const val LOG_SURFACE = true
        private const val LOG_RENDERER = true
        private const val LOG_RENDERER_DRAW_FRAME = false
        private const val LOG_EGL = true
        private val sGLThreadManager = GLThreadManager()

        // Helpers to access the Object monitor methods on an arbitrary lock,
        // since Kotlin hides java.lang.Object.wait()/notifyAll() on Any.
        @Throws(InterruptedException::class)
        private fun monitorWait(lock: Any) {
            (lock as java.lang.Object).wait()
        }

        private fun monitorNotifyAll(lock: Any) {
            (lock as java.lang.Object).notifyAll()
        }
    }
}
