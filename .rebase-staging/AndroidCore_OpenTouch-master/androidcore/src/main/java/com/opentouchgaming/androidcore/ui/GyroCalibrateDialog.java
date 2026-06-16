package com.opentouchgaming.androidcore.ui;

import static android.content.Context.SENSOR_SERVICE;

import android.app.Activity;
import android.app.Dialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.R;

/**
 * Created by Emile on 31/10/2017.
 */

public class GyroCalibrateDialog implements SensorEventListener
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.LICENSE, "GyroCalibrateDialog");
    }

    final int COUNTDOWN_TIME = 5;

    Activity activity;
    TextView counterTextView;
    Sensor sensor;

    long startTime;

    // Averaging
    float[] values = new float[3];
    int count;

    public GyroCalibrateDialog(final Activity act, final Sensor sensor)
    {
        activity = act;
        this.sensor = sensor;

        final Dialog dialog = new Dialog(act, R.style.DialogEngineSettingsWrap);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_gyro_calibrate);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        dialog.setOnDismissListener(dialog1 -> dismissFirst());

        counterTextView = dialog.findViewById(R.id.calib_count_textView);

        Button go = dialog.findViewById(R.id.gyro_calibrate_button);

        go.setOnClickListener(v ->
                              {
                                  registerSensor(true);
                                  startTime = System.currentTimeMillis();

                                  // Reset averaging
                                  count = 0;
                                  values[0] = 0;
                                  values[1] = 0;
                                  values[2] = 0;
                              });

        dialog.show();
    }

    private void registerSensor(boolean yes)
    {
        SensorManager sm = (SensorManager) activity.getSystemService(SENSOR_SERVICE);

        if (yes)
        {
            sm.registerListener(GyroCalibrateDialog.this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
        else
        {
            sm.unregisterListener(this, sensor);
        }
    }

    private void dismissFirst()
    {
        registerSensor(false);
        dismiss();
    }

    public void dismiss()
    {
        //Override me
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            long timeNow = System.currentTimeMillis();
            long diff = timeNow - startTime;

            int seconds = (int) (diff / 1000);

            int cntDown = (COUNTDOWN_TIME - seconds);

            counterTextView.setText(Integer.toString(cntDown));

            // Start averaging from 3 seconds
            if (cntDown < 3)
            {
                values[0] += event.values[0];
                values[1] += event.values[1];
                values[2] += event.values[2];
                count++;
            }

            if (cntDown <= 0)
            {
                float xAverage = values[0] / count;
                float yAverage = values[1] / count;

                //log.log(DebugLog.Level.D, "count = " + count + "xAverage = " + xAverage + " yAverage = " + yAverage);

                AppSettings.setFloatOption(activity, "gyro_x_offset", xAverage);
                AppSettings.setFloatOption(activity, "gyro_y_offset", yAverage);

                registerSensor(false);

                counterTextView.setText("Done");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
