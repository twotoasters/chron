package com.twotoasters.chron.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.wearable.view.WearableListView;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.twotoasters.chron.R;
import com.twotoasters.chron.adapter.ColorListAdapter;
import com.twotoasters.chron.adapter.ColorListAdapter.ItemViewHolder;
import com.twotoasters.chron.common.ChronDataMapUtils;
import com.twotoasters.chron.common.Constants;
import com.twotoasters.chron.common.Utils;

import timber.log.Timber;

public class ChronWearConfigActivity extends Activity implements WearableListView.ClickListener {

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

        String[] colorNames = getResources().getStringArray(R.array.color_name_array);

        listView.setAdapter(new ColorListAdapter(colorNames));
        listView.addOnScrollListener(new WearableHeaderScrollListener(findViewById(R.id.headerText)));
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Timber.d("onConnected: " + connectionHint);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Timber.d("onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Timber.d("onConnectionFailed: " + result);
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
        updateConfigDataItem(Utils.colorForName(this, colorName));
        Timber.d("selected background color from config: " + colorName);
        finish();
    }

    private void updateConfigDataItem(final int primaryColor) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putInt(Constants.KEY_PRIMARY_COLOR, primaryColor);
        ChronDataMapUtils.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override
    public void onTopEmptyRegionClick() {
        // no op
    }

    static class WearableHeaderScrollListener extends OnScrollListener implements WearableListView.OnScrollListener {

        private View headerView;

        WearableHeaderScrollListener(View headerView) {
            this.headerView = headerView;
        }

        @Override
        public void onScroll(int dy) {
            headerView.setTranslationY(headerView.getTranslationY() - dy);
        }

        @Override
        public void onAbsoluteScrollChange(int i) {
            // no op
        }

        @Override
        public void onScrollStateChanged(int i) {
            // no op
        }

        @Override
        public void onCentralPositionChanged(int i) {
            // no op
        }
    }
}
