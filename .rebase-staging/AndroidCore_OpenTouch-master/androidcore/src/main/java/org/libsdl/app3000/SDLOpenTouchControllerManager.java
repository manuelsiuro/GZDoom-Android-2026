package org.libsdl.app3000;

import android.os.Build;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.RequiresApi;

import org.libsdl.app.SDLOpenTouch;

public class SDLOpenTouchControllerManager extends SDLGenericMotionListener_API14 {

    private boolean mRelativeModeEnabled;

    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {

        if ((mRelativeModeEnabled) && (event.getSource() == InputDevice.SOURCE_MOUSE) || (event.getSource() == (InputDevice.SOURCE_MOUSE | InputDevice.SOURCE_TOUCHSCREEN)))
        {
            // If this happens it means the mouse has lost capture somehow
            //if (event.getAction() == MotionEvent.ACTION_HOVER_MOVE)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    setRelativeMouseEnabled(true);
                    //SDLActivity.getContentView().requestPointerCapture();
                }
            }
            return true;
        }

        return SDLOpenTouch.controlInterp.onGenericMotionEvent(event);
    }

    public boolean supportsRelativeMouse() {
        return true;
    }

    @Override
    public boolean inRelativeMode() {
        return mRelativeModeEnabled;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean setRelativeMouseEnabled(boolean enabled) {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            if (enabled) {
                Log.d("TEST", "MOUSE ENABLED");
                SDLActivity.getContentView().requestPointerCapture();
            }
            else {
                Log.d("TEST", "MOUSE DISABLE");
                SDLActivity.getContentView().releasePointerCapture();
            }
            mRelativeModeEnabled = enabled;
            return true;
        }
        else
        {
            return false;
        }
    }
}
