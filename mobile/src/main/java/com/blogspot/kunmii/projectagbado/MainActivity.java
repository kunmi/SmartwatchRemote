package com.blogspot.kunmii.projectagbado;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity {

    String TAG = "Kunmi";
    GoogleApiClient mGoogleApiClient;

    View sendButton;
    EditText inputText;

    public final String DATA_PATH = "/ip";
    public final String IP_SETTINGS_KEY = "ip";

    Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sendButton = findViewById(R.id.saveButton);
        inputText = (EditText) findViewById(R.id.inputBox);
        context = this;

                mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
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
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

            //addAPIifAvailable

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(inputText.getText().toString().compareTo("") == 0)
                {
                    Toast.makeText(context,"Please input an IP address",Toast.LENGTH_SHORT).show();
                    return;
                }

                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(DATA_PATH);
                putDataMapReq.setUrgent();
                putDataMapReq.getDataMap().putString(IP_SETTINGS_KEY, inputText.getText().toString());
                putDataMapReq.getDataMap().putLong("Time",System.currentTimeMillis());
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult =
                        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

                Toast.makeText(context, "Sent",Toast.LENGTH_SHORT).show();
            }
        });




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }
}
