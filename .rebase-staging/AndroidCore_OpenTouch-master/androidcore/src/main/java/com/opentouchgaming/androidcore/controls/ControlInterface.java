package com.opentouchgaming.androidcore.controls;

public interface ControlInterface
{

    void initTouchControls_if(String pngPath, int width, int height);

    boolean touchEvent_if(int action, int pid, float x, float y);

    void keyPress_if(int down, int qkey, int unicode);

    int doAction_if(int state, int action);

    void backButton_if();

    void analogFwd_if(float v, float raw);

    void analogSide_if(float v, float raw);

    void analogPitch_if(int mode, float v, float raw);

    void analogYaw_if(int mode, float v, float raw);

    int loadSettings_if(String filename);

    int saveSettings_if(String filename);

}
 