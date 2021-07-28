// implementation is based on the code from this page
// https://code2care.org/2015/detect-phone-shakes-android-programming
// thanks to c2cDev for the code share!

package com.opensourcesoftware.mobiletouchpad;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.List;

public class DeviceShakeDetector implements SensorEventListener {

    private static final String TAG = "DeviceShakeDetector";

    public interface DeviceShakeDetectorListener {
        void onDeviceShakeDetected();
    }

    private int count = 1;
    private boolean init;
    private Sensor mAccelerometer = null;
    private SensorManager mSensorManager = null;
    private DeviceShakeDetectorListener mListener = null;
    private float x1, y1, z1;
    private static final float ERROR = (float) 7.0;
    private static final long SHAKE_TIMEOUT = 1000; // 1 shake per second
    private long mLastShakeTS = 0;

    public DeviceShakeDetector(DeviceShakeDetectorListener listener) {
        this.mListener = listener;
    }

    public boolean initAccelerometer(Context ctx) {
        mSensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        if (list.size() > 0) {
            Log.d(TAG, "initAccelerometer: found " + list.size() + " sensors!");
            for (int i = 0; i < list.size(); i++) {
                Sensor s = list.get(i);
                Log.d(TAG, "initAccelerometer/Sensor: "  + s.getStringType() + " / " + s.getType() + " / " + s.getName() + " / " + s.getPower());
                if (s.getType() == Sensor.TYPE_ACCELEROMETER) {
                    init = false;

                    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }
            }
        }
        return mAccelerometer != null;
    }

    public void registerListener() {
        if (mAccelerometer != null)
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterListener() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
//        Log.d(TAG, "onSensorChanged: " + e.timestamp + " / " + e.accuracy);

        //Get x,y and z values
        float x,y,z;
        x = e.values[0];
        y = e.values[1];
        z = e.values[2];

        if (!init) {
            x1 = x;
            y1 = y;
            z1 = z;
            init = true;
        } else {
            float diffX = Math.abs(x1 - x);
            float diffY = Math.abs(y1 - y);
            float diffZ = Math.abs(z1 - z);

            //Handling ACCELEROMETER Noise
            if (diffX < ERROR) diffX = (float) 0.0;
            if (diffY < ERROR) diffY = (float) 0.0;
            if (diffZ < ERROR) diffZ = (float) 0.0;

            x1 = x;
            y1 = y;
            z1 = z;

            // Shake Detected!
            if (diffX > diffY) {
//                Log.d(TAG, "onSensorChanged: " + diffX + " / " + diffY);
                count = count+1;
                long currTS = System.currentTimeMillis();
                if (currTS - mLastShakeTS >= SHAKE_TIMEOUT) {
                    mLastShakeTS = currTS;
                    mListener.onDeviceShakeDetected();
                }
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged: " + sensor.toString() + " accuracy: " + accuracy);
    }
}
