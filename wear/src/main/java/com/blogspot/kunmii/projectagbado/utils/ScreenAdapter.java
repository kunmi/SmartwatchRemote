package com.blogspot.kunmii.projectagbado.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.support.wearable.view.WearableListView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    private TextView mStatusTextView;
    public TextView mSpeechStatusTextView;
    private WearableListView listview;
    View button1;
    View button2;
    TextView button1Text;
    TextView button2Text;
    String[] configItems = new String[]{"Toggle orientation", "Speak Long", "Calibrate Tilt", "Close App", "Exit Menu",
            "Test Case 1", "Test UI 2", "Test UI 3", "Test UI 4"
    };

    float screenRotation = 0f;


    //MENU TEST
    String[] chromeMenu = new String[]{"Tab Options", "Bookmark / History", "Settings", "Exit"};
    String[] tabOptions = new String[]{"Open Last Tab","Save Page","Incognito","<- Back"};
    String[] bookmarkOptions = new String[] {"Bookmarks","Bookmark - Manager", "History", "Downloads" , "<- Back"};
    String[] SettingsOptions = new String[] {"Clear Data","Developer Tools","Open Feedback Form","<- Back"};

    AdapterWearable menuListViewAdapter;


    public ScreenAdapter(MainActivity act)
    {
        this.activity = act;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        if (position == 0) {

            View rootView = activity.getLayoutInflater().inflate(R.layout.default_page, container, false);


            mStatusTextView = (TextView) rootView.findViewById(R.id.status);
            mSpeechStatusTextView = (TextView) rootView.findViewById(R.id.speechStatus);

            View configButton = rootView.findViewById(R.id.configureText);
            View configButtonLayout = rootView.findViewById(R.id.menuLayout);

            button1 = rootView.findViewById(R.id.button1);
            button2 = rootView.findViewById(R.id.button2);

            button1Text = (TextView) rootView.findViewById(R.id.button1Text);
            button2Text = (TextView) rootView.findViewById(R.id.button2Text);

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
                    if (activity.mode == MainActivity.TestMode.Test_4_ScrollMode) {
                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, activity.dictionary.get("previous")));
                    }
                }
            });

            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (activity.mode == MainActivity.TestMode.Test_4_ScrollMode) {
                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, activity.dictionary.get("next")));
                    }
                }
            });

            button1.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    int action = MotionEventCompat.getActionMasked(motionEvent);

                    switch (action) {
                        case (MotionEvent.ACTION_DOWN):
                            if (activity.mode == MainActivity.TestMode.Test_4_ScrollMode) {
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, activity.dictionary.get("previous")));
                                button1.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                            } else {
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.DOWN));
                                button1.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                            }
                            break;
                        case (MotionEvent.ACTION_OUTSIDE):
                        case (MotionEvent.ACTION_CANCEL):
                        case (MotionEvent.ACTION_UP):
                            if (activity.mode != MainActivity.TestMode.Test_4_ScrollMode) {
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.UP));
                            }
                            button1.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));
                            break;

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
                            if (activity.mode == MainActivity.TestMode.Test_4_ScrollMode) {
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SPEECH, activity.dictionary.get("next")));
                                button2.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                            } else {
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "R", Utils.Touchtype.DOWN));
                                button2.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                            }
                            break;
                        case (MotionEvent.ACTION_OUTSIDE):
                        case (MotionEvent.ACTION_CANCEL):
                        case (MotionEvent.ACTION_UP):
                            if (activity.mode != MainActivity.TestMode.Test_4_ScrollMode) {
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "R", Utils.Touchtype.UP));
                            }
                            button2.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));
                    }
                    return true;
                }
            });

            container.addView(rootView);

            return rootView;
        }
        else if (position == 1)
        {
            View rootView = activity.getLayoutInflater().inflate(R.layout.gamepad_page, container, false);

            rootView.findViewById(R.id.upleftButton).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.upButton).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.upRightButton).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.leftButton).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.rightButton).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.downLeft).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.downButton).setOnTouchListener(dpadTouchListener);
            rootView.findViewById(R.id.downRight).setOnTouchListener(dpadTouchListener);

            rootView.findViewById(R.id.actionButton).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    int action = MotionEventCompat.getActionMasked(motionEvent);

                    switch (action) {
                        case (MotionEvent.ACTION_DOWN):
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.DOWN));
                                view.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonActive));
                            break;
                        case (MotionEvent.ACTION_OUTSIDE):
                        case (MotionEvent.ACTION_CANCEL):
                        case (MotionEvent.ACTION_UP):
                                activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.TOUCH, "L", Utils.Touchtype.UP));
                                view.setBackgroundColor(ContextCompat.getColor(activity, R.color.buttonInactive));
                    }
                    return true;
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


    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((FrameLayout) object);
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
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-lasttab"));
                                        break;
                                    case 1:
                                        //Save page
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-savepage"));
                                        break;
                                    case 2:
                                        //Bookmark
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-incognito"));
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
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-viewbook"));
                                        break;
                                    case 1:
                                        //Bookmark Manager
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-viewbookman"));
                                        break;
                                    case 2:
                                        //History
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-viewhist"));
                                        break;
                                    case 3:
                                        //Downloads
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-dls"));
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
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-cleard"));
                                        break;
                                    case 1:
                                        //DevTools
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-devt"));
                                        break;
                                    case 2:
                                        //Feedback
                                        activity.transferToNetworkHelper(Utils.buildJson(Utils.DataType.SHORTCUT,"chrome-feedback"));
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
                    //FOR TESTS
                    case 5: //TEST 1
                        activity.mode = MainActivity.TestMode.Test_1;
                        button1.setVisibility(View.VISIBLE);
                        button1Text.setText("L");
                        button2.setVisibility(View.VISIBLE);
                        button2Text.setText("R");
                        Toast.makeText(activity,"Re-caliberate",Toast.LENGTH_SHORT).show();
                        hideSettingsListView();
                        break;
                    case 6: //Test 2
                        activity.mode = MainActivity.TestMode.Test_2_MenuNav;
                        prepChromeMenuListView();
                        //hideSettingsListView();

                        break;
                    case 7: //Test 3
                        button1.setVisibility(View.INVISIBLE);
                        button2.setVisibility(View.INVISIBLE);
                        activity.tiltCalibrated = false;
                        activity.mode = MainActivity.TestMode.Test_3_SpeechOnly;
                        hideSettingsListView();
                        break;
                    case 8: //Test 4
                        button1.setVisibility(View.GONE);
                        button1Text.setText("Prev");
                        button2.setVisibility(View.VISIBLE);
                        button2Text.setText("Next");
                        activity.mode = MainActivity.TestMode.Test_4_ScrollMode;
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


}
