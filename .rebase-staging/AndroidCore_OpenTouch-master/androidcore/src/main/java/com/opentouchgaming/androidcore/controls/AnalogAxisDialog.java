package com.opentouchgaming.androidcore.controls;

import android.app.Activity;
import android.app.Dialog;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;

import java.util.Locale;

/**
 * Created by Emile on 31/10/2017.
 */

public class AnalogAxisDialog implements ActionInput.ActionInputExtra
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.CONTROLS, "AnalogAxisDialog");
    }

    Activity activity;
    public AnalogAxisDialog()
    {

    }
    public void dismiss()
    {
        //Override me
    }

    private float progressToDeadZone(int progress)
    {
        float p = (float) progress;
        if (p <= 50f)
        {
            return (p / 50f) * 0.2f;
        }
        else
        {
            return 0.2f + ((p - 50f) / 50f) * 0.6f;
        }
    }

    private int deadZoneToProgress(float deadZone)
    {
        if (deadZone <= 0.2f)
        {
            return (int) ((deadZone / 0.2f) * 50f);
        }
        else
        {
            float p = 50f + ((deadZone - 0.2f) / 0.6f) * 50f;
            return (int) Math.min(100, p);
        }
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
        dialog.setContentView(R.layout.dialog_analog_axis);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        final SeekBar sensitivity = dialog.findViewById(R.id.sensitivity_seekBar);
        final SeekBar deadZone = dialog.findViewById(R.id.dead_zone_seekBar);
        final SwitchCompat invert = dialog.findViewById(R.id.invert_checkBox);

        final TextView sensitivityValue = dialog.findViewById(R.id.sensitivity_value_textView);
        final TextView deadZoneValue = dialog.findViewById(R.id.dead_zone_value_textView);

        final TextView name = dialog.findViewById(R.id.axis_name_textBox);
        name.setText(action.description);

        invert.setChecked(action.invert);

        sensitivity.setMax(150);
        sensitivity.setProgress((int) (action.scale * 50));

        deadZone.setMax(100);
        deadZone.setProgress(deadZoneToProgress(action.deadZone));

        sensitivityValue.setText(String.format(Locale.US, "%.2f", (float) sensitivity.getProgress() / 50.f));
        deadZoneValue.setText(String.format(Locale.US, "%.2f", progressToDeadZone(deadZone.getProgress())));

        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sensitivityValue.setText(String.format(Locale.US, "%.2f", (float) progress / 50.f));
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

        deadZone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                deadZoneValue.setText(String.format(Locale.US, "%.2f", progressToDeadZone(progress)));
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

        dialog.setOnDismissListener(dialog1 ->
                                    {
                                        action.scale = (float) sensitivity.getProgress() / 50.f;
                                        action.invert = invert.isChecked();
                                        action.deadZone = progressToDeadZone(deadZone.getProgress());
                                        runnable.run();
                                    });

        dialog.show();
    }
}
