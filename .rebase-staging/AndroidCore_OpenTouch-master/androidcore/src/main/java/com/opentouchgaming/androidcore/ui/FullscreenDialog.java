package com.opentouchgaming.androidcore.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.Utils;

public class FullscreenDialog extends Dialog
{

    public FullscreenDialog(Context context)
    {
        super(context, R.style.DialogFullscreenAnimate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // This is where we adjust the dialog's size
        if (getWindow() != null)
        {
            Utils.expandToCutout(getContext(), getWindow(), OptionsDialogKt.LAUNCHER_EXPAND_INTO_NOTCH);
            Utils.setImmersionMode(getContext(), getWindow(), OptionsDialogKt.LAUNCHER_HIDE_NAV_BAR);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
}
