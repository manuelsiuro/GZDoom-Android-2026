@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package org.libsdl.app

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.AbsoluteLayout
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

/**
 * SDL Activity
 */
open class SDLActivity : Activity() {

    // Handler for the messages
    var commandHandler: Handler = SDLCommandHandler()

    companion object {
        const val COMMAND_USER = 0x8000

        // Messages from the SDLMain thread
        const val COMMAND_CHANGE_TITLE = 1
        const val COMMAND_UNUSED = 2
        const val COMMAND_TEXTEDIT_HIDE = 3

        private const val TAG = "SDL"

        // Keep track of the paused state
        @JvmField
        var mIsPaused = false

        @JvmField
        var mIsSurfaceReady = false

        @JvmField
        var mHasFocus = true

        // Main components
        @JvmField
        var mSingleton: SDLActivity? = null

        @JvmField
        var mSurface: SDLSurface? = null

        @JvmField
        var mTextEdit: View? = null

        @JvmField
        var mLayout: ViewGroup? = null

        // This is what SDL runs in. It invokes SDL_main(), eventually
        @JvmField
        var mSDLThread: Thread? = null

        // Audio
        @JvmField
        var mAudioThread: Thread? = null

        @JvmField
        var mAudioTrack: AudioTrack? = null

        // EGL objects
        @JvmField
        var mEGLContext: EGLContext? = null

        @JvmField
        var mEGLSurface: EGLSurface? = null

        /*
        // Load the .so
        static {
            System.loadLibrary("SDL2");
            //System.loadLibrary("SDL2_image");
            //System.loadLibrary("SDL2_mixer");
            //System.loadLibrary("SDL2_net");
            //System.loadLibrary("SDL2_ttf");
            System.loadLibrary("main");
        }
    */
        @JvmField
        var mEGLDisplay: EGLDisplay? = null

        @JvmField
        var mEGLConfig: EGLConfig? = null

        @JvmField
        var mGLMajor = 0

        @JvmField
        var mGLMinor = 0

        @JvmStatic
        fun loadSDL() {
            try {
                Log.i("JNI", "Trying to load SDL.so")

                System.loadLibrary("SDL2")
                System.loadLibrary("SDL2_mixer")
                System.loadLibrary("SDL2_image")
            } catch (ule: UnsatisfiedLinkError) {
                Log.e("JNI", "WARNING: Could not load SDL.so: $ule")
            }
        }

        /**
         * Called by onPause or surfaceDestroyed. Even if surfaceDestroyed
         * is the first to be called, mIsSurfaceReady should still be set
         * to 'true' during the call to onPause (in a usual scenario).
         */
        @JvmStatic
        fun handlePause() {
            if (!mIsPaused && mIsSurfaceReady) {
                mIsPaused = true
                nativePause()
                mSurface!!.enableSensor(Sensor.TYPE_ACCELEROMETER, false)
            }
        }

        /**
         * Called by onResume or surfaceCreated. An actual resume should be done only when the surface is ready.
         * Note: Some Android variants may send multiple surfaceChanged events, so we don't need to resume
         * every time we get one of those events, only if it comes after surfaceDestroyed
         */
        @JvmStatic
        fun handleResume() {
            if (mIsPaused && mIsSurfaceReady && mHasFocus) {
                mIsPaused = false
                nativeResume()
                mSurface!!.enableSensor(Sensor.TYPE_ACCELEROMETER, true)
            }
        }

        // C functions we call
        @JvmStatic
        external fun nativeInit()

        @JvmStatic
        external fun nativeLowMemory()

        @JvmStatic
        external fun nativeQuit()

        @JvmStatic
        external fun nativePause()

        @JvmStatic
        external fun nativeResume()

        @JvmStatic
        external fun onNativeResize(x: Int, y: Int, format: Int)

        @JvmStatic
        external fun onNativeKeyDown(keycode: Int)

        @JvmStatic
        external fun onNativeKeyUp(keycode: Int)

        @JvmStatic
        external fun onNativeKeyboardFocusLost()

        @JvmStatic
        external fun onNativeTouch(
            touchDevId: Int, pointerFingerId: Int,
            action: Int, x: Float,
            y: Float, p: Float
        )

        @JvmStatic
        external fun onNativeAccel(x: Float, y: Float, z: Float)

        @JvmStatic
        fun createGLContext(majorVersion: Int, minorVersion: Int, attribs: IntArray): Boolean {
            return true
            //return initEGL(majorVersion, minorVersion, attribs);
        }

        @JvmStatic
        fun deleteGLContext() {
            if (mEGLDisplay != null && mEGLContext != EGL10.EGL_NO_CONTEXT) {
                val egl = EGLContext.getEGL() as EGL10
                egl.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                egl.eglDestroyContext(mEGLDisplay, mEGLContext)
                mEGLContext = EGL10.EGL_NO_CONTEXT

                if (mEGLSurface != EGL10.EGL_NO_SURFACE) {
                    egl.eglDestroySurface(mEGLDisplay, mEGLSurface)
                    mEGLSurface = EGL10.EGL_NO_SURFACE
                }
            }
        }

        @JvmStatic
        fun flipBuffers() {
            flipEGL()
        }

        @JvmStatic
        fun setActivityTitle(title: String): Boolean {
            // Called from SDLMain() thread and can't directly affect the view
            return true
            //return mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
        }

        @JvmStatic
        fun sendMessage(command: Int, param: Int): Boolean {
            return true
            // return mSingleton.sendCommand(command, Integer.valueOf(param));
        }

        @JvmStatic
        fun getContext(): Context? {
            return mSingleton
        }

        @JvmStatic
        fun showTextInput(x: Int, y: Int, w: Int, h: Int): Boolean {
            return false
            // Transfer the task to the main thread as a Runnable
            //return mSingleton.commandHandler.post(new ShowTextInputTask(x, y, w, h));
        }

        // EGL functions
        @JvmStatic
        fun initEGL(majorVersion: Int, minorVersion: Int, attribs: IntArray): Boolean {
            try {
                val egl = EGLContext.getEGL() as EGL10

                if (mEGLDisplay == null) {
                    mEGLDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
                    val version = IntArray(2)
                    egl.eglInitialize(mEGLDisplay, version)
                }

                if (mEGLDisplay != null && mEGLContext == EGL10.EGL_NO_CONTEXT) {
                    // No current GL context exists, we will create a new one.
                    Log.v("SDL", "Starting up OpenGL ES $majorVersion.$minorVersion")
                    val configs = arrayOfNulls<EGLConfig>(128)
                    val num_config = IntArray(1)
                    if (!egl.eglChooseConfig(mEGLDisplay, attribs, configs, 1, num_config) || num_config[0] == 0) {
                        Log.e("SDL", "No EGL config available")
                        return false
                    }
                    var config: EGLConfig? = null
                    var bestdiff = -1
                    var bitdiff: Int
                    val value = IntArray(1)

                    // eglChooseConfig returns a number of configurations that match or exceed the requested attribs.
                    // From those, we select the one that matches our requirements more closely
                    Log.v("SDL", "Got " + num_config[0] + " valid modes from egl")
                    for (i in 0 until num_config[0]) {
                        bitdiff = 0
                        // Go through some of the attributes and compute the bit difference between what we want and what we get.
                        var j = 0
                        while (true) {
                            if (attribs[j] == EGL10.EGL_NONE)
                                break

                            if (attribs[j + 1] != EGL10.EGL_DONT_CARE && (attribs[j] == EGL10.EGL_RED_SIZE ||
                                        attribs[j] == EGL10.EGL_GREEN_SIZE ||
                                        attribs[j] == EGL10.EGL_BLUE_SIZE ||
                                        attribs[j] == EGL10.EGL_ALPHA_SIZE ||
                                        attribs[j] == EGL10.EGL_DEPTH_SIZE ||
                                        attribs[j] == EGL10.EGL_STENCIL_SIZE)
                            ) {
                                egl.eglGetConfigAttrib(mEGLDisplay, configs[i], attribs[j], value)
                                bitdiff += value[0] - attribs[j + 1] // value is always >= attrib
                            }
                            j += 2
                        }

                        if (bitdiff < bestdiff || bestdiff == -1) {
                            config = configs[i]
                            bestdiff = bitdiff
                        }

                        if (bitdiff == 0) break // we found an exact match!
                    }

                    Log.d("SDL", "Selected mode with a total bit difference of $bestdiff")

                    mEGLConfig = config
                    mGLMajor = majorVersion
                    mGLMinor = minorVersion
                }

                return createEGLSurface()

            } catch (e: Exception) {
                Log.v("SDL", e.toString() + "")
                for (s in e.stackTrace) {
                    Log.v("SDL", s.toString())
                }
                return false
            }
        }

        @JvmStatic
        fun createEGLContext(): Boolean {
            val egl = EGLContext.getEGL() as EGL10
            val EGL_CONTEXT_CLIENT_VERSION = 0x3098
            val contextAttrs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, mGLMajor, EGL10.EGL_NONE)
            mEGLContext = egl.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttrs)
            if (mEGLContext == EGL10.EGL_NO_CONTEXT) {
                Log.e("SDL", "Couldn't create context")
                return false
            }
            return true
        }

        @JvmStatic
        fun createEGLSurface(): Boolean {
            if (mEGLDisplay != null && mEGLConfig != null) {
                val egl = EGLContext.getEGL() as EGL10
                if (mEGLContext == EGL10.EGL_NO_CONTEXT) createEGLContext()

                if (mEGLSurface == EGL10.EGL_NO_SURFACE) {
                    Log.v("SDL", "Creating new EGL Surface")
                    mEGLSurface = egl.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, mSurface, null)
                    if (mEGLSurface == EGL10.EGL_NO_SURFACE) {
                        Log.e("SDL", "Couldn't create surface")
                        return false
                    }
                } else Log.v("SDL", "EGL Surface remains valid")

                if (egl.eglGetCurrentContext() != mEGLContext) {
                    if (!egl.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                        Log.e("SDL", "Old EGL Context doesnt work, trying with a new one")
                        // TODO: Notify the user via a message that the old context could not be restored, and that textures need to be manually restored.
                        createEGLContext()
                        if (!egl.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                            Log.e("SDL", "Failed making EGL Context current")
                            return false
                        }
                    } else Log.v("SDL", "EGL Context made current")
                } else Log.v("SDL", "EGL Context remains current")

                return true
            } else {
                Log.e("SDL", "Surface creation failed, display = $mEGLDisplay, config = $mEGLConfig")
                return false
            }
        }

        // Java functions called from C

        // EGL buffer flip
        @JvmStatic
        fun flipEGL() {
            try {
                val egl = EGLContext.getEGL() as EGL10

                egl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null)

                // drawing here

                egl.eglWaitGL()

                egl.eglSwapBuffers(mEGLDisplay, mEGLSurface)

            } catch (e: Exception) {
                Log.v("SDL", "flipEGL(): $e")
                for (s in e.stackTrace) {
                    Log.v("SDL", s.toString())
                }
            }
        }

        // Audio
        @JvmStatic
        fun audioInit(sampleRate: Int, is16Bit: Boolean, isStereo: Boolean, desiredFrames: Int): Int {
            val channelConfig =
                if (isStereo) AudioFormat.CHANNEL_CONFIGURATION_STEREO else AudioFormat.CHANNEL_CONFIGURATION_MONO
            val audioFormat =
                if (is16Bit) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT
            val frameSize = (if (isStereo) 2 else 1) * (if (is16Bit) 2 else 1)

            Log.v(
                "SDL",
                "SDL audio: wanted " + (if (isStereo) "stereo" else "mono") + " " + (if (is16Bit) "16-bit" else "8-bit") + " " + (sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer"
            )

            // Let the user pick a larger buffer if they really want -- but ye
            // gods they probably shouldn't, the minimums are horrifyingly high
            // latency already
            val frames = Math.max(
                desiredFrames,
                (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize
            )

            if (mAudioTrack == null) {
                mAudioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig, audioFormat, frames * frameSize, AudioTrack.MODE_STREAM
                )

                // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
                // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
                // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()

                if (mAudioTrack!!.state != AudioTrack.STATE_INITIALIZED) {
                    Log.e("SDL", "Failed during initialization of Audio Track")
                    mAudioTrack = null
                    return -1
                }

                mAudioTrack!!.play()
            }

            Log.v(
                "SDL",
                "SDL audio: got " + (if (mAudioTrack!!.channelCount >= 2) "stereo" else "mono") + " " + (if (mAudioTrack!!.audioFormat == AudioFormat.ENCODING_PCM_16BIT) "16-bit" else "8-bit") + " " + (mAudioTrack!!.sampleRate / 1000f) + "kHz, " + frames + " frames buffer"
            )

            return 0
        }

        @JvmStatic
        fun audioWriteShortBuffer(buffer: ShortArray) {
            //Log.d("SDL","audioWriteShortBuffer");
            var i = 0
            while (i < buffer.size) {
                val result = mAudioTrack!!.write(buffer, i, buffer.size - i)
                if (result > 0) {
                    i += result
                } else if (result == 0) {
                    try {
                        Log.d("SDL", "audioWriteShortBuffer sleep")
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        // Nom nom
                    }
                } else {
                    Log.w("SDL", "SDL audio: error return from write(short)")
                    return
                }
            }
        }

        @JvmStatic
        fun audioWriteByteBuffer(buffer: ByteArray) {
            //Log.d("SDL","audioWriteByteBuffer");
            var i = 0
            while (i < buffer.size) {
                val result = mAudioTrack!!.write(buffer, i, buffer.size - i)
                if (result > 0) {
                    i += result
                } else if (result == 0) {
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        // Nom nom
                    }
                } else {
                    Log.w("SDL", "SDL audio: error return from write(byte)")
                    return
                }
            }
        }

        @JvmStatic
        fun audioQuit() {
            if (mAudioTrack != null) {
                mAudioTrack!!.stop()
                mAudioTrack = null
            }
        }
    }

    // Setup
    override fun onCreate(savedInstanceState: Bundle?) {
        //Log.v("SDL", "onCreate()");
        super.onCreate(savedInstanceState)

        // So we can call stuff from static callbacks
        mSingleton = this

        // Set up the surface
        mEGLSurface = EGL10.EGL_NO_SURFACE
        mSurface = SDLSurface(application)
        mEGLContext = EGL10.EGL_NO_CONTEXT

        mLayout = AbsoluteLayout(this)
        mLayout!!.addView(mSurface)

        setContentView(mLayout)
    }

    // Events
    override fun onPause() {
        Log.v("SDL", "onPause()")
        super.onPause()
        handlePause()
    }

    override fun onResume() {
        Log.v("SDL", "onResume()")
        super.onResume()
        handleResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.v("SDL", "onWindowFocusChanged(): $hasFocus")

        mHasFocus = hasFocus
        if (hasFocus) {
            handleResume()
        }
    }

    override fun onLowMemory() {
        Log.v("SDL", "onLowMemory()")
        super.onLowMemory()
        nativeLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v("SDL", "onDestroy()")
        // Send a quit message to the application
        nativeQuit()

        // Now wait for the SDL thread to quit
        if (mSDLThread != null) {
            try {
                mSDLThread!!.join()
            } catch (e: Exception) {
                Log.v("SDL", "Problem stopping thread: $e")
            }
            mSDLThread = null

            //Log.v("SDL", "Finished waiting for SDL thread");
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        // Ignore volume keys so they're handled by Android
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP
        ) {
            return false
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * This method is called by SDL if SDL did not handle a message itself.
     * This happens if a received message contains an unsupported command.
     * Method can be overwritten to handle Messages in a different class.
     *
     * @param command the command of the message.
     * @param param   the parameter of the message. May be null.
     * @return if the message was handled in overridden method.
     */
    protected open fun onUnhandledMessage(command: Int, param: Any?): Boolean {
        return false
    }

    // Send a message from the SDLMain thread
    fun sendCommand(command: Int, data: Any?): Boolean {
        val msg = commandHandler.obtainMessage()
        msg.arg1 = command
        msg.obj = data
        return commandHandler.sendMessage(msg)
    }

    /**
     * A Handler class for Messages from native SDL applications.
     * It uses current Activities as target (e.g. for the title).
     * static to prevent implicit references to enclosing object.
     */
    protected class SDLCommandHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val context = getContext()
            if (context == null) {
                Log.e(TAG, "error handling message, getContext() returned null")
                return
            }
            when (msg.arg1) {
                COMMAND_CHANGE_TITLE -> {
                    if (context is Activity) {
                        context.title = msg.obj as String
                    } else {
                        Log.e(TAG, "error handling message, getContext() returned no Activity")
                    }
                }
                COMMAND_TEXTEDIT_HIDE -> {
                    if (mTextEdit != null) {
                        mTextEdit!!.visibility = View.GONE

                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(mTextEdit!!.windowToken, 0)
                    }
                }
                else -> {
                    if (context is SDLActivity && !context.onUnhandledMessage(msg.arg1, msg.obj)) {
                        Log.e(TAG, "error handling message, command is " + msg.arg1)
                    }
                }
            }
        }
    }

    class ShowTextInputTask(var x: Int, var y: Int, var w: Int, var h: Int) : Runnable {
        /*
         * This is used to regulate the pan&scan method to have some offset from
         * the bottom edge of the input region and the top edge of an input
         * method (soft keyboard)
         */

        override fun run() {
            val params = AbsoluteLayout.LayoutParams(
                w, h + HEIGHT_PADDING, x, y
            )

            if (mTextEdit == null) {
                mTextEdit = DummyEdit(getContext())

                mLayout!!.addView(mTextEdit, params)
            } else {
                mTextEdit!!.layoutParams = params
            }

            mTextEdit!!.visibility = View.VISIBLE
            mTextEdit!!.requestFocus()

            val imm = getContext()!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(mTextEdit, 0)
        }

        companion object {
            const val HEIGHT_PADDING = 15
        }
    }
}

