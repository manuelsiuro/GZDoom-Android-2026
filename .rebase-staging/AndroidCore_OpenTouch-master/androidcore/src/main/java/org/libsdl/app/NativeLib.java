package org.libsdl.app;


import com.opentouchgaming.androidcore.controls.ControlInterface;

public class NativeLib implements ControlInterface {

    public static native int init(String graphics_dir, int options, int wheelNbr, String[] args, int game, String path, String secondaryPath, String filesPath, String nativeLibs, String userFiles, String tmpFiles, String sourceDir, String resDir);

    public static native void setScreenSize(int width, int height);

    public static native void setFramebufferSize(int width, int height, int aspect);

    public static native int frame();

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

    @Override
    public void initTouchControls_if(String pngPath, int width, int height) {

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
    public int doAction_if(int state, int action) {
        return doAction(state, action);
    }

    @Override
    public void backButton_if() {
        backButton();
    }

    @Override
    public void analogFwd_if(float v, float raw) {
        analogFwd(v, raw);
    }

    @Override
    public void analogSide_if(float v, float raw) {
        analogSide(v, raw);
    }

    @Override
    public void analogPitch_if(int mode, float v, float raw) {
        analogPitch(mode, v, raw);
    }

    @Override
    public void analogYaw_if(int mode, float v, float raw) {
        analogYaw(mode, v, raw);
    }

    @Override
    public int loadSettings_if(String filename) {
        return loadTouchSettings(filename);
    }

    @Override
    public int saveSettings_if(String filename) {
        return saveTouchSettings(filename);
    }

}
