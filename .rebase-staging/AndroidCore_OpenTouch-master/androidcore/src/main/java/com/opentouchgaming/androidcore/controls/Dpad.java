package com.opentouchgaming.androidcore.controls;

import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Created by Emile on 06/08/2017.
 */
public class Dpad
{
    public final static int UP = 0;
    public final static int LEFT = 1;
    public final static int RIGHT = 2;
    public final static int DOWN = 3;

    int directionPressedLast = -1; // initialized to -1

    boolean[] axisState = new boolean[4];
    boolean[] keyState = new boolean[4];
    boolean[] finalState = new boolean[4];

    public static boolean isDpadDevice(InputEvent event)
    {
        //Log.d("test","source = " +event.getSource() );
        // Check that input comes from a device with directional pads.

        // Get the source, but clear the button bit which is always set
        int sourceNoButton = event.getSource() & ~InputDevice.SOURCE_CLASS_BUTTON;

        // Trying to determine if a gamepad and NOT and keyboard
        return ((sourceNoButton & InputDevice.SOURCE_KEYBOARD) == 0) ||
               ((sourceNoButton & InputDevice.SOURCE_GAMEPAD) != 0) ||
               ((sourceNoButton & InputDevice.SOURCE_JOYSTICK) != 0);

        /*
        if ((event.getSource() & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD)
        {
            return true;
        } else
        {
            return false;
        }
        */
    }

    public boolean[] getFinalState()
    {
        finalState[Dpad.LEFT] = axisState[Dpad.LEFT] | keyState[Dpad.LEFT];
        finalState[Dpad.RIGHT] = axisState[Dpad.RIGHT] | keyState[Dpad.RIGHT];
        finalState[Dpad.UP] = axisState[Dpad.UP] | keyState[Dpad.UP];
        finalState[Dpad.DOWN] = axisState[Dpad.DOWN] | keyState[Dpad.DOWN];
        return finalState;
    }

    public int getDirectionPressed(InputEvent event)
    {
        if (!isDpadDevice(event))
        {
            return -1;
        }

        int directionPressed = -1; // initialized to -1

        // If the input event is a MotionEvent, check its hat axis values.
        if (event instanceof MotionEvent)
        {

            // Use the hat axis value to find the D-pad direction
            MotionEvent motionEvent = (MotionEvent) event;
            float xaxis = motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X);
            float yaxis = motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y);

            float xaxis1 = motionEvent.getAxisValue(MotionEvent.AXIS_X);
            float yaxis1 = motionEvent.getAxisValue(MotionEvent.AXIS_Y);

            // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
            // LEFT and RIGHT direction accordingly.
            if ((Float.compare(xaxis, -1.0f) == 0) || (Float.compare(xaxis1, -1.0f) == 0))
            {
                axisState[Dpad.LEFT] = true;
                axisState[Dpad.RIGHT] = false;
                directionPressed = Dpad.LEFT;
            }
            else if ((Float.compare(xaxis, 1.0f) == 0) || (Float.compare(xaxis1, 1.0f) == 0))
            {
                axisState[Dpad.LEFT] = false;
                axisState[Dpad.RIGHT] = true;
                directionPressed = Dpad.RIGHT;
            }
            else
            {
                axisState[Dpad.LEFT] = false;
                axisState[Dpad.RIGHT] = false;
            }
            // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
            // UP and DOWN direction accordingly.
            if ((Float.compare(yaxis, -1.0f) == 0) || (Float.compare(yaxis1, -1.0f) == 0))
            {
                axisState[Dpad.UP] = true;
                axisState[Dpad.DOWN] = false;
                directionPressed = Dpad.UP;
            }
            else if ((Float.compare(yaxis, 1.0f) == 0) || (Float.compare(yaxis1, 1.0f) == 0))
            {
                axisState[Dpad.UP] = false;
                axisState[Dpad.DOWN] = true;
                directionPressed = Dpad.DOWN;
            }
            else
            {
                axisState[Dpad.UP] = false;
                axisState[Dpad.DOWN] = false;
            }
        }

        // If the input event is a KeyEvent, check its key code.
        else if (event instanceof KeyEvent)
        {
            // Use the key code to find the D-pad direction.
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT)
            {

                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    keyState[Dpad.LEFT] = true;
                else if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                    keyState[Dpad.LEFT] = false;

                directionPressed = Dpad.LEFT;
            }
            else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)
            {

                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    keyState[Dpad.RIGHT] = true;
                else if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                    keyState[Dpad.RIGHT] = false;

                directionPressed = Dpad.RIGHT;
            }
            else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP)
            {

                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    keyState[Dpad.UP] = true;
                else if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                    keyState[Dpad.UP] = false;

                directionPressed = Dpad.UP;
            }
            else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)
            {

                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    keyState[Dpad.DOWN] = true;
                else if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                    keyState[Dpad.DOWN] = false;

                directionPressed = Dpad.DOWN;
            }
        }

        if (directionPressed != directionPressedLast)
        {
            directionPressedLast = directionPressed;
            return directionPressed;
        }
        else
        {
            return -1;
        }
    }
}