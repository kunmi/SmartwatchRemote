package com.blogspot.kunmii.projectagbado.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Olakunmi on 11/11/2016.
 */

public class Utils {
    public enum DataType{
        ACCELEROMETER,
        GYRO,
        TOUCH,
        SPEECH,
        SCROLL,
        SHORTCUT
    }

    public enum Touchtype{
        DOWN,
        UP
    }

    public static final String DATA_PATH = "/ip";
    public static final String IP_SETTINGS_KEY = "ip";



    public static String buildJson(DataType type, double[] values){
        String str = null;
        try {
            JSONObject object = new JSONObject();
            object.put("type", type.name());

            JSONObject vals = new JSONObject();
            vals.put("x",(float)values[0]);
            vals.put("y",(float)values[1]);
            vals.put("z",(float)values[2]);

            object.put("values",vals);

            str = object.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return str;
    }
    public static String buildJson(DataType type, float[] values){
        String str = null;
        try {
            JSONObject object = new JSONObject();
            object.put("type", type.name());

            JSONObject vals = new JSONObject();
            vals.put("x",values[0]);
            vals.put("y",values[1]);
            vals.put("z",values[2]);

            object.put("values",vals);

            str = object.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String buildJson(DataType type, String id ,Touchtype val){
        String str = null;
        try {
            JSONObject object = new JSONObject();
            object.put("type", type.name());
            object.put("id",id);
            object.put("values",val.name());

            str = object.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return str;
    }
    public static String buildJson(DataType type, String string){
        String str = null;
        try {
            JSONObject object = new JSONObject();
            object.put("type", type.name());

            object.put("values",string);

            str = object.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return str;
    }

   public static void logData(String TAG,String s)
   {
       Log.d(TAG,s);
   }


    public static boolean checkAudioDeviceAvailability(Activity context)
    {
        PackageManager packageManager = context.getPackageManager();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Check whether the device has a speaker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                return false;
            }

            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void flipView(ViewGroup mainLayout, float rotation)
    {
       // RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.main);
        int w = mainLayout.getWidth();
        int h = mainLayout.getHeight();

        mainLayout.setRotation(rotation);
        mainLayout.setTranslationX((w - h) / 2);
        mainLayout.setTranslationY((h - w) / 2);

        ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mainLayout.getLayoutParams();
        lp.height = w;
        lp.width = h;
        mainLayout.requestLayout();
    }
}
