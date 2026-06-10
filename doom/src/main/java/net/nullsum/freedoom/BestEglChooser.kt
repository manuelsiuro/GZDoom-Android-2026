package net.nullsum.freedoom

import android.content.Context
import android.opengl.GLSurfaceView.EGLConfigChooser
import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class BestEglChooser(private val ctx: Context) : EGLConfigChooser {
    private val LOG = "BestEglChooser"

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        Log.i(LOG, "chooseConfig")

        val mConfigSpec = arrayOf(
            intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 24,
                EGL10.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_NONE,
            ),
            intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 24,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE,
            ),
            intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_NONE,
            ),
            intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE,
            ),
        )

        Log.i(LOG, "Number of specs to test: " + mConfigSpec.size)

        var specChosen = 0
        val numConfig = IntArray(1)
        var numConfigs = 0
        while (specChosen < mConfigSpec.size) {
            egl.eglChooseConfig(display, mConfigSpec[specChosen], null, 0, numConfig)
            if (numConfig[0] > 0) {
                numConfigs = numConfig[0]
                break
            }
            specChosen++
        }

        if (specChosen == mConfigSpec.size) {
            throw IllegalArgumentException("No EGL configs match configSpec")
        }

        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        egl.eglChooseConfig(display, mConfigSpec[specChosen], configs, numConfigs, numConfig)

        val eglConfigsString = StringBuilder()
        for (n in configs.indices) {
            Log.i(LOG, "found EGL config : " + printConfig(egl, display, configs[n]!!))
            eglConfigsString.append(n).append(": ").append(printConfig(egl, display, configs[n]!!)).append(",")
        }
        Log.i(LOG, eglConfigsString.toString())
        AppSettings.setStringOption(ctx, "egl_configs", eglConfigsString.toString())

        var selected = 0
        val override = AppSettings.getIntOption(ctx, "egl_config_selected", 0)
        if (override < configs.size) {
            selected = override
        }

        Log.i(LOG, "selected EGL config[$selected]: " + printConfig(egl, display, configs[selected]!!))

        return configs[selected]!!
    }

    private fun printConfig(egl: EGL10, display: EGLDisplay, config: EGLConfig): String {
        val r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0)
        val g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0)
        val b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0)
        val a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0)
        val d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0)
        val s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0)

        return String.format("rgba=%d%d%d%d z=%d sten=%d", r, g, b, a, d, s) +
            " n=" + findConfigAttrib(egl, display, config, EGL10.EGL_NATIVE_RENDERABLE, 0) +
            " b=" + findConfigAttrib(egl, display, config, EGL10.EGL_BUFFER_SIZE, 0) +
            String.format(" c=0x%04x", findConfigAttrib(egl, display, config, EGL10.EGL_CONFIG_CAVEAT, 0))
    }

    private fun findConfigAttrib(
        egl: EGL10,
        display: EGLDisplay,
        config: EGLConfig,
        attribute: Int,
        defaultValue: Int,
    ): Int {
        val value = IntArray(1)
        return if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
            value[0]
        } else {
            defaultValue
        }
    }
}
