package com.blogspot.kunmii.projectagbado;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.MotionEventCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.kunmii.projectagbado.utils.Helper;
import com.blogspot.kunmii.projectagbado.utils.SpeechRecognizerHelper;
import com.blogspot.kunmii.projectagbado.utils.UDPHelper;
import com.blogspot.kunmii.projectagbado.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;

public class MainActivity extends Activity {

    public WatchViewStub stub;

    String TAG = "KunmiWEAR";
    public String ip = "192.168.0.108";

    Activity activity;
    public DismissOverlayView mDismissOverlayView;

    Vibrator vibrator;

    //Handles long press to exi
    GestureDetector mDetector;

    SensorHelper mSensorHelper;

    //SHAKE
    private static final int SHAKE_THRESHOLD = 290;
    long lastUpdate = 0;

    //TILT

    float[] previousRotation = null;
    int calibrationSamples = 20;

    public float[] mGravity;
    float[] mLinearAcceleration;


    boolean lastValZero = false;

    public boolean tiltCalibrated = false;
    public boolean tiltCalibrationMode = false;

    public List<float[]> tiltCalibrationSamples = new ArrayList<>();
    float[] tiltCenter = null;
    float deadzone = 1.0f;


    PipeLine pipeLine;
    boolean audioAvailable = false;


    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    MediaPlayer listeningAwakeAudio;
    MediaPlayer commandSuccessfulAudio;
    MediaPlayer errorAudio;
    public SpeechRecognizerHelper speechRecognizerHelper;
    boolean listening = false;
    public boolean freeMode = false;


    public HashMap<String, String> dictionary = new HashMap<>();


    //Settings Related
    public SharedPreferences preferencesCompat;
    private GoogleApiClient mGoogleApiClient;
    public DataApi.DataListener dataListener;

    Helper adapter = null;

    //Needed for the dynamic tilt centering; The positon of the watch at touch point becomes the new center
    boolean screenTouched = false;

    //Network Related
    UDPHelper udp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;

