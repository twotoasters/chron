package com.twotoasters.chron.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.twotoasters.chron.R;
import com.twotoasters.chron.util.ChronWatchFaceUtil;

public class ChronWatchFaceConfigActivity extends Activity implements WearableListView.ClickListener {

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
        listView.setAdapter(new ColorListAdapter(colors));
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
        ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) viewHolder;
        String colorName = colorItemViewHolder.label.getText().toString();
        updateConfigDataItem(Color.parseColor(colorName));
        Log.d(TAG, "selected color from config: " + colorName);
        finish();
    }

    private void updateConfigDataItem(final int backgroundColor) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putInt(ChronWatchFaceUtil.KEY_BACKGROUND_COLOR,
                backgroundColor);
        ChronWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override
    public void onTopEmptyRegionClick() {
        // no op
    }

    private class ColorListAdapter extends RecyclerView.Adapter<ColorItemViewHolder> {

        private final String[] mColors;

        public ColorListAdapter(String[] colors) {
            mColors = colors;
        }

        @Override
        public ColorItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.color_picker_item, parent, false);
            TextView label = (TextView) view.findViewById(R.id.label);
            return new ColorItemViewHolder(view, label);
        }

        /**
         * TODO: Implement snapping to items, like in SettingsActivity.
         */
        @Override
        public void onBindViewHolder(ColorItemViewHolder holder, int position) {
            String colorName = mColors[position];
            holder.label.setText(colorName);

            RecyclerView.LayoutParams layoutParams =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int colorPickerItemMargin = (int) getResources().getDimension(R.dimen.config_color_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
            } else if (position == mColors.length - 1) {
                layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            holder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mColors.length;
        }
    }

    static class ColorItemViewHolder extends WearableListView.ViewHolder {
        private final TextView label;
        public ColorItemViewHolder(View view, TextView label) {
            super(view);
            this.label = label;
        }
    }
}
