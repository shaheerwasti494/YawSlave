package com.yaw.yawcommon;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;

public class YawProvider implements SensorEventListener {
    public interface Listener { void onYawDegrees(float yaw0to360); }

    private final SensorManager sm;
    private final Sensor rotVec;
    private final WindowManager wm;
    private Listener listener;

    private final float[] R = new float[9];
    private final float[] outR = new float[9];
    private final float[] orientation = new float[3];

    public YawProvider(Activity activity) {
        this.sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        this.rotVec = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        this.wm = activity.getWindowManager();
    }

    public void start(Listener l) {
        listener = l;
        if (rotVec != null) sm.registerListener(this, rotVec, SensorManager.SENSOR_DELAY_GAME);
    }
    public void stop() { sm.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent ev) {
        if (ev.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR || listener == null) return;
        SensorManager.getRotationMatrixFromVector(R, ev.values);

        int rotation = wm.getDefaultDisplay().getRotation();
        int x = SensorManager.AXIS_X, y = SensorManager.AXIS_Y;
        switch (rotation) {
            case Surface.ROTATION_90:  x = SensorManager.AXIS_Y;       y = SensorManager.AXIS_MINUS_X; break;
            case Surface.ROTATION_180: x = SensorManager.AXIS_MINUS_X; y = SensorManager.AXIS_MINUS_Y; break;
            case Surface.ROTATION_270: x = SensorManager.AXIS_MINUS_Y; y = SensorManager.AXIS_X; break;
            default: break;
        }
        SensorManager.remapCoordinateSystem(R, x, y, outR);
        SensorManager.getOrientation(outR, orientation);

        float deg = (float) Math.toDegrees(orientation[0]); // [-180..+180]
        if (deg < 0) deg += 360f;                            // [0..360)
        listener.onYawDegrees(deg);
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}