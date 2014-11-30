package com.twotoasters.chron.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBarActivity;
import android.support.wearable.companion.WatchFaceCompanion;
import android.support.wearable.view.CircledImageView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.afollestad.materialdialogs.MaterialDialog.ListCallback;
import com.afollestad.materialdialogs.list.ItemProcessor;
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
import com.twotoasters.chron.common.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class ChronCompanionConfigActivity extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                DataApi.DataListener, ResultCallback<DataItemResult> {

    @InjectView(R.id.circlePrimaryColor) CircledImageView circlePrimaryColor;
    @InjectView(R.id.circleAccentColor) CircledImageView circleAccentColor;
    @InjectView(R.id.txtPrimaryColor) TextView txtPrimaryColor;
    @InjectView(R.id.txtAccentColor) TextView txtAccentColor;

    private GoogleApiClient googleApiClient;
    private String peerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chron_config);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.inject(this);

        peerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_acknowledgements) {
            startActivity(new Intent(this, AcknowledgementsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Timber.d("onConnected: " + connectionHint);
        if (peerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme(Constants.SCHEME_WEAR).path(Constants.CONFIG_PATH).authority(peerId).build();
            Wearable.DataApi.getDataItem(googleApiClient, uri).setResultCallback(this);
            Wearable.DataApi.addListener(googleApiClient, this);
        } else {
            displayNoConnectedDeviceDialog();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Timber.d("onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Timber.d("onConnectionFailed: " + result);
        displayNoConnectedDeviceDialog();
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult != null && dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();
            updateUi(config);
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on each picker
            updateUi(null);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            DataItem dataItem = dataEvent.getDataItem();
            if (!dataItem.getUri().getPath().equals(Constants.CONFIG_PATH)) {
                continue;
            }

            final DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            final DataMap config = dataMapItem.getDataMap();
            Timber.d("Config DataItem updated:" + config);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUi(config);
                }
            });
        }
    }

    private void updateUi(DataMap config) {
        updateColorSelection(Constants.KEY_PRIMARY_COLOR, config, Constants.DEFAULT_PRIMARY_COLOR_NAME);
        updateColorSelection(Constants.KEY_ACCENT_COLOR, config, Constants.DEFAULT_ACCENT_COLOR_NAME);
    }

    private void updateColorSelection(final String configKey, DataMap config, @StringRes int defaultColorNameResId) {
        String defaultColorName = getString(defaultColorNameResId);
        int defaultColor = Utils.colorForName(this, defaultColorName);
        int color = (config != null ? config.getInt(configKey, defaultColor) : defaultColor);
        String[] colorNames = getResources().getStringArray(R.array.color_name_array);
        for (int i = 0; i < colorNames.length; i++) {
            if (Utils.colorForName(this, colorNames[i]) == color) {
                if (Constants.KEY_PRIMARY_COLOR.equals(configKey)) {
                    txtPrimaryColor.setText(colorNames[i]);
                    circlePrimaryColor.setCircleColor(color);
                } else if (Constants.KEY_ACCENT_COLOR.equals(configKey)) {
                    txtAccentColor.setText(colorNames[i]);
                    circleAccentColor.setCircleColor(color);
                }
                break;
            }
        }
    }

    @OnClick(R.id.btnPrimaryColor)
    public void choosePrimaryColor() {
        chooseColor(true);
    }

    @OnClick(R.id.btnAccentColor)
    public void chooseAccentColor() {
        chooseColor(false);
    }

    private void chooseColor(final boolean isPrimary) {
        new Builder(this)
                .title(isPrimary ? R.string.primary_color : R.string.accent_color)
                .items(R.array.color_name_array)
                .itemsCallback(new ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (isPrimary) {
                            sendConfigUpdateMessage(Constants.KEY_PRIMARY_COLOR, Utils.colorForName(ChronCompanionConfigActivity.this, text.toString()));
                        } else {
                            sendConfigUpdateMessage(Constants.KEY_ACCENT_COLOR, Utils.colorForName(ChronCompanionConfigActivity.this, text.toString()));
                        }
                    }
                })
                .negativeText(R.string.cancel)
                .itemProcessor(new ItemProcessor(this) {
                    @Override
                    protected int getLayout(int i) {
                        return R.layout.item_color;
                    }

                    @Override
                    protected void onViewInflated(int position, CharSequence text, View view) {
                        CircledImageView circle = ButterKnife.findById(view, R.id.circle);
                        circle.setCircleColor(Utils.colorForName(view.getContext(), text.toString()));

                        TextView title = ButterKnife.findById(view, R.id.title);
                        title.setText(text);
                    }
                })
                .show();
    }

    private void sendConfigUpdateMessage(String configKey, int color) {
        if (peerId != null) {
            DataMap config = new DataMap();
            config.putInt(configKey, color);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(googleApiClient, peerId, Constants.CONFIG_PATH, rawData);
            Timber.d("Sent watch face config message: " + configKey + " -> " + Integer.toHexString(color));
        }
    }

    private void displayNoConnectedDeviceDialog() {
        new MaterialDialog.Builder(this)
                .content(R.string.title_no_device_connected)
                .positiveText(R.string.ok_no_device_connected)
                .show();
    }
}
