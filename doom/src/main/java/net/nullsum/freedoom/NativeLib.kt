package net.nullsum.freedoom

import android.util.Log
import android.view.KeyEvent
import com.beloko.libsdl.SDLLib
import com.beloko.touchcontrols.ControlInterface

class NativeLib : ControlInterface {

    override fun quickCommand_if(command: String) {
        quickCommand(command)
    }

    override fun initTouchControls_if(pngPath: String?, width: Int, height: Int) {
    }

    override fun touchEvent_if(action: Int, pid: Int, x: Float, y: Float): Boolean =
        touchEvent(action, pid, x, y)

    override fun keyPress_if(down: Int, qkey: Int, unicode: Int) {
        keypress(down, qkey, unicode)
    }

    override fun doAction_if(state: Int, action: Int) {
        doAction(state, action)
    }

    override fun analogFwd_if(v: Float) {
        analogFwd(v)
    }

    override fun analogSide_if(v: Float) {
        analogSide(v)
    }

    override fun analogPitch_if(mode: Int, v: Float) {
        analogPitch(mode, v)
    }

    override fun analogYaw_if(mode: Int, v: Float) {
        analogYaw(mode, v)
    }

    override fun setTouchSettings_if(
        alpha: Float,
        strafe: Float,
        fwd: Float,
        pitch: Float,
        yaw: Float,
        other: Int,
    ) {
        setTouchSettings(alpha, strafe, fwd, pitch, yaw, other)
    }

    override fun mapKey(acode: Int, unicode: Int): Int {
        Log.d("TEST", "key = $acode $unicode")
        if (unicode == 95) { // Hack to make underscore work
            return SDL_SCANCODE_POWER
        } else if (acode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
            return SDL_SCANCODE_A + (acode - KeyEvent.KEYCODE_A)
        } else {
            when (acode) {
                KeyEvent.KEYCODE_TAB -> return SDL_SCANCODE_TAB
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> return SDL_SCANCODE_RETURN
                KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> return SDL_SCANCODE_ESCAPE
                KeyEvent.KEYCODE_SPACE -> return SDL_SCANCODE_SPACE
                KeyEvent.KEYCODE_DEL -> return SDL_SCANCODE_BACKSPACE
                KeyEvent.KEYCODE_DPAD_UP -> return SDL_SCANCODE_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> return SDL_SCANCODE_DOWN
                KeyEvent.KEYCODE_DPAD_LEFT -> return SDL_SCANCODE_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> return SDL_SCANCODE_RIGHT
                KeyEvent.KEYCODE_ALT_LEFT -> return SDL_SCANCODE_A
                KeyEvent.KEYCODE_ALT_RIGHT -> return SDL_SCANCODE_RALT
                KeyEvent.KEYCODE_CTRL_LEFT -> return SDL_SCANCODE_LCTRL
                KeyEvent.KEYCODE_CTRL_RIGHT -> return SDL_SCANCODE_RCTRL
                KeyEvent.KEYCODE_SHIFT_LEFT -> return SDL_SCANCODE_LSHIFT
                KeyEvent.KEYCODE_SHIFT_RIGHT -> return SDL_SCANCODE_RSHIFT
                KeyEvent.KEYCODE_F1 -> return SDL_SCANCODE_F1
                KeyEvent.KEYCODE_F2 -> return SDL_SCANCODE_F2
                KeyEvent.KEYCODE_F3 -> return SDL_SCANCODE_F3
                KeyEvent.KEYCODE_F4 -> return SDL_SCANCODE_F4
                KeyEvent.KEYCODE_F5 -> return SDL_SCANCODE_F5
                KeyEvent.KEYCODE_F6 -> return SDL_SCANCODE_F6
                KeyEvent.KEYCODE_F7 -> return SDL_SCANCODE_F7
                KeyEvent.KEYCODE_F8 -> return SDL_SCANCODE_F8
                KeyEvent.KEYCODE_F9 -> return SDL_SCANCODE_F9
                KeyEvent.KEYCODE_F10 -> return SDL_SCANCODE_F10
                KeyEvent.KEYCODE_F11 -> return SDL_SCANCODE_F11
                KeyEvent.KEYCODE_F12 -> return SDL_SCANCODE_F12
                KeyEvent.KEYCODE_FORWARD_DEL -> return SDL_SCANCODE_DELETE
                KeyEvent.KEYCODE_INSERT -> return SDL_SCANCODE_INSERT
                KeyEvent.KEYCODE_PAGE_UP -> return SDL_SCANCODE_PAGEUP
                KeyEvent.KEYCODE_PAGE_DOWN -> return SDL_SCANCODE_PAGEDOWN
                KeyEvent.KEYCODE_MOVE_HOME -> return SDL_SCANCODE_HOME
                KeyEvent.KEYCODE_MOVE_END -> return SDL_SCANCODE_END
                KeyEvent.KEYCODE_BREAK -> return SDL_SCANCODE_PRINTSCREEN
                KeyEvent.KEYCODE_PERIOD -> return SDL_SCANCODE_PERIOD
                KeyEvent.KEYCODE_COMMA -> return SDL_SCANCODE_COMMA
                KeyEvent.KEYCODE_SLASH -> return SDL_SCANCODE_SLASH
                KeyEvent.KEYCODE_0 -> return SDL_SCANCODE_0
                KeyEvent.KEYCODE_1 -> return SDL_SCANCODE_1
                KeyEvent.KEYCODE_2 -> return SDL_SCANCODE_2
                KeyEvent.KEYCODE_3 -> return SDL_SCANCODE_3
                KeyEvent.KEYCODE_4 -> return SDL_SCANCODE_4
                KeyEvent.KEYCODE_5 -> return SDL_SCANCODE_5
                KeyEvent.KEYCODE_6 -> return SDL_SCANCODE_6
                KeyEvent.KEYCODE_7 -> return SDL_SCANCODE_7
                KeyEvent.KEYCODE_8 -> return SDL_SCANCODE_8
                KeyEvent.KEYCODE_9 -> return SDL_SCANCODE_9
                KeyEvent.KEYCODE_MINUS -> return SDL_SCANCODE_MINUS
                KeyEvent.KEYCODE_EQUALS -> return SDL_SCANCODE_EQUALS
                else -> if (unicode < 128) return Character.toLowerCase(unicode)
            }
        }
        return 0
    }

