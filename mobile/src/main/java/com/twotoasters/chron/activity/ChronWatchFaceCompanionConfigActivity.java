/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twotoasters.chron.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataApi.DataItemResult;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.twotoasters.chron.R;
import com.twotoasters.chron.common.Constants;

/**
 * The phone-side config activity for {@code DigitalWatchFaceService}. Like the watch-side config activity
 * ({@code DigitalWatchFaceWearableConfigActivity}), allows for setting the background color.
 * Additionally, enables setting the color for hour, minute and second digits.
 */
public class ChronWatchFaceCompanionConfigActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                DataApi.DataListener, ResultCallback<DataItemResult> {

    private static final String TAG = "ChronWatchFaceConfig";

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chron_watch_face_config);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        ComponentName name = getIntent().getParcelableExtra(
                WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);
        TextView label = (TextView)findViewById(R.id.label);
        label.setText(label.getText() + " (" + name.getClassName() + ")");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }

        if (mPeerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme(Constants.SCHEME_WEAR).path(Constants.PATH_WITH_FEATURE).authority(mPeerId).build();
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        } else {
            displayNoConnectedDeviceDialog();
        }
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();
            setUpAllPickers(config);
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            setUpAllPickers(null);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            DataItem dataItem = dataEvent.getDataItem();
            if (!dataItem.getUri().getPath().equals(Constants.PATH_WITH_FEATURE)) {
                continue;
            }

            final DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            final DataMap config = dataMapItem.getDataMap();
            Log.d(TAG, "Config DataItem updated:" + config);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setUpAllPickers(config);
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }

    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.title_no_device_connected);
        String okText = getResources().getString(R.string.ok_no_device_connected);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Sets up selected items for all pickers according to given {@code config} and sets up their
     * item selection listeners.
     *
     * @param config the {@code DigitalWatchFaceService} config {@link DataMap}. If null, the default items
     *         are selected.
     */
    private void setUpAllPickers(DataMap config) {
        removeColorPickerListener(R.id.primaryColor);
        removeColorPickerListener(R.id.accentColor);

        setUpColorPickerSelection(R.id.primaryColor, Constants.KEY_PRIMARY_COLOR, config, R.string.color_blue);
        setUpColorPickerSelection(R.id.accentColor, Constants.KEY_ACCENT_COLOR, config, R.string.color_orange);

        setUpColorPickerListener(R.id.primaryColor, Constants.KEY_PRIMARY_COLOR);
        setUpColorPickerListener(R.id.accentColor, Constants.KEY_ACCENT_COLOR);
    }

    private void setUpColorPickerSelection(int spinnerId, final String configKey, DataMap config,
            int defaultColorNameResId) {
        String defaultColorName = getString(defaultColorNameResId);
        int defaultColor = Constants.colorForName(defaultColorName);
        int color;
        if (config != null) {
            color = config.getInt(configKey, defaultColor);
        } else {
            color = defaultColor;
        }
        Spinner spinner = (Spinner) findViewById(spinnerId);
        String[] colorNames = getResources().getStringArray(R.array.color_array);
        for (int i = 0; i < colorNames.length; i++) {
            if (Constants.colorForName(colorNames[i]) == color) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setUpColorPickerListener(int spinnerId, final String configKey) {
        Spinner spinner = (Spinner) findViewById(spinnerId);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                final String colorName = (String) adapterView.getItemAtPosition(pos);
                DataMap config = new DataMap();
                config.putInt(configKey, Constants.colorForName(colorName));

                //ChronConfigUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, config);
                sendConfigUpdateMessage(configKey, Constants.colorForName(colorName));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    private void removeColorPickerListener(int spinnerId) {
        Spinner spinner = (Spinner) findViewById(spinnerId);
        if (spinner != null) {
            spinner.setOnItemSelectedListener(null);
        }
    }

    private void sendConfigUpdateMessage(String configKey, int color) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putInt(configKey, color);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, Constants.PATH_WITH_FEATURE, rawData);

            Log.d(TAG, "Sent watch face config message: " + configKey + " -> " + Integer.toHexString(color));
        }
    }
}
