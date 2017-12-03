package com.blogspot.kunmii.projectagbado.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.kunmii.projectagbado.AdapterWearable;
import com.blogspot.kunmii.projectagbado.MainActivity;
import com.blogspot.kunmii.projectagbado.R;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

/**
 * Created by Olakunmi on 19/09/2017.
 */

public class ScreenAdapter extends PagerAdapter {

    MainActivity activity = null;
    public TextView mStatusTextView;
    public TextView mSpeechStatusTextView;
    public WearableListView listview;
    public View button1;
    public View button2;
    public TextView button1Text;
    public TextView button2Text;
    public String[] configItems = new String[]{"Toggle orientation", "Speak Long", "Calibrate Tilt", "Close App", "Exit Menu"
    };

    float screenRotation = 0f;


    //MENU TEST
    String[] chromeMenu = new String[]{"Tab Options", "Bookmark / History", "Settings", "Exit"};
    String[] tabOptions = new String[]{"Open Last Tab","Save Page","Incognito","<- Back"};
    String[] bookmarkOptions = new String[] {"Bookmarks","Bookmark - Manager", "History", "Downloads" , "<- Back"};
    String[] SettingsOptions = new String[] {"Clear Data","Developer Tools","Open Feedback Form","<- Back"};

    public AdapterWearable menuListViewAdapter;


    public ScreenAdapter(MainActivity act)
    {
        this.activity = act;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {


        if (position == 0)
        {
            View rootView = activity.getLayoutInflater().inflate(R.layout.globelayout, container, false);
            mStatusTextView = (TextView) rootView.findViewById(R.id.status);
            View configButtonLayout = rootView.findViewById(R.id.menuLayout);

            View trackpad = rootView.findViewById(R.id.trackPad);
            setAsTrackPad(trackpad);


            View swicthButton = rootView.findViewById(R.id.switchButton);
            setAsSwitchButton(swicthButton);

            mStatusTextView.setText(activity.ip);

            activity.dataListener = new DataApi.DataListener() {
                @Override
                public void onDataChanged(DataEventBuffer dataEvents) {
                    for (DataEvent event : dataEvents) {
                        if (event.getType() == DataEvent.TYPE_CHANGED) {
                            // DataItem changed
                            DataItem item = event.getDataItem();
                            if (item.getUri().getPath().compareTo(Utils.DATA_PATH) == 0) {
                                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                                activity.ip = dataMap.getString(Utils.IP_SETTINGS_KEY);
                                SharedPreferences.Editor editSettings = activity.preferencesCompat.edit();
                                editSettings.putString(Utils.IP_SETTINGS_KEY, activity.ip);
                                editSettings.apply();
                                editSettings.commit();
                                activity.runOnUiThread(new Runnable() {
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

            listview = (WearableListView) rootView.findViewById(R.id.wearable_list);
            menuListViewAdapter = new AdapterWearable(activity.getApplicationContext(), configItems);
            listview.setAdapter(menuListViewAdapter);

            prepUpMenuListView();

            configButtonLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showSettingsListView();
                }
            });



            container.addView(rootView);

            return rootView;
        }

        return null;
    }


    View.OnTouchListener dpadTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = MotionEventCompat.getActionMasked(motionEvent);

            switch (action) {
                case (MotionEvent.ACTION_HOVER_ENTER):
                case (MotionEvent.ACTION_DOWN):
                    view.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                        TransmitForDPAD(view, Utils.Touchtype.DOWN);
                    break;
                case (MotionEvent.ACTION_OUTSIDE):
                case (MotionEvent.ACTION_CANCEL):
                case (MotionEvent.ACTION_UP):
                    TransmitForDPAD(view, Utils.Touchtype.UP);
                    view.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));
            }
            return true;
        }
    };

    public void TransmitForDPAD(View view, Utils.Touchtype type)
    {
        /*
         x,y
         0,0 (left-up)    1,0 (up)     2,0 (up-right)
         0,1 (left)                   2,1 (right)
         0,2 (left-down)  1,2 (down)  2,2(down Right)
         */
        if (type == Utils.Touchtype.DOWN)
        {
            switch (view.getId())
            {
                case R.id.upleftButton:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.UL));
                    break;
                case R.id.upButton:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.U));
                    break;
                case R.id.upRightButton:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.UR));
                    break;

                case R.id.leftButton:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.L));
                    break;

                case R.id.rightButton:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.R));
                    break;

                case R.id.downLeft:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.DL));
                    break;
                case R.id.downButton:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.D));
                    break;
                case R.id.downRight:
                    activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.D));
                    break;
            }
        }
        else if(type == Utils.Touchtype.UP)
        {
                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.DPAD, Utils.TouchTypeDPAD.C));
        }

    }

    private VelocityTracker mVelocityTracker = null;

    public void setAsTrackPad(final View v)
    {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int index = event.getActionIndex();
                int action = event.getActionMasked();
                int pointerId = event.getPointerId(index);

                switch(action) {
                    case MotionEvent.ACTION_DOWN:

                        //transmit Mouse Down
                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.DOWN));
                        v.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));


                        if(mVelocityTracker == null) {
                            // Retrieve a new VelocityTracker object to watch the
                            // velocity of a motion.
                            mVelocityTracker = VelocityTracker.obtain();
                        }
                        else {
                            // Reset the velocity tracker back to its initial state.
                            mVelocityTracker.clear();
                        }
                        // Add a user's movement to the tracker.
                        mVelocityTracker.addMovement(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(event);

                        mVelocityTracker.computeCurrentVelocity(100);

                        String data = Utils.buildJson(Utils.DataType.GYRO, new float[]{
                                0,
                                VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId),
                                VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId)
                        });
                        activity.transferToNetworkHelper(data);


                        /*
                        Log.d("Kunmi", "X velocity: " +
                                VelocityTrackerCompat.getXVelocity(mVelocityTracker,
                                        pointerId));
                        Log.d("Kunmi", "Y velocity: " +
                                VelocityTrackerCompat.getYVelocity(mVelocityTracker,
                                        pointerId));
                        */
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.UP));
                        v.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));
                        String data2 = Utils.buildJson(Utils.DataType.GYRO, new float[]{0, 0, 0});
                        //activity.transferToNetworkHelper(data2);

                        //Log.d("Kunmi", "CANCEL");

                        // Return a VelocityTracker object back to be re-used by others.
                        mVelocityTracker = null;
                        break;
                }
                return true;
            }
        });
    }

    public void setAsSwitchButton(final View v)
    {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.KEYPAD, "m"));
            }
        });

    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((FrameLayout) object);
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
                        Utils.flipView(activity.stub, screenRotation);
                        hideSettingsListView();
                        break;
                    case 1:
                        activity.vibrateDevice(new long[]{50, 50});
                        activity.testGoogleSpeechRecognizer();
                        activity.speechRecognizerHelper.switchSearch(SpeechRecognizerHelper.FREE_FORM);
                        activity.freeMode = true;
                        hideSettingsListView();
                        break;
                    case 2:
                        Toast.makeText(activity, "Position in favoured center", Toast.LENGTH_SHORT).show();
                        activity.tiltCalibrated = false;
                        activity.tiltCalibrationMode = true;
                        activity.tiltCalibrationSamples.clear();
                        hideSettingsListView();
                        break;
                    case 3:
                        activity.mDismissOverlayView.show();
                        break;
                    case 4:
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


}
