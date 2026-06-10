@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.beloko.libsdl

import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

/**
 * SDL Activity
 */
class SDLLib {

    companion object {

        private val threadLock = Any()

        @JvmField
        var resumed = false

        // Audio
        private var mAudioThread: Thread? = null
        private var mAudioTrack: AudioTrack? = null

        // EGL private objects
        private var mEGLContext: EGLContext? = null
        private var mEGLSurface: EGLSurface? = null
        private var mEGLDisplay: EGLDisplay? = null
        private var mEGLConfig: EGLConfig? = null
        private var mGLMajor = 0
        private var mGLMinor = 0

        @JvmStatic
        fun loadSDL() {
            try {
                Log.i("JNI", "Trying to load SDL.so")

                System.loadLibrary("SDL")
                System.loadLibrary("SDL_mixer")
                System.loadLibrary("SDL_image")
            } catch (ule: UnsatisfiedLinkError) {
                Log.e("JNI", "WARNING: Could not load SDL.so: $ule")
            }
        }

        // C functions we call
        @JvmStatic
        external fun nativeInit(launch: Boolean)

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
        external fun onNativeTouch(
            touchDevId: Int, pointerFingerId: Int,
            action: Int, x: Float,
            y: Float, p: Float
        )

        @JvmStatic
        external fun onNativeAccel(x: Float, y: Float, z: Float)

        // Java functions called from C
        @JvmStatic
        external fun nativeRunAudioThread()

        @JvmStatic
        fun createGLContext(majorVersion: Int, minorVersion: Int): Boolean {
            return true
        }

        @JvmStatic
        fun flipBuffers() {
        }

        @JvmStatic
        fun setActivityTitle(title: String) {
        }

        // Called when the surface is resized
        @JvmStatic
        fun surfaceChanged(format: Int, width: Int, height: Int) {
            Log.v("SDL", "surfaceChanged()")

            var sdlFormat = 0x85151002.toInt() // SDL_PIXELFORMAT_RGB565 by default
            when (format) {
                PixelFormat.A_8 -> Log.v("SDL", "pixel format A_8")
                PixelFormat.LA_88 -> Log.v("SDL", "pixel format LA_88")
                PixelFormat.L_8 -> Log.v("SDL", "pixel format L_8")
                PixelFormat.RGBA_4444 -> {
                    Log.v("SDL", "pixel format RGBA_4444")
                    sdlFormat = 0x85421002.toInt() // SDL_PIXELFORMAT_RGBA4444
                }
                PixelFormat.RGBA_5551 -> {
                    Log.v("SDL", "pixel format RGBA_5551")
                    sdlFormat = 0x85441002.toInt() // SDL_PIXELFORMAT_RGBA5551
                }
                PixelFormat.RGBA_8888 -> {
                    Log.v("SDL", "pixel format RGBA_8888")
                    sdlFormat = 0x86462004.toInt() // SDL_PIXELFORMAT_RGBA8888
                }
                PixelFormat.RGBX_8888 -> {
                    Log.v("SDL", "pixel format RGBX_8888")
                    sdlFormat = 0x86262004.toInt() // SDL_PIXELFORMAT_RGBX8888
                }
                PixelFormat.RGB_332 -> {
                    Log.v("SDL", "pixel format RGB_332")
                    sdlFormat = 0x84110801.toInt() // SDL_PIXELFORMAT_RGB332
                }
                PixelFormat.RGB_565 -> {
                    Log.v("SDL", "pixel format RGB_565")
                    sdlFormat = 0x85151002.toInt() // SDL_PIXELFORMAT_RGB565
                }
                PixelFormat.RGB_888 -> {
                    Log.v("SDL", "pixel format RGB_888")
                    // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
                    sdlFormat = 0x86161804.toInt() // SDL_PIXELFORMAT_RGB888
                }
                else -> Log.v("SDL", "pixel format unknown $format")
            }
            onNativeResize(width, height, sdlFormat)
            Log.v("SDL", "Window size:" + width + "x" + height)
        }

        // Audio
        @JvmStatic
        fun onPause() {
            resumed = false
        }

        @JvmStatic
        fun onResume() {
            resumed = true
            synchronized(threadLock) {
                (threadLock as Object).notifyAll()
            }
        }

        @JvmStatic
        fun audioInit(sampleRate: Int, is16Bit: Boolean, isStereo: Boolean, desiredFrames: Int): Any {
            val channelConfig =
                if (isStereo) AudioFormat.CHANNEL_CONFIGURATION_STEREO else AudioFormat.CHANNEL_CONFIGURATION_MONO
            val audioFormat =
                if (is16Bit) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT
            val frameSize = (if (isStereo) 2 else 1) * (if (is16Bit) 2 else 1)

            Log.v(
                "SDL",
                "SDL audio: wanted " + (if (isStereo) "stereo" else "mono") + " " + (if (is16Bit) "16-bit" else "8-bit") + " " + (sampleRate.toFloat() / 1000f) + "kHz, " + desiredFrames + " frames buffer"
            )

            // Let the user pick a larger buffer if they really want -- but ye
            // gods they probably shouldn't, the minimums are horrifyingly high
            // latency already
            val frames = Math.max(
                desiredFrames,
                (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize
            )

            mAudioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, frames * frameSize, AudioTrack.MODE_STREAM
            )

            audioStartThread()

            Log.v(
                "SDL",
                "SDL audio: got " + (if (mAudioTrack!!.channelCount >= 2) "stereo" else "mono")
                        + " " + (if (mAudioTrack!!.audioFormat == AudioFormat.ENCODING_PCM_16BIT) "16-bit" else "8-bit")
                        + " " + (mAudioTrack!!.sampleRate.toFloat() / 1000f) + "kHz, " + frames + " frames buffer"
            )

            val buf: Any = if (is16Bit) {
                ShortArray(frames * (if (isStereo) 2 else 1))
            } else {
                ByteArray(frames * (if (isStereo) 2 else 1))
            }
            return buf
        }

        @JvmStatic
        fun audioStartThread() {
            mAudioThread = Thread {
                mAudioTrack!!.play()
                nativeRunAudioThread()
            }

            // I'd take REALTIME if I could get it!
            mAudioThread!!.priority = Thread.MAX_PRIORITY
            mAudioThread!!.start()
        }

        @JvmStatic
        fun audioWriteShortBuffer(buffer: ShortArray) {
            if (!resumed)
                synchronized(threadLock) {
                    try {
                        if (mAudioTrack != null)
                            mAudioTrack!!.pause()
                        (threadLock as Object).wait()
                        if (mAudioTrack != null)
                            mAudioTrack!!.play()
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }

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
                    Log.w("SDL", "SDL audio: error return from write(short)")
                    return
                }
            }
        }

        @JvmStatic
        fun audioWriteByteBuffer(buffer: ByteArray) {
            if (!resumed)
                synchronized(threadLock) {
                    try {
                        if (mAudioTrack != null)
                            mAudioTrack!!.pause()
                        (threadLock as Object).wait()
                        if (mAudioTrack != null)
                            mAudioTrack!!.play()
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }

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
                    Log.w("SDL", "SDL audio: error return from write(short)")
                    return
                }
            }
        }

        @JvmStatic
        fun audioQuit() {
            if (mAudioThread != null) {
                try {
                    mAudioThread!!.join()
                } catch (e: Exception) {
                    Log.v("SDL", "Problem stopping audio thread: $e")
                }
                mAudioThread = null

                //Log.v("SDL", "Finished waiting for audio thread");
            }

            if (mAudioTrack != null) {
                mAudioTrack!!.stop()
                mAudioTrack = null
            }
        }
    }
}
