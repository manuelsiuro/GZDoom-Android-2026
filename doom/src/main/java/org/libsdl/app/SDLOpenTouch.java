package org.libsdl.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.beloko.touchcontrols.ControlInterpreter;
import com.beloko.touchcontrols.TouchSettings;

import com.msa.freedoom.AppSettings;
import com.msa.freedoom.Utils;

/**
 * Freedoom's minimal replacement for emileb's AndroidCore SDLOpenTouch.
 *
 * org.libsdl.app2012.SDLActivity (the vendored SDL2 Java glue) calls these
 * static hooks; the original routed them into the full AndroidCore framework.
 * This version wires them to the app's own AppSettings plus the vendored
 * com.beloko.touchcontrols ControlInterpreter, and boots the engine through
 * NativeLib.init() (which never returns; the engine owns the thread).
 */
public class SDLOpenTouch
{
    static final String TAG = "SDLOpenTouch";

    static float resDiv = 1.0f;
    static boolean divDone = false;

    // The engine renders at a fixed landscape resolution for the whole
    // session (it cannot resize after init); the Game activity is locked to
    // sensorLandscape so the surface never changes aspect mid-game.
    static int fbWidth = 0;
    static int fbHeight = 0;

    public static boolean swapMouseXY = false;
    public static boolean invertMouseX = false;
    public static boolean invertMouseY = false;

    static NativeLib engine;

    public static ControlInterpreter controlInterp;

    // Bridge references set by SDLActivity so this class stays independent of
    // the package-specific SDL classes.
    static View surfaceView;
    static Runnable audioPauseCallback;
    static Runnable audioResumeCallback;
    static Runnable enableRelativeMouseCallback;

    public static void setBridge(View surface, Runnable audioPause, Runnable audioResume, Runnable enableRelativeMouse)
    {
        surfaceView = surface;
        audioPauseCallback = audioPause;
        audioResumeCallback = audioResume;
        enableRelativeMouseCallback = enableRelativeMouse;
    }

    public static void onPause(Context context)
    {
        if (audioPauseCallback != null)
            audioPauseCallback.run();
    }

    public static void onResume(Context context)
    {
        if (audioResumeCallback != null)
            audioResumeCallback.run();
    }

    public static void Setup(Activity activity, Intent intent)
    {
        AppSettings.INSTANCE.reloadSettings(activity.getApplication());

        NativeConsoleBox.init(activity);

        // fullscreen + keep screen on
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Utils.setImmersionMode(activity);

        engine = new NativeLib();

        controlInterp = new ControlInterpreter(
                engine,
                Utils.getGameGamepadConfig(activity.getResources()),
                TouchSettings.gamePadControlsFile,
                TouchSettings.gamePadEnabled);

        int div = intent.getIntExtra("res_div", 1);
        resDiv = 1.0f / div;
    }

    public static void RunApplication(Activity activity, Intent intent, float displayWidth, float displayHeight)
    {
        String args = intent.getStringExtra("args");
        if (args == null)
            args = "";

        // The engine resolution is fixed for the whole session: always the
        // landscape dimensions of the surface, regardless of the orientation
        // the game was launched in.
        fbWidth = (int) Math.max(displayWidth, displayHeight);
        fbHeight = (int) Math.min(displayWidth, displayHeight);

        // Force emileb's GLES backend: the default (1 = Vulkan) cannot host
        // the GL-drawn touch controls, which render inside the engine's
        // swap-buffer callback. The GLES framebuffer takes its size from
        // vid_defwidth/vid_defheight (desktop default 640x480), so pass the
        // fixed landscape size, like Delta Touch's $W/$H substitution does.
        args += " +set vid_preferbackend 2 +set vid_fullscreen 1"
                + " +set vid_defwidth " + fbWidth
                + " +set vid_defheight " + fbHeight;

        String[] argsArray = Utils.createArgs(args);

        String gamePath = intent.getStringExtra("game_path");

        int options = 0;
        // The engine renders with its own GLES3 backend; the touch controls
        // draw with the matching GLES context.
        options |= 0x10; // GAME_OPTION_GLES3

        int gameType = 1; // GAME_TYPE_DOOM
        int wheelNbr = 10;

        String userFiles = gamePath + "/user_files";
        new java.io.File(userFiles).mkdirs();
        String logFilename = userFiles + "/gzdoom_log.txt";
        String tmpFiles = activity.getCacheDir().getAbsolutePath();
        String sourceDir = activity.getApplicationContext().getApplicationInfo().sourceDir;
        String nativeSoPath = activity.getApplicationInfo().nativeLibraryDir;
        String pngFiles = activity.getFilesDir().getAbsolutePath();

        Utils.copyPNGAssets(activity, pngFiles);

        Log.v(TAG, "native .so path = " + nativeSoPath);
        Log.v(TAG, "gamePath = " + gamePath);
        Log.v(TAG, "userFiles = " + userFiles);

        NativeLib.audioOverride(0, 0);

        // Only secondary_path may be null in the glue; every other string is
        // dereferenced unconditionally.
        String resDir = gamePath + "/res";

        // Never returns: the engine main loop runs on this (the SDL) thread.
        NativeLib.init(pngFiles + "/", options, wheelNbr, argsArray, gameType, gamePath,
                null, logFilename, nativeSoPath, userFiles, tmpFiles, sourceDir, resDir);
    }

    public static boolean surfaceChanged(Context context, SurfaceHolder holder, int width, int height)
    {
        Log.v(TAG, "surfaceChanged: " + width + " x " + height);

        if (resDiv != 1.0f && !divDone)
        {
            holder.setFixedSize((int) ((width * resDiv) + 0.5f), (int) ((height * resDiv) + 0.5f));
            divDone = true;
            return true;
        }

        NativeLib.setScreenSize(width, height);

        if (controlInterp != null)
            controlInterp.setScreenSize(width, height);

        if (enableRelativeMouseCallback != null)
            enableRelativeMouseCallback.run();

        return false;
    }

    public static boolean onTouchEvent(MotionEvent event)
    {
        return controlInterp != null && controlInterp.onTouchEvent(event);
    }

    public static boolean onKey(int keyCode, KeyEvent event)
    {
        int source = event.getSource();
        // Stop right mouse button being backbutton
        if ((source == InputDevice.SOURCE_MOUSE) || (source == InputDevice.SOURCE_MOUSE_RELATIVE))
        {
            return true;
        }

        // We always want the back button to do an escape
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN)
            {
                NativeLib.backButton();
            }
            return true;
        }

        if (controlInterp == null)
            return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
            return controlInterp.onKeyDown(keyCode, event);
        }
        else if (event.getAction() == KeyEvent.ACTION_UP)
        {
            return controlInterp.onKeyUp(keyCode, event);
        }

        return false;
    }

    // Sent by the native exit() override (Clibs_OpenTouch android_jni_inc.cpp)
    // when the engine terminates; the process must die so the next launch
    // starts with clean native state.
    static final int COMMAND_EXIT_APP = 0x8007;

    public static boolean CommandHandler(Activity activity, Message msg)
    {
        if (msg.arg1 == COMMAND_EXIT_APP)
        {
            Log.v(TAG, "COMMAND_EXIT_APP: finishing");
            activity.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        }
        return false;
    }
}
