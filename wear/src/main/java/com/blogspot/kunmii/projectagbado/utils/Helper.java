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

public class Helper {

    MainActivity activity = null;
    public TextView mStatusTextView;
    public WearableListView listview;
    public View leftButton;
    public View rightButton;
    public View actionButton;
    public View menuButton;

    public TextView button1Text;
    public TextView button2Text;
    public String[] configItems = new String[]{"Toggle orientation", "Speak Long", "Calibrate Tilt", "Close App", "Exit Menu"
    };

    float screenRotation = 0f;

    public AdapterWearable menuListViewAdapter;


    public Helper(MainActivity act)
    {
        this.activity = act;
        mStatusTextView = (TextView) activity.findViewById(R.id.status);
        View configButtonLayout = activity.findViewById(R.id.menuLayout);


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

        listview = (WearableListView) activity.findViewById(R.id.wearable_list);
        menuListViewAdapter = new AdapterWearable(activity.getApplicationContext(), configItems);
        listview.setAdapter(menuListViewAdapter);

        prepUpMenuListView();

        configButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSettingsListView();
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
