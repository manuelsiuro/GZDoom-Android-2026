package com.opentouchgaming.androidcore;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.opentouchgaming.androidcore.controls.GamePadFragment;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;

public class GamepadActivity extends Activity
{
    GamePadFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AppInfo.Apps app = AppInfo.Apps.valueOf(getIntent().getStringExtra("app"));
        AppInfo.setApp(app);

        Utils.setImmersionMode(this, getWindow(), OptionsDialogKt.LAUNCHER_HIDE_NAV_BAR);
        Utils.expandToCutout(this, getWindow(), OptionsDialogKt.LAUNCHER_EXPAND_INTO_NOTCH);

        if (AppInfo.getContext() == null)
            AppInfo.setContext(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamepad);

        Utils.setInsets(this, findViewById(R.id.activity_entry_top), false);

        fragment = (GamePadFragment) getFragmentManager().findFragmentById(R.id.gamepad_fragment_container);
        Typeface face = Typeface.createFromAsset(getAssets(), "recharge_font.ttf");

        TextView titleTextView = findViewById(R.id.title_textView);
        titleTextView.setTypeface(face);
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return fragment.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (fragment.onKeyDown(keyCode, event))
            return true;
        else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (fragment.onKeyUp(keyCode, event))
            return true;
        else
            return super.onKeyUp(keyCode, event);
    }
}
