package com.opentouchgaming.androidcore.controls;

import static com.opentouchgaming.androidcore.DebugLog.Level.E;

import android.content.Context;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;

import java.io.IOException;
import java.util.HashMap;

public class ControlInterpreter
{
    static DebugLog log;
    static int lastMenuButton = -1;

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "ControlInterpreter");
    }

    Context context;
    ControlInterface controlInterface;
    ControlConfig config;
    boolean gamePadEnabled;
    boolean alternatePointerCode = false;
    float screenWidth, screenHeight;
    HashMap<Integer, Boolean> analogButtonState = new HashMap<Integer, Boolean>(); //Saves current state of analog buttons so all sent each time
    Dpad mDpad = new Dpad();
    boolean[] dpadLastState = new boolean[4];

    /** Engine-specific gamepad config override; null means use the global SharedPreferences setting. */
    public String engineGamepadConfig = null;

    public ControlInterpreter(Context ctx, ControlInterface qif, ActionInputDefinition gamepadDefinition, boolean ctrlEn, boolean alternatePointerCode)
    {
        this(ctx, qif, gamepadDefinition, ctrlEn, alternatePointerCode, null);
    }

    public ControlInterpreter(Context ctx, ControlInterface qif, ActionInputDefinition gamepadDefinition, boolean ctrlEn, boolean alternatePointerCode, String overrideGamepadConfig)
    {
        context = ctx;
        gamePadEnabled = ctrlEn;

        this.alternatePointerCode = alternatePointerCode;
        this.engineGamepadConfig = overrideGamepadConfig;

        config = new ControlConfig(gamepadDefinition, null);

        loadGameControlsFile();

        for (ActionInput ai : config.actions)
        {
            // Put all possible actions in here, fixes crash when configuring gamepad in game and an axis is assigned to a new button
            analogButtonState.put(ai.actionCode, false);
        }

        controlInterface = qif;
    }

    public void loadGameControlsFile()
    {
        try
        {
            String gamepadConfigFile = (engineGamepadConfig != null)
                    ? engineGamepadConfig
                    : AppSettings.getStringOption(context, "gamepad_config_filename", GamePadFragment.DEFAULT_CONFIG);
            config.loadControls(gamepadConfigFile);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            log.log(E, "Error loading gamepad file: " + e);
        }
    }

    public void setScreenSize(int w, int h)
    {
        screenWidth = w;
        screenHeight = h;
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if (alternatePointerCode)
        {
            // Get pointer index from the event object
            int pointerIndex = event.getActionIndex();

            // Get pointer ID
            int pointerId = event.getPointerId(pointerIndex);

            // Get masked (not specific to a pointer) action
            int maskedAction = event.getActionMasked();

            float x = event.getX(pointerIndex) / screenWidth;
            float y = event.getY(pointerIndex) / screenHeight;

            switch (maskedAction)
            {
                case MotionEvent.ACTION_MOVE:
                {
                    int pointerCount = event.getPointerCount();

                    for (int i = 0; i < pointerCount; ++i)
                    {
                        // i is the pointer index but we'll update our pointerIndex variable for clarity
                        pointerIndex = i;

                        // To find out WHICH pointer moved we must compare pointer historical locations
                        if (event.getHistorySize() > 0)
                        {
                            // X or Y location for that pointer index moved?
                            // Corner-case: Pointer index changed (pointer up or down promoted or demoted pointer index while moving?)
                            // Fix: Track by pointer Id via sparse array as outlined in second potential solution below.
                            if ((int) event.getX(i) != (int) event.getHistoricalX(i, 0) || (int) event.getY(i) != (int) event.getHistoricalY(i, 0))
                            {
                                pointerId = event.getPointerId(i);
                                // ...and update the circle's location.
                                x = event.getX(pointerIndex) / screenWidth;
                                y = event.getY(pointerIndex) / screenHeight;

                                controlInterface.touchEvent_if(3, pointerId, x, y);
                            }
                        }
                    }
                }
                break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                {
                    controlInterface.touchEvent_if(1, pointerId, x, y);
                }
                break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                {
                    controlInterface.touchEvent_if(2, pointerId, x, y);
                }
                break;
            }
        }
        else
        {
            int action = event.getAction();
            int actionCode = action & MotionEvent.ACTION_MASK;

            if (actionCode == MotionEvent.ACTION_MOVE)
            {
                for (int i = 0; i < event.getPointerCount(); i++)
                {
                    float x = event.getX(i) / screenWidth;
                    float y = event.getY(i) / screenHeight;
                    int pid = event.getPointerId(i);
                    controlInterface.touchEvent_if(3, pid, x, y);
                }
            }
            else if (actionCode == MotionEvent.ACTION_DOWN)
            {
                float x = event.getX() / screenWidth;
                float y = event.getY() / screenHeight;
                controlInterface.touchEvent_if(1, 0, x, y);
            }
            else if (actionCode == MotionEvent.ACTION_POINTER_DOWN)
            {
                int index = event.getActionIndex();
                if (index != -1)
                {
                    float x = event.getX(index) / screenWidth;
                    float y = event.getY(index) / screenHeight;
                    int pid = event.getPointerId(index);
                    controlInterface.touchEvent_if(1, pid, x, y);
                }
            }
            else if (actionCode == MotionEvent.ACTION_POINTER_UP)
            {
                int index = event.getActionIndex();
                if (index != -1)
                {
                    float x = event.getX(index) / screenWidth;
                    float y = event.getY(index) / screenHeight;
                    int pid = event.getPointerId(index);
                    controlInterface.touchEvent_if(2, pid, x, y);
                }
            }
            else if (actionCode == MotionEvent.ACTION_UP)
            {
                float x = event.getX() / screenWidth;
                float y = event.getY() / screenHeight;
                int index = event.getActionIndex();
                int pid = event.getPointerId(index);

                controlInterface.touchEvent_if(2, pid, x, y);
            }
        }

        return true;
    }

    private void handleDpad(InputEvent event)
    {
        if (Dpad.isDpadDevice(event))
        {
            mDpad.getDirectionPressed(event);
            boolean[] dpadState = mDpad.getFinalState();

            if (dpadState[Dpad.LEFT] != dpadLastState[Dpad.LEFT])
            {
                controlInterface.doAction_if(dpadState[Dpad.LEFT] ? 1 : 0, PortActDefs.PORT_ACT_MENU_LEFT);
                dpadLastState[Dpad.LEFT] = dpadState[Dpad.LEFT];
            }

            if (dpadState[Dpad.RIGHT] != dpadLastState[Dpad.RIGHT])
            {
                controlInterface.doAction_if(dpadState[Dpad.RIGHT] ? 1 : 0, PortActDefs.PORT_ACT_MENU_RIGHT);
                dpadLastState[Dpad.RIGHT] = dpadState[Dpad.RIGHT];
            }

            if (dpadState[Dpad.UP] != dpadLastState[Dpad.UP])
            {
                controlInterface.doAction_if(dpadState[Dpad.UP] ? 1 : 0, PortActDefs.PORT_ACT_MENU_UP);
                dpadLastState[Dpad.UP] = dpadState[Dpad.UP];
            }

            if (dpadState[Dpad.DOWN] != dpadLastState[Dpad.DOWN])
            {
                controlInterface.doAction_if(dpadState[Dpad.DOWN] ? 1 : 0, PortActDefs.PORT_ACT_MENU_DOWN);
                dpadLastState[Dpad.DOWN] = dpadState[Dpad.DOWN];
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        boolean used = false;

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
        {
            if (event.getRepeatCount() > 0)
                return true;

            // If this returns 1 it means the volume key was used
            int ret = controlInterface.doAction_if(1,
                                                   (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? PortActDefs.PORT_ACT_VOLUME_UP : PortActDefs.PORT_ACT_VOLUME_DOWN));
            if (ret == 1)
                return true;
        }

        if (gamePadEnabled)
        {
            for (ActionInput ai : config.actions)
            {
                if (((ai.sourceType == ActionInput.SourceType.BUTTON)) && (ai.source == keyCode))
                {
                    if (event.getRepeatCount() == 0)//Don't want to send key repeats
                        controlInterface.doAction_if(1, ai.actionCode);

                    //log.log(D, "key down intercept");
                    used = true;
                }
            }
        }

        handleDpad(event);

        return used;
        /*
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || //If these were mapped it would have already returned
                (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            return false;
        else {
            int unicode = 0;
            if (event.isPrintingKey() || keyCode == KeyEvent.KEYCODE_SPACE) {
                unicode = event.getUnicodeChar();
            }
            SDLActivity.onNativeKeyDown(keyCode, unicode);
            return true;
        }
         */
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        boolean used = false;

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
        {

            if (event.getRepeatCount() > 0)
                return true;

            // If this returns 1 it means the volume key was used
            int ret = controlInterface.doAction_if(0,
                                                   (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? PortActDefs.PORT_ACT_VOLUME_UP : PortActDefs.PORT_ACT_VOLUME_DOWN));
            if (ret == 1)
                return true;
        }

        if (gamePadEnabled)
        {
            for (ActionInput ai : config.actions)
            {
                if (((ai.sourceType == ActionInput.SourceType.BUTTON)) && (ai.source == keyCode))
                {
                    controlInterface.doAction_if(0, ai.actionCode);
                    used = true;
                }
            }
        }

        handleDpad(event);

        return used;
    }

    //float deadRegion = 0.2f;

    public void onBackButton()
    {
        controlInterface.backButton_if();
    }

    private float analogCalibrate(ActionInput ai, float v)
    {
        if ((v < ai.deadZone) && (v > -ai.deadZone))
            return 0;
        else
        {
            if (v > 0)
                return (v - ai.deadZone) / (1 - ai.deadZone);
            else
                return (v + ai.deadZone) / (1 - ai.deadZone);
        }
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        //log.log(D, "onGenericMotionEvent");
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_BUTTON_PRESS && action != MotionEvent.ACTION_BUTTON_RELEASE)
            return false;

        boolean used = false;
        if (gamePadEnabled)
        {
            for (ActionInput ai : config.actions)
            {
                if ((ai.sourceType == ActionInput.SourceType.AXIS) && (ai.source != -1))
                {
                    int invert;
                    invert = ai.invert ? -1 : 1;
                    float raw = event.getAxisValue(ai.source);
                    float rawDeadZone = analogCalibrate(ai, raw);
                    if (ai.actionCode == PortActDefs.ACTION_ANALOG_PITCH)
                    {
                        controlInterface.analogPitch_if(ControlConfig.LOOK_MODE_JOYSTICK, rawDeadZone * invert * ai.scale, raw);
                    }
                    else if (ai.actionCode == PortActDefs.ACTION_ANALOG_YAW)
                    {
                        controlInterface.analogYaw_if(ControlConfig.LOOK_MODE_JOYSTICK, -rawDeadZone * invert * ai.scale, raw);
                    }
                    else if (ai.actionCode == PortActDefs.ACTION_ANALOG_FWD)
                    {
                        controlInterface.analogFwd_if(-rawDeadZone * invert * ai.scale, raw);
                    }
                    else if (ai.actionCode == PortActDefs.ACTION_ANALOG_STRAFE)
                    {
                        controlInterface.analogSide_if(rawDeadZone * invert * ai.scale, raw);
                    }
                    else //Must be using analog as a button
                    {
                        float value = event.getAxisValue(ai.source);
                        //log.log(D, "Analog as button, value = " + value);
                        //log.log(D, ai.toString());

                        if (((ai.sourcePositive) && (value) > 0.5) || ((!ai.sourcePositive) && (value) < -0.5))
                        {
                            if (!analogButtonState.get(ai.actionCode)) //Check internal state, only send if different
                            {
                                controlInterface.doAction_if(1, ai.actionCode); //press
                                analogButtonState.put(ai.actionCode, true);
                            }
                        }
                        else
                        {
                            if (analogButtonState.get(ai.actionCode)) //Check internal state, only send if different
                            {
                                controlInterface.doAction_if(0, ai.actionCode); //un-press
                                analogButtonState.put(ai.actionCode, false);
                            }
                        }
                    }
                    used = true;
                }
            }
        }

        // Moved to below the above so GZDOOM gamepad custom buttons get registered before the arrows
        handleDpad(event);

        return used;
    }
}