/**
 * Simple nativeInit() runnable
 */
internal class SDLMain : Runnable {
    override fun run() {
        // Runs SDL_main()
        SDLActivity.nativeInit()

        //Log.v("SDL", "SDL thread terminated");
    }
}

/**
 * SDLSurface. This is what we draw on, so we need to know when it's created
 * in order to do anything useful.
 *
 *
 * Because of this, that's where we set up the SDL thread
 */
class SDLSurface(context: Context) : SurfaceView(context), SurfaceHolder.Callback,
    View.OnKeyListener, View.OnTouchListener, SensorEventListener {

    // Startup
    init {
        holder.addCallback(this)

        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        setOnKeyListener(this)
        setOnTouchListener(this)

        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Some arbitrary defaults to avoid a potential division by zero
        mWidth = 1.0f
        mHeight = 1.0f
    }

    // Called when we have a valid drawing surface
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.v("SDL", "surfaceCreated()")
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU)
        // Set mIsSurfaceReady to 'true' *before* any call to handleResume
        SDLActivity.mIsSurfaceReady = true
    }

    // Called when we lose the surface
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.v("SDL", "surfaceDestroyed()")
        // Call this *before* setting mIsSurfaceReady to 'false'
        SDLActivity.handlePause()
        SDLActivity.mIsSurfaceReady = false

        /* We have to clear the current context and destroy the egl surface here
         * Otherwise there's BAD_NATIVE_WINDOW errors coming from eglCreateWindowSurface on resume
         * Ref: http://stackoverflow.com/questions/8762589/eglcreatewindowsurface-on-ics-and-switching-from-2d-to-3d
         */

        val egl = EGLContext.getEGL() as EGL10
        egl.eglMakeCurrent(
            SDLActivity.mEGLDisplay,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        egl.eglDestroySurface(SDLActivity.mEGLDisplay, SDLActivity.mEGLSurface)
        SDLActivity.mEGLSurface = EGL10.EGL_NO_SURFACE
    }

    // Called when the surface is resized
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int, width: Int, height: Int
    ) {
        Log.v("SDL", "surfaceChanged()")

        var sdlFormat = 0x15151002 // SDL_PIXELFORMAT_RGB565 by default
        when (format) {
            PixelFormat.A_8 -> Log.v("SDL", "pixel format A_8")
            PixelFormat.LA_88 -> Log.v("SDL", "pixel format LA_88")
            PixelFormat.L_8 -> Log.v("SDL", "pixel format L_8")
            PixelFormat.RGBA_4444 -> {
                Log.v("SDL", "pixel format RGBA_4444")
                sdlFormat = 0x15421002 // SDL_PIXELFORMAT_RGBA4444
            }
            PixelFormat.RGBA_5551 -> {
                Log.v("SDL", "pixel format RGBA_5551")
                sdlFormat = 0x15441002 // SDL_PIXELFORMAT_RGBA5551
            }
            PixelFormat.RGBA_8888 -> {
                Log.v("SDL", "pixel format RGBA_8888")
                sdlFormat = 0x16462004 // SDL_PIXELFORMAT_RGBA8888
            }
            PixelFormat.RGBX_8888 -> {
                Log.v("SDL", "pixel format RGBX_8888")
                sdlFormat = 0x16261804 // SDL_PIXELFORMAT_RGBX8888
            }
            PixelFormat.RGB_332 -> {
                Log.v("SDL", "pixel format RGB_332")
                sdlFormat = 0x14110801 // SDL_PIXELFORMAT_RGB332
            }
            PixelFormat.RGB_565 -> {
                Log.v("SDL", "pixel format RGB_565")
                sdlFormat = 0x15151002 // SDL_PIXELFORMAT_RGB565
            }
            PixelFormat.RGB_888 -> {
                Log.v("SDL", "pixel format RGB_888")
                // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
                sdlFormat = 0x16161804 // SDL_PIXELFORMAT_RGB888
            }
            else -> Log.v("SDL", "pixel format unknown $format")
        }

        mWidth = width.toFloat()
        mHeight = height.toFloat()
        SDLActivity.onNativeResize(width, height, sdlFormat)
        Log.v("SDL", "Window size:" + width + "x" + height)

        // Set mIsSurfaceReady to 'true' *before* making a call to handleResume
        SDLActivity.mIsSurfaceReady = true

        if (SDLActivity.mSDLThread == null) {
            // This is the entry point to the C app.
            // Start up the C app thread and enable sensor input for the first time

            SDLActivity.mSDLThread = Thread(SDLMain(), "SDLThread")
            enableSensor(Sensor.TYPE_ACCELEROMETER, true)
            SDLActivity.mSDLThread!!.start()
        } else {
            // The app already exists, we resume via handleResume
            // Multiple sequential calls to surfaceChanged are handled internally by handleResume

            SDLActivity.handleResume()
        }
    }

    // unused
    override fun onDraw(canvas: Canvas) {
    }

    // Key events
    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            //Log.v("SDL", "key down: " + keyCode);
            SDLActivity.onNativeKeyDown(keyCode)
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            //Log.v("SDL", "key up: " + keyCode);
            SDLActivity.onNativeKeyUp(keyCode)
            return true
        }

        return false
    }

    // Touch events
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val touchDevId = event.deviceId
        val pointerCount = event.pointerCount
        // touchId, pointerId, action, x, y, pressure
        val actionPointerIndex =
            (event.action and MotionEvent.ACTION_POINTER_ID_MASK) shr MotionEvent.ACTION_POINTER_ID_SHIFT /* API 8: event.getActionIndex(); */
        var pointerFingerId = event.getPointerId(actionPointerIndex)
        val action = event.action and MotionEvent.ACTION_MASK /* API 8: event.getActionMasked(); */

        var x = event.getX(actionPointerIndex) / mWidth
        var y = event.getY(actionPointerIndex) / mHeight
        var p = event.getPressure(actionPointerIndex)

        if (action == MotionEvent.ACTION_MOVE && pointerCount > 1) {
            // TODO send motion to every pointer if its position has
            // changed since prev event.
            for (i in 0 until pointerCount) {
                pointerFingerId = event.getPointerId(i)
                x = event.getX(i) / mWidth
                y = event.getY(i) / mHeight
                p = event.getPressure(i)
                SDLActivity.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p)
            }
        } else {
            SDLActivity.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p)
        }
        return true
    }

    // Sensor events
    fun enableSensor(sensortype: Int, enabled: Boolean) {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if (enabled) {
            mSensorManager!!.registerListener(
                this,
                mSensorManager!!.getDefaultSensor(sensortype),
                SensorManager.SENSOR_DELAY_GAME, null
            )
        } else {
            mSensorManager!!.unregisterListener(
                this,
                mSensorManager!!.getDefaultSensor(sensortype)
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // TODO
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            SDLActivity.onNativeAccel(
                event.values[0] / SensorManager.GRAVITY_EARTH,
                event.values[1] / SensorManager.GRAVITY_EARTH,
                event.values[2] / SensorManager.GRAVITY_EARTH
            )
        }
    }

    companion object {
        // Sensors
        @JvmStatic
        protected var mSensorManager: SensorManager? = null

        // Keep track of the surface size to normalize touch events
        @JvmStatic
        protected var mWidth = 0f

        @JvmStatic
        protected var mHeight = 0f
    }
}

