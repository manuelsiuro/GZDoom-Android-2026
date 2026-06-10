package org.libsdl.app;

import android.view.KeyEvent;

import com.beloko.touchcontrols.ControlInterface;

/**
 * JNI seam to the GZDoom 4.15 engine glue (Clibs_OpenTouch
 * android_jni_inc.cpp, JAVA_FUNC = Java_org_libsdl_app_NativeLib_*).
 *
 * The native methods mirror emileb's AndroidCore NativeLib; the
 * ControlInterface implementation adapts them to this app's vendored
 * com.beloko.touchcontrols ControlInterpreter (same PORT_ACT_* numbering).
 */
public class NativeLib implements ControlInterface {

    public static native int init(String graphics_dir, int options, int wheelNbr, String[] args, int game, String path, String secondaryPath, String logFilename, String nativeLibs, String userFiles, String tmpFiles, String sourceDir, String resDir);

    public static native void setScreenSize(int width, int height);

    public static native void setFramebufferSize(int width, int height, int aspect);

    public static native boolean touchEvent(int action, int pid, float x, float y);

    public static native void keypress(int down, int qkey, int unicode);

    public static native int doAction(int state, int action);

    public static native void backButton();

    public static native void analogFwd(float v, float raw);

    public static native void analogSide(float v, float raw);

    public static native void analogPitch(int mode, float v, float raw);

    public static native void analogYaw(int mode, float v, float raw);

    public static native void weaponWheelSettings(int useMoveStick, int mode, int autoTimeout);

    public static native int audioOverride(int freq, int samples);

    public static native int loadTouchSettings(String filename);

    public static native int saveTouchSettings(String filename);

    public static native int executeCommand(String command);

    public static native int renderControls();

    // ------- com.beloko.touchcontrols.ControlInterface adapter -------

    @Override
    public void initTouchControls_if(String pngPath, int width, int height) {
        // Touch controls are initialised natively by NativeLib.init().
    }

    @Override
    public boolean touchEvent_if(int action, int pid, float x, float y) {
        return touchEvent(action, pid, x, y);
    }

    @Override
    public void keyPress_if(int down, int qkey, int unicode) {
        keypress(down, qkey, unicode);
    }

    @Override
    public void doAction_if(int state, int action) {
        doAction(state, action);
    }

    @Override
    public void analogFwd_if(float v) {
        analogFwd(v, v);
    }

    @Override
    public void analogSide_if(float v) {
        analogSide(v, v);
    }

    @Override
    public void analogPitch_if(int mode, float v) {
        analogPitch(mode, v, v);
    }

    @Override
    public void analogYaw_if(int mode, float v) {
        analogYaw(mode, v, v);
    }

    @Override
    public void setTouchSettings_if(float alpha, float strafe, float fwd, float pitch, float yaw, int other) {
        // Touch-control look/move scaling is configured in the engine's own
        // native settings UI now; nothing to forward.
    }

    @Override
    public void quickCommand_if(String command) {
        executeCommand(command);
    }

    // SDL scancodes (USB HID usage page 0x07), as expected by the glue's
    // PortableKeyEvent -> SDL_SendKeyboardKey.
    public static final int SDL_SCANCODE_A = 4;
    public static final int SDL_SCANCODE_1 = 30;
    public static final int SDL_SCANCODE_0 = 39;
    public static final int SDL_SCANCODE_RETURN = 40;
    public static final int SDL_SCANCODE_ESCAPE = 41;
    public static final int SDL_SCANCODE_BACKSPACE = 42;
    public static final int SDL_SCANCODE_TAB = 43;
    public static final int SDL_SCANCODE_SPACE = 44;
    public static final int SDL_SCANCODE_MINUS = 45;
    public static final int SDL_SCANCODE_EQUALS = 46;
    public static final int SDL_SCANCODE_COMMA = 54;
    public static final int SDL_SCANCODE_PERIOD = 55;
    public static final int SDL_SCANCODE_SLASH = 56;
    public static final int SDL_SCANCODE_F1 = 58;
    public static final int SDL_SCANCODE_PRINTSCREEN = 70;
    public static final int SDL_SCANCODE_INSERT = 73;
    public static final int SDL_SCANCODE_HOME = 74;
    public static final int SDL_SCANCODE_PAGEUP = 75;
    public static final int SDL_SCANCODE_DELETE = 76;
    public static final int SDL_SCANCODE_END = 77;
    public static final int SDL_SCANCODE_PAGEDOWN = 78;
    public static final int SDL_SCANCODE_RIGHT = 79;
    public static final int SDL_SCANCODE_LEFT = 80;
    public static final int SDL_SCANCODE_DOWN = 81;
    public static final int SDL_SCANCODE_UP = 82;
    public static final int SDL_SCANCODE_POWER = 102; // overridden to underscore by the glue
    public static final int SDL_SCANCODE_LCTRL = 224;
    public static final int SDL_SCANCODE_LSHIFT = 225;
    public static final int SDL_SCANCODE_LALT = 226;
    public static final int SDL_SCANCODE_RCTRL = 228;
    public static final int SDL_SCANCODE_RSHIFT = 229;
    public static final int SDL_SCANCODE_RALT = 230;

