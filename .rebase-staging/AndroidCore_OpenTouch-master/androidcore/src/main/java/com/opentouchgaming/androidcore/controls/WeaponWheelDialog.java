package com.opentouchgaming.androidcore.controls;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;
import com.opentouchgaming.androidcore.Utils;

import java.util.ArrayList;

/**
 * Created by Emile on 31/10/2017.
 */

public class WeaponWheelDialog implements ActionInput.ActionInputExtra
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "WeaponWheelDialog");
    }

    Activity activity;
    LinearLayout moveLayout;
    LinearLayout lookLayout;
    LinearLayout mode0Layout;
    LinearLayout mode1Layout;
    public WeaponWheelDialog()
    {

    }
    public void dismiss()
    {
        //Override me
    }
    @Override
    public void show(Activity activity, ActionInput action, Runnable runnable)
    {
        log.log(DebugLog.Level.D, "SHOW");

        this.activity = activity;

        final Dialog dialog = new Dialog(activity, R.style.DialogEngineSettingsWrap);
        //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_weapon_wheel);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        moveLayout = dialog.findViewById(R.id.move_layout);
        lookLayout = dialog.findViewById(R.id.look_layout);
        mode0Layout = dialog.findViewById(R.id.mode_0_layout);
        mode1Layout = dialog.findViewById(R.id.mode_1_layout);
        Spinner spinner = dialog.findViewById(R.id.auto_select_spinner);
        setupAutoTimout(spinner);

        moveLayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setStickUI(true);
            }
        });
        lookLayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setStickUI(false);
            }
        });

        mode0Layout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setModeUI(0);
            }
        });

        mode1Layout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setModeUI(1);
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                dismiss();
            }
        });

        setStickUI(AppSettings.getBoolOption(activity, "weapon_wheel_move_stick", true));
        setModeUI(AppSettings.getIntOption(activity, "weapon_wheel_button_mode", 0));

        dialog.show();
    }

    private void setupAutoTimout(Spinner spinner)
    {
        // LinkedHasMap so order is maintained

        final ArrayList<Utils.SpinnerValues> values = new ArrayList<>();
        values.add(new Utils.SpinnerValues(0, "Disabled"));
        values.add(new Utils.SpinnerValues(200, "0.2 seconds"));
        values.add(new Utils.SpinnerValues(300, "0.3 seconds"));
        values.add(new Utils.SpinnerValues(500, "0.5 seconds"));
        values.add(new Utils.SpinnerValues(750, "0.75 seconds"));
        values.add(new Utils.SpinnerValues(1000, "1.0 seconds"));
        values.add(new Utils.SpinnerValues(1250, "1.25 seconds"));
        values.add(new Utils.SpinnerValues(1500, "1.5 seconds"));
        ArrayAdapter<Utils.SpinnerValues> dataAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, values);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {

                AppSettings.setIntOption(activity, "weapon_wheel_auto_timeout", values.get(position).getIntValue());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        int autoTimeout = AppSettings.getIntOption(activity, "weapon_wheel_auto_timeout", 0);
        int pos = 0;
        for (int n = 0; n < values.size(); n++)
        {
            if (autoTimeout == values.get(n).getIntValue())
            {
                pos = n;
                break;
            }
        }
        spinner.setSelection(pos);
    }

    private void setStickUI(boolean moveStick)
    {
        AppSettings.setBoolOption(activity, "weapon_wheel_move_stick", moveStick);

        if (moveStick)
        {
            moveLayout.setBackgroundResource(R.drawable.layout_sel_background);
            lookLayout.setBackgroundResource(0);
        }
        else
        {
            moveLayout.setBackgroundResource(0);
            lookLayout.setBackgroundResource(R.drawable.layout_sel_background);
        }
    }

    private void setModeUI(int mode)
    {
        AppSettings.setIntOption(activity, "weapon_wheel_button_mode", mode);

        if (mode == 0)
        {
            mode0Layout.setBackgroundResource(R.drawable.layout_sel_background);
            mode1Layout.setBackgroundResource(0);
        }
        else
        { // mode 1
            mode0Layout.setBackgroundResource(0);
            mode1Layout.setBackgroundResource(R.drawable.layout_sel_background);
        }
    }
}
