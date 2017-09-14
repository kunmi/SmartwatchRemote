package com.blogspot.kunmii.projectagbado;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * Created by Olakunmi on 27/09/2016.
 */
public class SensorHelper implements SensorEventListener{

    private static SensorHelper mCurrnetInstance;

    Activity activity;
    SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mGeoMagnetic;

     public static int delay = SensorManager.SENSOR_DELAY_FASTEST;
    //public static int delay = 1000;
   // int delay = SensorManager.SENSOR_DELAY_UI;

    //int delay = 200000;

    ISensorUpdateListener accelerometerListeners ;
    ISensorUpdateListener magneticListeners;


    public  float[] mGravity = new float[3];
    public float[] mGeomagnetic;
    public float[] mAcceleroemter;

    private SensorHelper(Activity activity)
    {
        this.activity = activity;

        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mGeoMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public static SensorHelper getInstance(Activity activity)
    {
        if(mCurrnetInstance == null)
            mCurrnetInstance = new SensorHelper(activity);
        return mCurrnetInstance;
    }

    public void addAcceleroemterListener(ISensorUpdateListener sensorUpdateListener)
    {
        accelerometerListeners = sensorUpdateListener;
    }
    public void addMagneticListener(ISensorUpdateListener sensorUpdateListener)
    {
        magneticListeners = sensorUpdateListener;
    }

    public void registerSensorListeners()
    {
        mSensorManager.registerListener(this,mAccelerometer,delay);
        mSensorManager.registerListener(this, mGeoMagnetic,delay);
    }

    public void unRegisterSensorListener()
    {
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mAccelerometer)
        {
            mAcceleroemter = sensorEvent.values;

            if(accelerometerListeners!=null)
                accelerometerListeners.onUpdate(sensorEvent);
        }

        if(sensorEvent.sensor == mGeoMagnetic)
        {
            mGeomagnetic = sensorEvent.values;

               if(magneticListeners !=null)
               magneticListeners.onUpdate(sensorEvent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    /***
     * Returns the acceleration force felt on the
     * @param event
     * @return
     */
    public float[] accelerometerFilter(SensorEvent event)
    {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        //Low Pass Filter
        final float alpha = 0.8f;

        float[] linear_acceleration = new float[3];

        mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
        mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
        mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];

        //Using the gravity from lowpass filter to get our acceleration
        linear_acceleration[0] = event.values[0] - mGravity[0];
        linear_acceleration[1] = event.values[1] - mGravity[1];
        linear_acceleration[2] = event.values[2] - mGravity[2];

        return linear_acceleration;
    }


    public float[] lowPass( float[] input, float[] output, float alpha) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i];
        }
        return output;
    }

/*
    public float[] lowPass( float[] input, float[] output, float alpha) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output; }
*/
}