    @Override
    public int mapKey(int acode, int unicode) {
        if (unicode == 95) { // Hack to make underscore work
            return SDL_SCANCODE_POWER;
        } else if (acode >= KeyEvent.KEYCODE_A && acode <= KeyEvent.KEYCODE_Z) {
            return SDL_SCANCODE_A + (acode - KeyEvent.KEYCODE_A);
        } else if (acode >= KeyEvent.KEYCODE_1 && acode <= KeyEvent.KEYCODE_9) {
            return SDL_SCANCODE_1 + (acode - KeyEvent.KEYCODE_1);
        } else {
            switch (acode) {
                case KeyEvent.KEYCODE_0: return SDL_SCANCODE_0;
                case KeyEvent.KEYCODE_TAB: return SDL_SCANCODE_TAB;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER: return SDL_SCANCODE_RETURN;
                case KeyEvent.KEYCODE_ESCAPE:
                case KeyEvent.KEYCODE_BACK: return SDL_SCANCODE_ESCAPE;
                case KeyEvent.KEYCODE_SPACE: return SDL_SCANCODE_SPACE;
                case KeyEvent.KEYCODE_DEL: return SDL_SCANCODE_BACKSPACE;
                case KeyEvent.KEYCODE_DPAD_UP: return SDL_SCANCODE_UP;
                case KeyEvent.KEYCODE_DPAD_DOWN: return SDL_SCANCODE_DOWN;
                case KeyEvent.KEYCODE_DPAD_LEFT: return SDL_SCANCODE_LEFT;
                case KeyEvent.KEYCODE_DPAD_RIGHT: return SDL_SCANCODE_RIGHT;
                case KeyEvent.KEYCODE_ALT_LEFT: return SDL_SCANCODE_LALT;
                case KeyEvent.KEYCODE_ALT_RIGHT: return SDL_SCANCODE_RALT;
                case KeyEvent.KEYCODE_CTRL_LEFT: return SDL_SCANCODE_LCTRL;
                case KeyEvent.KEYCODE_CTRL_RIGHT: return SDL_SCANCODE_RCTRL;
                case KeyEvent.KEYCODE_SHIFT_LEFT: return SDL_SCANCODE_LSHIFT;
                case KeyEvent.KEYCODE_SHIFT_RIGHT: return SDL_SCANCODE_RSHIFT;
                case KeyEvent.KEYCODE_F1: return SDL_SCANCODE_F1;
                case KeyEvent.KEYCODE_F2: return SDL_SCANCODE_F1 + 1;
                case KeyEvent.KEYCODE_F3: return SDL_SCANCODE_F1 + 2;
                case KeyEvent.KEYCODE_F4: return SDL_SCANCODE_F1 + 3;
                case KeyEvent.KEYCODE_F5: return SDL_SCANCODE_F1 + 4;
                case KeyEvent.KEYCODE_F6: return SDL_SCANCODE_F1 + 5;
                case KeyEvent.KEYCODE_F7: return SDL_SCANCODE_F1 + 6;
                case KeyEvent.KEYCODE_F8: return SDL_SCANCODE_F1 + 7;
                case KeyEvent.KEYCODE_F9: return SDL_SCANCODE_F1 + 8;
                case KeyEvent.KEYCODE_F10: return SDL_SCANCODE_F1 + 9;
                case KeyEvent.KEYCODE_F11: return SDL_SCANCODE_F1 + 10;
                case KeyEvent.KEYCODE_F12: return SDL_SCANCODE_F1 + 11;
                case KeyEvent.KEYCODE_FORWARD_DEL: return SDL_SCANCODE_DELETE;
                case KeyEvent.KEYCODE_INSERT: return SDL_SCANCODE_INSERT;
                case KeyEvent.KEYCODE_PAGE_UP: return SDL_SCANCODE_PAGEUP;
                case KeyEvent.KEYCODE_PAGE_DOWN: return SDL_SCANCODE_PAGEDOWN;
                case KeyEvent.KEYCODE_MOVE_HOME: return SDL_SCANCODE_HOME;
                case KeyEvent.KEYCODE_MOVE_END: return SDL_SCANCODE_END;
                case KeyEvent.KEYCODE_BREAK: return SDL_SCANCODE_PRINTSCREEN;
                case KeyEvent.KEYCODE_PERIOD: return SDL_SCANCODE_PERIOD;
                case KeyEvent.KEYCODE_COMMA: return SDL_SCANCODE_COMMA;
                case KeyEvent.KEYCODE_SLASH: return SDL_SCANCODE_SLASH;
                case KeyEvent.KEYCODE_MINUS: return SDL_SCANCODE_MINUS;
                case KeyEvent.KEYCODE_EQUALS: return SDL_SCANCODE_EQUALS;
                default:
                    if (unicode < 128) return Character.toLowerCase(unicode);
            }
        }
        return 0;
    }
}
