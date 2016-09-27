package com.blogspot.kunmii.projectagbado;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    DismissOverlayView mDismissOverlayView;
    private TextView mTextView;

    //Handles long press to exi
    GestureDetector mDetector;

    Activity activity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        activity = this;

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                mDismissOverlayView = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
                mDismissOverlayView.setIntroText("Long press to close app");
                mDismissOverlayView.showIntroIfNecessary();
                mDismissOverlayView.setLongClickable(true);

                mDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener(){

                    @Override
                    public void onLongPress(MotionEvent e) {
                        mDismissOverlayView.show();
                    }

                });

                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}
