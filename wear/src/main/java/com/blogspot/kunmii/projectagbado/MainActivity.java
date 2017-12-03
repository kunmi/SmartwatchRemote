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
import android.support.v4.view.ViewPager;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.kunmii.projectagbado.utils.ScreenAdapter;
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
    public String ip = "169.254.167.231";

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

    ScreenAdapter adapter = null;

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

//TODO CLEAR VIEWPAGER

                //ViewPager viewPager = (ViewPager) stub.findViewById(R.id.viewPager);
                adapter = new ScreenAdapter((MainActivity) activity);
                //viewPager.setAdapter(adapter);

                //start here

                adapter.mStatusTextView = (TextView) stub.findViewById(R.id.status);
                View configButtonLayout = stub.findViewById(R.id.menuLayout);

                View trackpad = stub.findViewById(R.id.trackPad);
                adapter.setAsTrackPad(trackpad);

                View swicthButton = stub.findViewById(R.id.switchButton);
                adapter.setAsSwitchButton(swicthButton);

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

                /*


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
                }); */
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.KEYPAD, "j"));
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.KEYPAD, "l"));
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_IN:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.KEYPAD, "j"));
                return true;
            case KeyEvent.KEYCODE_NAVIGATE_OUT:
                transferToNetworkHelper(Utils.buildJson(Utils.DataType.KEYPAD, "l"));
                return true;
        }


        return super.onKeyDown(keyCode, event);
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

        if(adapter.mSpeechStatusTextView!=null)
            adapter.mSpeechStatusTextView.setText("Setting Up Speech");


        speechRecognizerHelper = new SpeechRecognizerHelper(activity, new SpeechRecognizerSetupListener() {
            @Override
            public void onSuccessfulInitialization() {
                if(adapter.mSpeechStatusTextView!=null)
                    adapter.mSpeechStatusTextView.setText("Speech Setup, Complete :)");
            }

            @Override
            public void onFailedInitialization() {
                if(adapter.mSpeechStatusTextView!=null)
                    adapter.mSpeechStatusTextView.setText("Speech Setup, FAILED :(");
            }
        });
        speechRecognizerHelper.startInitialization(new RecognitionListener() {
            @Override
            public void onBeginningOfSpeech() {
                if(adapter.mSpeechStatusTextView!=null)
                    adapter.mSpeechStatusTextView.setText("Listening");
            }

            @Override
            public void onEndOfSpeech() {
                if(adapter.mSpeechStatusTextView!=null)
                    adapter.mSpeechStatusTextView.setText("Speech Setup, Complete :)");
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
                            if(adapter.mSpeechStatusTextView!=null)
                                adapter.mSpeechStatusTextView.setText("Start Dictating");
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
                        if(adapter.mSpeechStatusTextView!=null)
                            adapter.mSpeechStatusTextView.setText("Speech Sleeping");
                    }
                } else {
                    listening = false;
                    freeMode = false;
                    Log.d("Kunmi", hypothesis.getHypstr());
                    if(adapter.mSpeechStatusTextView!=null)
                        adapter.mSpeechStatusTextView.setText("Speech Sleeping");
                }
            }

            @Override
            public void onError(Exception e) {
                if(adapter.mSpeechStatusTextView!=null)
                    adapter.mSpeechStatusTextView.setText("Speech: " + e.getMessage());
            }

            @Override
            public void onTimeout() {
                //play timeout
                playAudio(errorAudio);
                freeMode = false;
                if(adapter.mSpeechStatusTextView!=null)
                adapter.mSpeechStatusTextView.setText("Speech Sleeping");
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

    public void testGoogleSpeechRecognizer() {

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
