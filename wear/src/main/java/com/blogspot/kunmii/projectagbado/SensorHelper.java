package com.blogspot.kunmii.projectagbado;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.blogspot.kunmii.projectagbado.utils.Utils;

import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;


/**
 * Created by Olakunmi on 27/09/2016.
 */
public class SensorHelper implements SensorEventListener{

    private static SensorHelper mCurrnetInstance;

    Activity activity;
    SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mGeoMagnetic;
    private Sensor vectorRotation;

     public static int delay = SensorManager.SENSOR_DELAY_FASTEST;
    //public static int delay = 1000;
   // int delay = SensorManager.SENSOR_DELAY_UI;

    //int delay = 200000;

    ISensorUpdateListener accelerometerListeners ;
    ISensorUpdateListener rotationListener;


    public  float[] mGravity = new float[3];
    public float[] mGeomagneticData;
    public float[] mAcceleroemter;

    float[] mRotationMatrix = new float[9];
    float[] iMat = new float[9];
    float[] orientation = new float[3];
    float[] values = new float[3];

    private SensorHelper(Activity activity)
    {
        this.activity = activity;

        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mGeoMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        vectorRotation = mSensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR);
    }

    public static SensorHelper getInstance(Activity activity)
    {
        if(mCurrnetInstance == null)
            mCurrnetInstance = new SensorHelper(activity);
        return mCurrnetInstance;
    }

    public void addAccelerometerListener(ISensorUpdateListener sensorUpdateListener)
    {
        accelerometerListeners = sensorUpdateListener;
    }
    public void addRotationUpdateListener(ISensorUpdateListener sensorUpdateListener)
    {
        rotationListener = sensorUpdateListener;
    }

    public void registerSensorListeners()
    {
        mSensorManager.registerListener(this,mAccelerometer,delay);
        mSensorManager.registerListener(this, mGeoMagnetic,delay);
        mSensorManager.registerListener(this, vectorRotation, delay);
    }

    public void unRegisterSensorListener()
    {
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mAccelerometer)
        {
            mAcceleroemter = sensorEvent.values.clone();
            mGravity = lowPass(mAcceleroemter, mGravity, 0.8f);

            if(accelerometerListeners!=null)
                accelerometerListeners.onUpdate(sensorEvent.values.clone());
        }

        if(sensorEvent.sensor == mGeoMagnetic)
        {
            mGeomagneticData = sensorEvent.values.clone();
        }
/*
        if (mGravity != null && mGeomagneticData != null) {
            // Gravity rotational data
            // Magnetic rotational data
            //float[] magnetic = new float[9];

            SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity, mGeomagneticData);

            //SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
            orientation = SensorManager.getOrientation(mRotationMatrix, values);

            //int mAzimuth= (int) ( Math.toDegrees(orientation[0] ) + 360 ) % 360;

            values[0] = orientation[0];



            values[0] = values[0] * 57.2957795f; //azimuth
            values[1] = values[1] * 57.2957795f;   // pitch
            values[2] = values[2] * 57.2957795f;   //roll

            Utils.logData("Kunmi","X: "+values[0] + " "+
                                  "Y: "+values[1] + " Z: " + values[2]);


            if(rotationListener!=null)
            {
                //rotationListener.onUpdate(values);
            }

        }*/

        if(sensorEvent.sensor == vectorRotation)
        {
            // calculate th rotation matrix
            SensorManager.getRotationMatrixFromVector(mRotationMatrix ,sensorEvent.values );
            // get the azimuth value (orientation[0]) in degree

            float[] vals = new float[3];
            vals[0] = (float) Math.toDegrees( SensorManager.getOrientation( mRotationMatrix, orientation )[0]);
            vals[1] = (float) Math.toDegrees( SensorManager.getOrientation( mRotationMatrix, orientation )[1]);
            vals[2] = (float) Math.toDegrees( SensorManager.getOrientation( mRotationMatrix, orientation )[2]);
            if(rotationListener!=null)
            {
                rotationListener.onUpdate(vals);
            }

            //Utils.logData("TOB","X: "+ vals[0] + " "+ "Y: "+ vals[1] + " Z: " + vals[2]);

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
