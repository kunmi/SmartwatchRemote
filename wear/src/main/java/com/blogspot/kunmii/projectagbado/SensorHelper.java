package com.blogspot.kunmii.projectagbado;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Olakunmi on 27/09/2016.
 */
public class SensorHelper implements SensorEventListener{

    private static SensorHelper mCurrnetInstance;

    Activity activity;
    SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    int delay = SensorManager.SENSOR_DELAY_GAME;

    List<WeakReference<ISensorUpdateListener>> accelerometerListeners = new ArrayList<>();
    List<WeakReference<ISensorUpdateListener>> gyroscopeListeners = new ArrayList<>();


    private SensorHelper(Activity activity)
    {
        this.activity = activity;

        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public static SensorHelper getInstance(Activity activity)
    {
        if(mCurrnetInstance == null)
            mCurrnetInstance = new SensorHelper(activity);
        return mCurrnetInstance;
    }

    public void registerSensorListeners()
    {
        mSensorManager.registerListener(this,mAccelerometer,delay);
        mSensorManager.registerListener(this,mGyroscope,delay);
    }

    public void unRegisterSensorListener()
    {
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mAccelerometer)
        {

        }
        else if(sensorEvent.sensor == mGyroscope)
        {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
