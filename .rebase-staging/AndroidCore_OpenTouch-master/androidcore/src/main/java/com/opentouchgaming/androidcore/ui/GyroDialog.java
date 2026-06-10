package com.opentouchgaming.androidcore.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.R;

/**
 * Created by Emile on 31/10/2017.
 */

public class GyroDialog
{
    Activity activity;

    public GyroDialog(final Activity act, final Sensor sensor)
    {
        activity = act;

        final Dialog dialog = new Dialog(act, R.style.DialogEngineSettingsWrap);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_gyro);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                dismiss();
            }
        });

        final Switch enableSwitch = dialog.findViewById(R.id.gyro_enable_switch);
        final SeekBar xSens = dialog.findViewById(R.id.gyro_x_seekBar);
        final SeekBar ySens = dialog.findViewById(R.id.gyro_y_seekBar);
        final CheckBox rollToTurn = dialog.findViewById(R.id.gyro_roll_to_turn_checkBox);
        final CheckBox invertX = dialog.findViewById(R.id.gyro_invert_x_checkBox);
        final CheckBox invertY = dialog.findViewById(R.id.gyro_invert_y_checkBox);
        final CheckBox swapXY = dialog.findViewById(R.id.gyro_swap_xy_checkBox);
        final Button gyroCalib = dialog.findViewById(R.id.gyro_calibrate_button);

        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (sensor == null)
                {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                    dialogBuilder.setTitle("Gyroscope not found");
                    dialogBuilder.setMessage("Your device does not appear to have a gyroscope sensor. Download the app called 'Sensors test' to confirm." +
                                             " Email support@opentouchgaming.com if you believe this is an error. Thank you.");
                    dialogBuilder.setPositiveButton("OK", new android.content.DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        }
                    });


                    AlertDialog errdialog = dialogBuilder.create();
                    errdialog.show();
                    enableSwitch.setChecked(false);
                }
                else
                {
                    AppSettings.setBoolOption(activity, "gyro_enable", isChecked);
                    xSens.setEnabled(isChecked);
                    ySens.setEnabled(isChecked);
                    rollToTurn.setEnabled(isChecked);
                    invertX.setEnabled(isChecked);
                    invertY.setEnabled(isChecked);
                    swapXY.setEnabled(isChecked);
                }
            }
        });

        xSens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                AppSettings.setFloatOption(activity, "gyro_x_sens", progress / 100.f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        ySens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                AppSettings.setFloatOption(activity, "gyro_y_sens", progress / 100.f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        rollToTurn.setOnCheckedChangeListener((buttonView, isChecked) -> AppSettings.setBoolOption(activity, "gyro_roll_to_turn", isChecked));

        invertX.setOnCheckedChangeListener((buttonView, isChecked) -> AppSettings.setBoolOption(activity, "gyro_invert_x", isChecked));

        invertY.setOnCheckedChangeListener((buttonView, isChecked) -> AppSettings.setBoolOption(activity, "gyro_invert_y", isChecked));

        swapXY.setOnCheckedChangeListener((buttonView, isChecked) -> AppSettings.setBoolOption(activity, "gyro_swap_xy", isChecked));

        xSens.setProgress((int) (AppSettings.getFloatOption(activity, "gyro_x_sens", 1) * 100));
        ySens.setProgress((int) (AppSettings.getFloatOption(activity, "gyro_y_sens", 1) * 100));
        rollToTurn.setChecked(AppSettings.getBoolOption(activity, "gyro_roll_to_turn", false));
        invertX.setChecked(AppSettings.getBoolOption(activity, "gyro_invert_x", false));
        invertY.setChecked(AppSettings.getBoolOption(activity, "gyro_invert_y", false));
        swapXY.setChecked(AppSettings.getBoolOption(activity, "gyro_swap_xy", false));

        enableSwitch.setChecked(AppSettings.getBoolOption(activity, "gyro_enable", false));
        xSens.setEnabled(enableSwitch.isChecked());
        ySens.setEnabled(enableSwitch.isChecked());
        rollToTurn.setEnabled(enableSwitch.isChecked());
        invertX.setEnabled(enableSwitch.isChecked());
        invertY.setEnabled(enableSwitch.isChecked());
        swapXY.setEnabled(enableSwitch.isChecked());

        gyroCalib.setOnClickListener(v -> new GyroCalibrateDialog(activity, sensor));

        dialog.show();
    }

    public void dismiss()
    {
        //Override me
    }

}