    companion object {
        const val KEY_PRESS = 1
        const val KEY_RELEASE = 0
        const val SDL_SCANCODE_A = 4
        const val SDL_SCANCODE_B = 5
        const val SDL_SCANCODE_C = 6
        const val SDL_SCANCODE_D = 7
        const val SDL_SCANCODE_E = 8
        const val SDL_SCANCODE_F = 9
        const val SDL_SCANCODE_G = 10
        const val SDL_SCANCODE_H = 11
        const val SDL_SCANCODE_I = 12
        const val SDL_SCANCODE_J = 13
        const val SDL_SCANCODE_K = 14
        const val SDL_SCANCODE_L = 15
        const val SDL_SCANCODE_M = 16
        const val SDL_SCANCODE_N = 17
        const val SDL_SCANCODE_O = 18
        const val SDL_SCANCODE_P = 19
        const val SDL_SCANCODE_Q = 20
        const val SDL_SCANCODE_R = 21
        const val SDL_SCANCODE_S = 22
        const val SDL_SCANCODE_T = 23
        const val SDL_SCANCODE_U = 24
        const val SDL_SCANCODE_V = 25
        const val SDL_SCANCODE_W = 26
        const val SDL_SCANCODE_X = 27
        const val SDL_SCANCODE_Y = 28
        const val SDL_SCANCODE_Z = 29
        const val SDL_SCANCODE_1 = 30
        const val SDL_SCANCODE_2 = 31
        const val SDL_SCANCODE_3 = 32
        const val SDL_SCANCODE_4 = 33
        const val SDL_SCANCODE_5 = 34
        const val SDL_SCANCODE_6 = 35
        const val SDL_SCANCODE_7 = 36
        const val SDL_SCANCODE_8 = 37
        const val SDL_SCANCODE_9 = 38
        const val SDL_SCANCODE_0 = 39
        const val SDL_SCANCODE_RETURN = 40
        const val SDL_SCANCODE_ESCAPE = 41
        const val SDL_SCANCODE_BACKSPACE = 42
        const val SDL_SCANCODE_TAB = 43
        const val SDL_SCANCODE_SPACE = 44
        const val SDL_SCANCODE_MINUS = 45
        const val SDL_SCANCODE_EQUALS = 46
        const val SDL_SCANCODE_LEFTBRACKET = 47
        const val SDL_SCANCODE_RIGHTBRACKET = 48
        const val SDL_SCANCODE_COMMA = 54
        const val SDL_SCANCODE_PERIOD = 55
        const val SDL_SCANCODE_SLASH = 56
        const val SDL_SCANCODE_CAPSLOCK = 57
        const val SDL_SCANCODE_F1 = 58
        const val SDL_SCANCODE_F2 = 59
        const val SDL_SCANCODE_F3 = 60
        const val SDL_SCANCODE_F4 = 61
        const val SDL_SCANCODE_F5 = 62
        const val SDL_SCANCODE_F6 = 63
        const val SDL_SCANCODE_F7 = 64
        const val SDL_SCANCODE_F8 = 65
        const val SDL_SCANCODE_F9 = 66
        const val SDL_SCANCODE_F10 = 67
        const val SDL_SCANCODE_F11 = 68
        const val SDL_SCANCODE_F12 = 69
        const val SDL_SCANCODE_PRINTSCREEN = 70
        const val SDL_SCANCODE_SCROLLLOCK = 71
        const val SDL_SCANCODE_PAUSE = 72
        const val SDL_SCANCODE_INSERT = 73
        const val SDL_SCANCODE_HOME = 74
        const val SDL_SCANCODE_PAGEUP = 75
        const val SDL_SCANCODE_DELETE = 76
        const val SDL_SCANCODE_END = 77
        const val SDL_SCANCODE_PAGEDOWN = 78
        const val SDL_SCANCODE_RIGHT = 79
        const val SDL_SCANCODE_LEFT = 80
        const val SDL_SCANCODE_DOWN = 81
        const val SDL_SCANCODE_UP = 82

        // Hack this is overridden to give an underscore instead
        const val SDL_SCANCODE_POWER = 102
        const val SDL_SCANCODE_LCTRL = 224
        const val SDL_SCANCODE_LSHIFT = 225
        const val SDL_SCANCODE_LALT = 226

        /** < alt; option */
        const val SDL_SCANCODE_LGUI = 227

        /** < windows; command (apple); meta */
        const val SDL_SCANCODE_RCTRL = 228
        const val SDL_SCANCODE_RSHIFT = 229
        const val SDL_SCANCODE_RALT = 230

        @JvmField
        var gv: MyGLSurfaceView? = null

        @JvmStatic
        fun loadLibraries() {
            try {
                Log.i("JNI", "Trying to load libraries")

                SDLLib.loadSDL()
                System.loadLibrary("touchcontrols")

                // FMOD removed: this build uses the engine's OpenAL + FluidSynth audio backend.
                System.loadLibrary("openal")
                System.loadLibrary("gzdoom")
            } catch (ule: UnsatisfiedLinkError) {
                Log.e("JNI", "WARNING: Could not load shared library: $ule")
            }
        }

        @JvmStatic
        external fun init(graphics_dir: String, mem: Int, args: Array<String>, game: Int, path: String): Int

        @JvmStatic
        external fun setScreenSize(width: Int, height: Int)

        @JvmStatic
        external fun frame(): Int

        @JvmStatic
        external fun touchEvent(action: Int, pid: Int, x: Float, y: Float): Boolean

        @JvmStatic
        external fun keypress(down: Int, qkey: Int, unicode: Int)

        @JvmStatic
        external fun doAction(state: Int, action: Int)

        @JvmStatic
        external fun analogFwd(v: Float)

        @JvmStatic
        external fun analogSide(v: Float)

        @JvmStatic
        external fun analogPitch(mode: Int, v: Float)

        @JvmStatic
        external fun analogYaw(mode: Int, v: Float)

        @JvmStatic
        external fun setTouchSettings(alpha: Float, strafe: Float, fwd: Float, pitch: Float, yaw: Float, other: Int)

        @JvmStatic
        external fun quickCommand(command: String)

        @JvmStatic
        fun swapBuffers() {
            var canDraw = false
            do {
                gv!!.swapBuffers()
                canDraw = gv!!.setupSurface()
            } while (!canDraw)
        }
    }
}
