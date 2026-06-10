package org.libsdl.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.controls.ControlConfig;

import org.libsdl.app.NativeLib;

public class SDLOpenTouchGyro implements SensorEventListener {

    protected SensorManager sensorManager;

    float gyroXSens = 1;
    float gyroYSens = 1;

    boolean rollToTurn = false;
    boolean gyroInvertX = false;
    boolean gyroInvertY = false;
    boolean gyroSwapXY = false;

    float gyroXOffset = 0;
    float gyroYOffset = 0;

    long lastGyroTime;

    int rotation;

    SDLOpenTouchGyro(Context context, int rotation) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.rotation = rotation;
    }

    void reload(Context context) {
        boolean enableGyro = AppSettings.getBoolOption(context, "gyro_enable", false);

        gyroXSens = AppSettings.getFloatOption(context, "gyro_x_sens", 1);
        gyroYSens = AppSettings.getFloatOption(context, "gyro_y_sens", 1);
        rollToTurn = AppSettings.getBoolOption(context, "gyro_roll_to_turn", false);
        gyroInvertX = AppSettings.getBoolOption(context, "gyro_invert_x", false);
        gyroInvertY = AppSettings.getBoolOption(context, "gyro_invert_y", false);
        gyroSwapXY = AppSettings.getBoolOption(context, "gyro_swap_xy", false);
        gyroXOffset = AppSettings.getFloatOption(context, "gyro_x_offset", 0);
        gyroYOffset = AppSettings.getFloatOption(context, "gyro_y_offset", 0);

        Sensor sensor = getRotationSensor();

        if (sensor != null) {
            if(enableGyro)
                enable(sensor);
            else
                disable(sensor);
        }
    }

    void enable(Sensor sensor) {
        sensorManager.registerListener(this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    void disable(Sensor sensor) {
        sensorManager.unregisterListener(this,
                sensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_GYROSCOPE) {
            long timeDiff = event.timestamp - lastGyroTime;
            lastGyroTime = event.timestamp;

            timeDiff /= 1000000; // Convert to milliseconds

            float yawDx;
            float pitchDx;

            int turnAxis = rollToTurn ? 2: 0;

            if (!gyroSwapXY) {
                yawDx = event.values[turnAxis] - gyroXOffset;
                pitchDx = event.values[1] - gyroYOffset;
            } else {
                yawDx = event.values[1] - gyroYOffset;
                pitchDx = event.values[turnAxis] - gyroXOffset;
            }

            if (gyroInvertX)
                yawDx = -yawDx;

            if (gyroInvertY)
                pitchDx = -pitchDx;

            yawDx = yawDx * timeDiff / 6000.f;
            pitchDx = pitchDx * timeDiff / 7000.f;

            if (rotation == Surface.ROTATION_90) {
                pitchDx = -pitchDx;
            } else {
                yawDx = -yawDx;
            }

            yawDx *= gyroXSens;
            pitchDx *= gyroYSens;

            if (yawDx > -2 && yawDx < 2) // Quick range check, and check for NaN
                NativeLib.analogYaw(ControlConfig.LOOK_MODE_MOUSE, (yawDx) * gyroXSens, 0);

            if (pitchDx > -2 && pitchDx < 2) // Quick range check, and check for NaN
                NativeLib.analogPitch(ControlConfig.LOOK_MODE_MOUSE, (pitchDx) * gyroYSens, 0);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    Sensor getRotationSensor() {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

}