/* This is a fake invisible editor view that receives the input and defines the
 * pan&scan region
 */
internal class DummyEdit(context: Context?) : View(context), View.OnKeyListener {
    var ic: InputConnection? = null

    init {
        isFocusableInTouchMode = true
        isFocusable = true
        setOnKeyListener(this)
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        // This handles the hardware keyboard input
        if (event.isPrintingKey) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                ic!!.commitText(event.unicodeChar.toChar().toString(), 1)
            }
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            SDLActivity.onNativeKeyDown(keyCode)
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            SDLActivity.onNativeKeyUp(keyCode)
            return true
        }

        return false
    }

    //
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        // As seen on StackOverflow: http://stackoverflow.com/questions/7634346/keyboard-hide-event
        // FIXME: Discussion at http://bugzilla.libsdl.org/show_bug.cgi?id=1639
        // FIXME: This is not a 100% effective solution to the problem of detecting if the keyboard is showing or not
        // FIXME: A more effective solution would be to change our Layout from AbsoluteLayout to Relative or Linear
        // FIXME: And determine the keyboard presence doing this: http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        // FIXME: An even more effective way would be if Android provided this out of the box, but where would the fun be in that :)
        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            if (SDLActivity.mTextEdit != null && SDLActivity.mTextEdit!!.visibility == View.VISIBLE) {
                SDLActivity.onNativeKeyboardFocusLost()
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        ic = SDLInputConnection(this, true)

        outAttrs.imeOptions = (EditorInfo.IME_FLAG_NO_EXTRACT_UI
                or 33554432) /* API 11: EditorInfo.IME_FLAG_NO_FULLSCREEN */

        return ic!!
    }
}

internal class SDLInputConnection(targetView: View, fullEditor: Boolean) :
    BaseInputConnection(targetView, fullEditor) {

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        /*
         * This handles the keycodes from soft keyboard (and IME-translated
         * input from hardkeyboard)
         */
        val keyCode = event.keyCode
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.isPrintingKey) {
                commitText(event.unicodeChar.toChar().toString(), 1)
            }
            SDLActivity.onNativeKeyDown(keyCode)
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            SDLActivity.onNativeKeyUp(keyCode)
            return true
        }
        return super.sendKeyEvent(event)
    }

    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        nativeCommitText(text.toString(), newCursorPosition)

        return super.commitText(text, newCursorPosition)
    }

    override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
        nativeSetComposingText(text.toString(), newCursorPosition)

        return super.setComposingText(text, newCursorPosition)
    }

    external fun nativeCommitText(text: String, newCursorPosition: Int)

    external fun nativeSetComposingText(text: String, newCursorPosition: Int)
}
