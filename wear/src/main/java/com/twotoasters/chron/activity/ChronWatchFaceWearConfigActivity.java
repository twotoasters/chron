package com.twotoasters.chron.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableListView;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.twotoasters.chron.R;
import com.twotoasters.chron.adapter.ColorListAdapter;
import com.twotoasters.chron.adapter.ColorListAdapter.ItemViewHolder;
import com.twotoasters.chron.adapter.HeaderFooterListAdapter;
import com.twotoasters.chron.common.ChronConfigUtil;
import com.twotoasters.chron.common.Constants;

public class ChronWatchFaceWearConfigActivity extends Activity implements WearableListView.ClickListener {

    private static final String TAG = "ChronWatchFace";

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chron_config);
        setupListView();
        buildGoogleApiClient();
    }

    private void setupListView() {
        WearableListView listView = (WearableListView) findViewById(R.id.color_list);

        listView.setHasFixedSize(true);
        listView.setClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);

        String[] colors = getResources().getStringArray(R.array.color_array);
        listView.setAdapter(new HeaderFooterListAdapter(new ColorListAdapter(colors), "Primary color"));
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnected: " + connectionHint);
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnectionSuspended: " + cause);
                        }
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnectionFailed: " + result);
                        }
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ItemViewHolder colorItemViewHolder = (ItemViewHolder) viewHolder;
        String colorName = colorItemViewHolder.name.getText().toString();
        updateConfigDataItem(Constants.colorForName(colorName));
        Log.d(TAG, "selected color from config: " + colorName);
        finish();
    }

    private void updateConfigDataItem(final int primaryColor) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putInt(Constants.KEY_PRIMARY_COLOR, primaryColor);
        ChronConfigUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override
    public void onTopEmptyRegionClick() {
        // no op
    }
}
