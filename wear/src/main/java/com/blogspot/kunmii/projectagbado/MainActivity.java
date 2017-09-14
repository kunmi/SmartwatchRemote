package com.blogspot.kunmii.projectagbado;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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

    enum TestMode {

        Normal,
        Test_1, //Only Mouse No Speech
        Test_2_MenuNav,
        Test_3_SpeechOnly, //only speech
        Test_4_ScrollMode //Scroll as tilt
    }

    TestMode mode = TestMode.Normal;
    WatchViewStub stub;

    String TAG = "KunmiWEAR";
    String ip = "192.168.178.1";

    Activity activity;
    DismissOverlayView mDismissOverlayView;
    private TextView mStatusTextView;
    private TextView mSpeechStatusTextView;
    private WearableListView listview;
    View button1;
    View button2;
    TextView button1Text;
    TextView button2Text;

   AdapterWearable menuListViewAdapter;

    Vibrator vibrator;

    String[] configItems = new String[]{"Toggle orientation", "Speak Long", "Calibrate Tilt", "Close App", "Exit Menu",
            //Entered for Test Purposes
            "Test Case 1", "Test UI 2", "Test UI 3", "Test UI 4"
    };

    //Handles long press to exi
    GestureDetector mDetector;

    SensorHelper mSensorHelper;

    //SHAKE
    private static final int SHAKE_THRESHOLD = 290;
    long lastUpdate = 0;

    //TILT

    float[] previousRotation = null;
    int calibrationSamples = 20;
    boolean freeMode = false;
    float screenRotation = 0f;

    public float[] mGravity;
    float[] mLinearAcceleration;
    float[] mGeomagnetic;

    boolean lastValZero = false;

    boolean tiltCalibrated = false;
    boolean tiltCalibrationMode = false;

    List<float[]> tiltCalibrationSamples = new ArrayList<>();
    float[] tiltCenter;

    PipeLine pipeLine;
    boolean audioAvailable = false;


    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    MediaPlayer listeningAwakeAudio;
    MediaPlayer commandSuccessfulAudio;
    MediaPlayer errorAudio;
    SpeechRecognizerHelper speechRecognizerHelper;
    boolean listening = false;

    HashMap<String, String> dictionary = new HashMap<>();


    //Settings Related
    SharedPreferences preferencesCompat;
    private GoogleApiClient mGoogleApiClient;
    DataApi.DataListener dataListener;

    //Network Related
    UDPHelper udp;



    //MENU TEST
    String[] chromeMenu = new String[]{"Tab Options", "Bookmark / History", "Settings", "Exit"};
    String[] tabOptions = new String[]{"Open Last Tab","Save Page","Incognito","<- Back"};
    String[] bookmarkOptions = new String[] {"Bookmarks","Bookmark - Manager", "History", "Downloads" , "<- Back"};
    String[] SettingsOptions = new String[] {"Clear Data","Developer Tools","Open Feedback Form","<- Back"};





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;

        preferencesCompat = PreferenceManager.getDefaultSharedPreferences(activity);
        ip = preferencesCompat.getString(Utils.IP_SETTINGS_KEY, ip);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);


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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mStatusTextView.setText("IP changed please restart");
                                }
                            });

                        }
                    } else if (event.getType() == DataEvent.TYPE_DELETED) {
                        // DataItem deleted
                    }
                }
            }
        };
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

        //check for audio
        audioAvailable = Utils.checkAudioDeviceAvailability(this);

        listeningAwakeAudio = MediaPlayer.create(this, R.raw.awake);
        commandSuccessfulAudio = MediaPlayer.create(this, R.raw.error2);
        errorAudio = MediaPlayer.create(this, R.raw.success);


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


                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


                mStatusTextView = (TextView) stub.findViewById(R.id.status);
                mSpeechStatusTextView = (TextView) stub.findViewById(R.id.speechStatus);

                View configButton = stub.findViewById(R.id.configureText);
                View configButtonLayout = stub.findViewById(R.id.menuLayout);

                button1 = stub.findViewById(R.id.button1);
                button2 = stub.findViewById(R.id.button2);

                button1Text = (TextView) stub.findViewById(R.id.button1Text);
                button2Text = (TextView) stub.findViewById(R.id.button2Text);

                mStatusTextView.setText(ip);
                preferencesCompat.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                        Toast.makeText(activity, "Settings Changed, Please Restart", Toast.LENGTH_LONG).show();
                    }
                });



                listview = (WearableListView) stub.findViewById(R.id.wearable_list);
                menuListViewAdapter =  new AdapterWearable(getApplicationContext(), configItems);
                listview.setAdapter(menuListViewAdapter);

                prepUpMenuListView();



                configButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showSettingsListView();
                    }
                });

                configButtonLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showSettingsListView();
                    }
                });


                button1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mode == TestMode.Test_4_ScrollMode)
                        {
                            transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("previous")));
                        }
                    }
                });

                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mode == TestMode.Test_4_ScrollMode)
                        {
                            transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("next")));
                        }
                    }
                });

                button1.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        int action = MotionEventCompat.getActionMasked(motionEvent);


                        switch (action) {
                            case (MotionEvent.ACTION_DOWN):
                                if(mode == TestMode.Test_4_ScrollMode)
                                {
                                    transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("previous")));
                                    button1.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                                }
                                else {
                                    transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.DOWN));
                                    button1.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                                }
                                    break;
                            case (MotionEvent.ACTION_UP):
                                if(mode!=TestMode.Test_4_ScrollMode) {
                                    transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.UP));
                                }
                                button1.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));

                        }
                        return true;
                    }
                });

                button2.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        int action = MotionEventCompat.getActionMasked(motionEvent);

                        switch (action) {
                            case (MotionEvent.ACTION_DOWN):
                                if(mode == TestMode.Test_4_ScrollMode) {
                                    transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get("next")));
                                    button2.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                                }
                                else {
                                    transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "R", Utils.Touchtype.DOWN));
                                    button2.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                                }
                                break;
                            case (MotionEvent.ACTION_UP):
                                if(mode!= TestMode.Test_4_ScrollMode) {
                                    transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "R", Utils.Touchtype.UP));
                                }
                                button2.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));
                        }
                        return true;
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


                //SPEEEEEEEECH RECOGNIZER PART

                // Check if user has given permission to record audio
                int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
                if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
                    return;
                }

                runSpeechRecognizerSetup();

                mSensorHelper.addAcceleroemterListener(new ISensorUpdateListener() {
                    @Override
                    public void onUpdate(SensorEvent event) {
                        mGravity = mSensorHelper.lowPass(event.values.clone(), mGravity, 0.8f);

                        float[] values = new float[3];
                        for (int i = 0; i < mGravity.length; i++) {
                            values[i] = event.values[i] - mGravity[i];
                        }


                        float last_x = 0f, last_y = 0f, last_z = 0f;
                        if (mLinearAcceleration != null) {
                            last_x = mLinearAcceleration[0];
                            last_y = mLinearAcceleration[1];
                            last_z = mLinearAcceleration[2];
                        }

                        //FOR GYRO
                        mLinearAcceleration = values.clone();

                        //WATCH FACE DOWN

                        if (mGravity != null) {
                            if (!listening) {
                                if (mGravity[2] < -9 && Math.abs(mGravity[0]) < 4 && Math.abs(mGravity[1]) < 4) {
                                    listening = true;
                                    vibrateDevice(new long[]{0, 50, 100, 50});
                                    mSpeechStatusTextView.setText("Listening for Keyword");
                                    speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.KEYWORD_CALLS);
                                    freeMode = false;
                                }
                            }
                        }

                        /*
                        //SHAKE
                        if(lastUpdate==0){
                            lastUpdate = System.currentTimeMillis();
                            return;
                        }
                        long curTime = System.currentTimeMillis();

                        // only allow one update every 100ms.
                        if ((curTime - lastUpdate) > 100) {
                            long diffTime = (curTime - lastUpdate);
                            lastUpdate = curTime;

                            float x = values[0];
                            float y = values[1];
                            float z = values[2];

                            float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

                            if (speed > SHAKE_THRESHOLD) {
                                vibrateDevice(new long[]{0,50,100,50});
                                mSpeechStatusTextView.setText("Listening for Keyword");
                                speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.KEYWORD_CALLS);
                                freeMode = false;
                            }
                    }
                    */
                    }
                });

                mSensorHelper.addMagneticListener(new ISensorUpdateListener() {
                    @Override
                    public void onUpdate(SensorEvent event) {
                        float[] values = new float[3];

                        mGeomagnetic = mSensorHelper.lowPass(event.values.clone(), mGeomagnetic, 2.5f);

                        if (mGravity != null && mGeomagnetic != null) {
                            // Gravity rotational data
                            float[] mRotationMatrix = new float[9];
                            // Magnetic rotational data
                            //float[] magnetic = new float[9];

                            SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity, mGeomagnetic);
                            float[] outGravity = new float[9];

                            //SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                            SensorManager.getOrientation(mRotationMatrix, values);

                            values[0] = values[0] * 57.2957795f; //azimuth
                            values[1] = values[1] * 57.2957795f;   // pitch
                            values[2] = values[2] * 57.2957795f;   //roll


                            if (previousRotation == null) {
                                previousRotation = values.clone();
                            } else {

                                double x = values[0] - previousRotation[0];
                                double y = values[1] - previousRotation[1];
                                double z = values[2] - previousRotation[2];

                                //TO be determined
                                //values =  mSensorHelper.lowPass(values,values, 2.5f);

//                                Utils.logData("GYRO","Azimuth: "+values[0]+", Pitch: "+values[1] + ", Roll: "+values[2] +
                                //                                      ", GRAVITY: "+mGravity[0] + ", "+mGravity[1] + ", " + mGravity[2] );

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

                                if (tiltCalibrated) {

                                    float[] realValues = new float[3];
                                    realValues[0] = (values[0] - tiltCenter[0]) / 15;
                                    realValues[1] = ((values[1] - tiltCenter[1]) / 15) * -1;
                                    realValues[2] = (values[2] - tiltCenter[2]) / 15;

                                    //Azimuth always NAN - misbehaving
                                    if (Float.isNaN(realValues[0]))
                                        realValues[0] = 0;


                                    // Utils.logData("GYRO","Azimuth: "+values[0]+", Pitch: "+values[1] + ", Roll: "+values[2] +
                                    //       ", SENT: "+realValues[0] + ", "+realValues[1] + ", " + realValues[2] );


                                    if (Math.abs(realValues[0]) > 0.1 || Math.abs(realValues[1]) > 0.1 || Math.abs(realValues[2]) > 0.1) {

                                        //TestCheck
                                        if(mode == TestMode.Test_4_ScrollMode)
                                        {
                                            String data = Utils.buildJson(Utils.DataType.SCROLL, realValues);
                                            if (data != null) {
                                                lastValZero = false;
                                                transferToNetworkHelper(data);
                                                Utils.logData("DATA", data);
                                            }
                                        }

                                        else{
                                            String data = Utils.buildJson(Utils.DataType.GYRO, realValues);
                                            if (data != null) {
                                                lastValZero = false;
                                                transferToNetworkHelper(data);
                                                Utils.logData("DATA", data);
                                            }
                                        }

                                    }
                                    //Needed to reset joystick to zero
                                    else {
                                        if (!lastValZero) {
                                            lastValZero = true;
                                            if(mode != TestMode.Test_4_ScrollMode) {
                                                String data = Utils.buildJson(Utils.DataType.GYRO, new float[]{0, 0, 0});
                                                transferToNetworkHelper(data);
                                                Utils.logData("DATA", data);
                                            }
                                        }
                                    }

                                }

                            }

                            //mLinearAcceleration = null;
                            //mGeomagnetic = null;

                        }


                    }
                });
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

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


    public void showSettingsListView() {
        listview.setVisibility(View.VISIBLE);
        listview.smoothScrollToPosition(1);
    }

    public void hideSettingsListView() {
        listview.setVisibility(View.GONE);
    }

    public void prepUpMenuListView()
    {
        menuListViewAdapter.updateDataSet(configItems);
        menuListViewAdapter.notifyDataSetChanged();
        listview.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                Integer tag = (Integer) viewHolder.itemView.getTag();
                switch (tag) {
                    case 0:

                        screenRotation = screenRotation == 0f ? 90f : 0f;
                        Utils.flipView(stub, screenRotation);
                        hideSettingsListView();
                        break;
                    case 1:
                        vibrateDevice(new long[]{50, 50});
                        testGoogleSpeechRecognizer();
                        speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.FREE_FORM);
                        freeMode = true;
                        hideSettingsListView();
                        break;
                    case 2:
                        Toast.makeText(activity, "Position in favoured center", Toast.LENGTH_SHORT).show();
                        tiltCalibrated = false;
                        tiltCalibrationMode = true;
                        tiltCalibrationSamples.clear();
                        hideSettingsListView();
                        break;
                    case 3:
                        mDismissOverlayView.show();
                        break;
                    case 4:
                        hideSettingsListView();
                        break;
                    //FOR TESTS
                    case 5: //TEST 1
                        mode = TestMode.Test_1;
                        button1.setVisibility(View.VISIBLE);
                        button1Text.setText("L");
                        button2.setVisibility(View.VISIBLE);
                        button2Text.setText("R");
                        Toast.makeText(activity,"Re-caliberate",Toast.LENGTH_SHORT).show();
                        hideSettingsListView();
                        break;
                    case 6: //Test 2
                        mode = TestMode.Test_2_MenuNav;

                        prepChromeMenuListView();



                        //hideSettingsListView();

                        break;
                    case 7: //Test 3
                        button1.setVisibility(View.INVISIBLE);
                        button2.setVisibility(View.INVISIBLE);
                        tiltCalibrated = false;
                        mode = TestMode.Test_3_SpeechOnly;
                        hideSettingsListView();
                        break;
                    case 8: //Test 4
                        button1.setVisibility(View.GONE);
                        button1Text.setText("Prev");
                        button2.setVisibility(View.VISIBLE);
                        button2Text.setText("Next");
                        mode = TestMode.Test_4_ScrollMode;
                        Toast.makeText(activity,"Re-caliberate",Toast.LENGTH_SHORT).show();
                        hideSettingsListView();
                        break;

                }
            }

            @Override
            public void onTopEmptyRegionClick() {
                hideSettingsListView();
            }
        });
    }

    void prepChromeMenuListView(){

        listview.scrollToPosition(0);

        menuListViewAdapter.updateDataSet(chromeMenu);
        menuListViewAdapter.notifyDataSetChanged();;

        listview.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                Integer tag1 = (Integer) viewHolder.itemView.getTag();
                switch (tag1) {
                    case 0:
                        menuListViewAdapter.updateDataSet(tabOptions);
                        menuListViewAdapter.notifyDataSetChanged();
                        listview.setClickListener(new WearableListView.ClickListener() {
                            @Override
                            public void onClick(WearableListView.ViewHolder viewHolder) {
                                Integer tag2 = (Integer) viewHolder.itemView.getTag();
                                switch (tag2)
                                {
                                    case 0:
                                        //last tab
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-lasttab"));
                                        break;
                                    case 1:
                                        //Save page
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-savepage"));
                                        break;
                                    case 2:
                                        //Bookmark
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-incognito"));
                                        break;
                                    case 3:
                                        prepChromeMenuListView();
                                        //BACK

                                }

                            }

                            @Override
                            public void onTopEmptyRegionClick() {
                                prepChromeMenuListView();
                            }
                        });
                        break;

                    case 1:
                        menuListViewAdapter.updateDataSet(bookmarkOptions);
                        menuListViewAdapter.notifyDataSetChanged();
                        listview.setClickListener(new WearableListView.ClickListener() {
                            @Override
                            public void onClick(WearableListView.ViewHolder viewHolder) {
                                Integer tag2 = (Integer) viewHolder.itemView.getTag();
                                switch (tag2)
                                {
                                    case 0:
                                        //Bookmarks
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-viewbook"));
                                        break;
                                    case 1:
                                        //Bookmark Manager
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-viewbookman"));
                                        break;
                                    case 2:
                                        //History
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-viewhist"));
                                        break;
                                    case 3:
                                        //Downloads
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-dls"));
                                        break;
                                    case 4:
                                        prepChromeMenuListView();
                                        break;
                                        //BACK

                                }

                            }

                            @Override
                            public void onTopEmptyRegionClick() {
                                prepChromeMenuListView();
                            }
                        });
                        break;
                    case 2:
                        menuListViewAdapter.updateDataSet(SettingsOptions);
                        menuListViewAdapter.notifyDataSetChanged();
                        listview.setClickListener(new WearableListView.ClickListener() {
                            @Override
                            public void onClick(WearableListView.ViewHolder viewHolder) {
                                Integer tag2 = (Integer) viewHolder.itemView.getTag();
                                switch (tag2)
                                {
                                    case 0:
                                        //Clear Data
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-cleard"));
                                        break;
                                    case 1:
                                        //DevTools
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-devt"));
                                        break;
                                    case 2:
                                        //Feedback
                                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-feedback"));
                                        break;
                                    case 3:
                                        prepChromeMenuListView();
                                        //BACK
                                        break;

                                }

                            }

                            @Override
                            public void onTopEmptyRegionClick() {
                                prepChromeMenuListView();
                            }
                        });
                        break;

                    case 3:
                        prepUpMenuListView();
                        break;
                }

            }

            @Override
            public void onTopEmptyRegionClick() {
                prepUpMenuListView();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runSpeechRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    public void runSpeechRecognizerSetup() {
        dictionary.put("save", "Save");
        dictionary.put("print doc", "Print");
        dictionary.put("next", "Next");
        dictionary.put("previous", "Previous");
        dictionary.put("print", "Print");
        dictionary.put("okay", "Return");
        dictionary.put("exit", "EXIT");
        dictionary.put("erase", "clear");
        dictionary.put("dictation", "dict");
        //TEST   |  |  |  |  |  |  |  |
        dictionary.put("green","green");
        dictionary.put("yellow","yellow");
        dictionary.put("blue","blue");
        dictionary.put("orange","orange");
        dictionary.put("red","red");
        dictionary.put("white","white");
        dictionary.put("black","black");
        dictionary.put("purple","purple");
        dictionary.put("indigo","indigo");
        dictionary.put("small","small");
        dictionary.put("big","big");

        mSpeechStatusTextView.setText("Setting Up Speech");


        speechRecognizerHelper = new SpeechRecognizerHelper(activity, new SpeechRecognizerSetupListener() {
            @Override
            public void onSuccessfulInitialization() {
                mSpeechStatusTextView.setText("Speech Setup, Complete :)");
            }

            @Override
            public void onFailedInitialization() {
                mSpeechStatusTextView.setText("Speech Setup, FAILED :(");
            }
        });
        speechRecognizerHelper.startInitialization(new RecognitionListener() {
            @Override
            public void onBeginningOfSpeech() {
                mSpeechStatusTextView.setText("Listening");
            }

            @Override
            public void onEndOfSpeech() {
                mSpeechStatusTextView.setText("Speech Setup, Complete :)");
                //if (!speechRecognizerHelper.getRecognizer().getSearchName().equals(speechRecognizerHelper.WAKEUP_CALL))
                //speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.WAKEUP_CALL);
                speechRecognizerHelper.pauseRecognizer();
            }

            @Override
            public void onPartialResult(Hypothesis hypothesis) {

            }

            @Override
            public void onResult(Hypothesis hypothesis) {
                speechRecognizerHelper.pauseRecognizer();
                if (hypothesis != null) {
                    String speech = hypothesis.getHypstr();
                    if (dictionary.containsKey(speech)) {
                        //play victory
                        playAudio(commandSuccessfulAudio);
                        if (dictionary.get(speech).compareTo("dict") != 0) {
                            transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, dictionary.get(speech)));
                            vibrateDevice(new long[]{0, 50});
                            speechRecognizerHelper.pauseRecognizer();
                            listening = false;

                        } else {
                            freeMode = true;
                            vibrateDevice(new long[]{0, 100, 200, 100, 200, 400});
                            mSpeechStatusTextView.setText("Start Dictating");
                            speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.FREE_FORM);
                            return;
                        }

                        //speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.WAKEUP_CALL);
                    }


                    if (freeMode) {
                        transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, "enter " + hypothesis.getHypstr()));
                        vibrateDevice(new long[]{0,100});
                    } else {
                        listening = false;
                        freeMode = false;
                        Log.d("Kunmi", hypothesis.getHypstr());
                        mSpeechStatusTextView.setText("Speech Sleeping");
                    }
                } else {
                    listening = false;
                    freeMode = false;
                    Log.d("Kunmi", hypothesis.getHypstr());
                    mSpeechStatusTextView.setText("Speech Sleeping");
                }
            }

            @Override
            public void onError(Exception e) {
                mSpeechStatusTextView.setText("Speech: " + e.getMessage());
            }

            @Override
            public void onTimeout() {
                //play timeout
                playAudio(errorAudio);
                freeMode = false;
                mSpeechStatusTextView.setText("Speech Sleeping");
                speechRecognizerHelper.pauseRecognizer();
                listening = false;
            }
        });


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

    public void playAudio(MediaPlayer player) {
        Log.d("AUDIO ", "Audio Avialable: " + audioAvailable);
        if (audioAvailable)
            player.start();
    }

    public void vibrateDevice(long[] pattern) {
        long[] vibrationPattern = pattern;
        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
    }


    private static final int SPEECH_REQUEST_CODE = 777;

    void testGoogleSpeechRecognizer() {

// Create an intent that can start the Speech Recognizer activity
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
// Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);

    }


    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, spokenText));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