        preferencesCompat = PreferenceManager.getDefaultSharedPreferences(activity);
        ip = preferencesCompat.getString(Utils.IP_SETTINGS_KEY, ip);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        Wearable.DataApi.addListener(mGoogleApiClient, dataListener);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })

                .build();

        setContentView(R.layout.activity_main);

        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);


        mSensorHelper = SensorHelper.getInstance(this);


        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(final WatchViewStub stub) {

                mDismissOverlayView = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
                mDismissOverlayView.setIntroText("Long press to close app");
                mDismissOverlayView.showIntroIfNecessary();
                mDismissOverlayView.setLongClickable(true);


                mDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public void onLongPress(MotionEvent e) {
                        mDismissOverlayView.show();
                    }

                });



                //ViewPager viewPager = (ViewPager) stub.findViewById(R.id.viewPager);
                adapter = new Helper((MainActivity) activity);

                View configButtonLayout = stub.findViewById(R.id.menuLayout);

                adapter.mStatusTextView.setText(ip);

                dataListener = new DataApi.DataListener() {
                    @Override
                    public void onDataChanged(DataEventBuffer dataEvents) {
                        for (DataEvent event : dataEvents) {
                            if (event.getType() == DataEvent.TYPE_CHANGED) {
                                // DataItem changed
                                DataItem item = event.getDataItem();
                                if (item.getUri().getPath().compareTo(Utils.DATA_PATH) == 0) {
                                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                                    ip = dataMap.getString(Utils.IP_SETTINGS_KEY);
                                    SharedPreferences.Editor editSettings = preferencesCompat.edit();
                                    editSettings.putString(Utils.IP_SETTINGS_KEY, ip);
                                    editSettings.apply();
                                    editSettings.commit();
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.mStatusTextView.setText("IP changed please restart");
                                        }
                                    });

                                }
                            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                                // DataItem deleted
                            }
                        }
                    }
                };

                adapter.listview = (WearableListView) stub.findViewById(R.id.wearable_list);
                adapter.menuListViewAdapter = new AdapterWearable(activity.getApplicationContext(), adapter.configItems);
                adapter.listview.setAdapter(adapter.menuListViewAdapter);

                adapter.prepUpMenuListView();

                configButtonLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        adapter.showSettingsListView();
                    }
                });



                //end here



                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                preferencesCompat.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                        Toast.makeText(activity, "Settings Changed, Please Restart", Toast.LENGTH_LONG).show();
                    }
                });



                //Network
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            udp = new UDPHelper(ip, 2407);
                            //PIPELINE
                            pipeLine = new PipeLine(udp);
                            pipeLine.startConsuming();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //mStatusTextView.setText(udp.isConnected()?"Connected":"Not Connected");
                                }
                            });
                        } catch (SocketException exp) {
                            exp.printStackTrace();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();

                mSensorHelper.addAccelerometerListener(new ISensorUpdateListener() {
                    @Override
                    public void onUpdate(float[] sensorValues) {
                        mGravity = mSensorHelper.lowPass(sensorValues, mGravity, 0.8f);

                        float[] values = new float[3];
                        for (int i = 0; i < mGravity.length; i++) {
                            values[i] = sensorValues[i] - mGravity[i];
                        }


                        float last_x = 0f, last_y = 0f, last_z = 0f;
                        if (mLinearAcceleration != null) {
                            last_x = mLinearAcceleration[0];
                            last_y = mLinearAcceleration[1];
                            last_z = mLinearAcceleration[2];
                        }

                        //FOR GYRO
                        mLinearAcceleration = values.clone();


                    }
                });

                mSensorHelper.addRotationUpdateListener(new ISensorUpdateListener() {
                    @Override
                    public void onUpdate(float[] values) {

                            if (previousRotation == null) {
                                previousRotation = values.clone();
                            } else {

                                double x = values[0] - previousRotation[0];
                                double y = values[1] - previousRotation[1];
                                double z = values[2] - previousRotation[2];

                                previousRotation = values;


                                if (tiltCalibrationMode) {
                                    tiltCalibrationSamples.add(values);
                                    if (tiltCalibrationSamples.size() == calibrationSamples) {
                                        float[] average = {0f, 0f, 0f};
                                        for (int i = 0; i < calibrationSamples; i++) {
                                            average[0] += tiltCalibrationSamples.get(i)[0];
                                            average[1] += tiltCalibrationSamples.get(i)[1];
                                            average[2] += tiltCalibrationSamples.get(i)[2];
                                        }

                                        average[0] = average[0] / calibrationSamples;
                                        average[1] = average[1] / calibrationSamples;
                                        average[2] = average[2] / calibrationSamples;

                                        tiltCenter = average.clone();
                                        tiltCalibrationSamples.clear();
                                        tiltCalibrated = true;
                                        tiltCalibrationMode = false;
                                        Toast.makeText(activity, "Calibration complete", Toast.LENGTH_SHORT).show();

                                    }
                                }

                                //tiltCalibrated
                                if (screenTouched) {


                                    if(tiltCenter == null)
                                    {
                                        tiltCenter = values;
                                        return;
                                    }

                                    float[] realValues = new float[3];
                                    realValues[0] = (values[0] - tiltCenter[0]) / 15;
                                    realValues[1] = ((values[1] - tiltCenter[1]) / 15) * -1;
                                    realValues[2] = (values[2] - tiltCenter[2]) / 15;

                                    //Azimuth always NAN - misbehaving
                                    if (Float.isNaN(realValues[0]))
                                        realValues[0] = 0;


                                    if (Math.abs(realValues[0]) > deadzone || Math.abs(realValues[1]) > deadzone || Math.abs(realValues[2]) > deadzone) {

                                        //TestCheck
                                            String data = Utils.buildJson(Utils.DataType.GYRO, realValues);
                                            if (data != null) {
                                                lastValZero = false;
                                                if(screenTouched) {
                                                    transferToNetworkHelper(data);
                                                    Utils.logData("DATA", data);
                                                }
                                            }

                                    }
                                    //Needed to reset joystick to zero
                                    else {
                                        if (!lastValZero) {
                                            lastValZero = true;
                                                String data = Utils.buildJson(Utils.DataType.GYRO, new float[]{0, 0, 0});
                                                transferToNetworkHelper(data);
                                                Utils.logData("DATA", data);
                                        }
                                    }

                                }
                                else
                                {
                                    tiltCenter = null;
                                }

                            }



                    }
                });
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("next")));
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("previous")));
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_IN:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("Return")));
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_OUT:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("EXIT")));
                return true;
        }


        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                Toast.makeText(activity, "Pressed", Toast.LENGTH_SHORT).show();
                break;
            case (MotionEvent.ACTION_OUTSIDE):
            case (MotionEvent.ACTION_CANCEL):
            case (MotionEvent.ACTION_UP):
                Toast.makeText(activity, "Released", Toast.LENGTH_SHORT).show();
                break;
        }

        return mDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();


        if (mSensorHelper != null)
            mSensorHelper.registerSensorListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, dataListener);
        mGoogleApiClient.disconnect();

        if (mSensorHelper != null)
            mSensorHelper.unRegisterSensorListener();

    }

    @Override
    protected void onDestroy() {
        if (speechRecognizerHelper != null)
            speechRecognizerHelper.stopRecognizer();

        super.onDestroy();

    }




    public void transferToNetworkHelper(final String message) {
        try {
            udp.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        if (pipeLine != null) {
            pipeLine.queData(message);
        }*/
    }

    private static final int SPEECH_REQUEST_CODE = 777;



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                screenTouched = true;
                break;
            case (MotionEvent.ACTION_OUTSIDE):
            case (MotionEvent.ACTION_CANCEL):
            case (MotionEvent.ACTION_UP):
                screenTouched = false;
                break;

        }
       return super.dispatchTouchEvent(ev);

    }



}
