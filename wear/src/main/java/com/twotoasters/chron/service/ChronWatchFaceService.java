package com.twotoasters.chron.service;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.twotoasters.chron.common.ChronConfigUtil;
import com.twotoasters.chron.common.ChronWatch;
import com.twotoasters.chron.common.Constants;
import com.twotoasters.chron.common.Utils;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ChronWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = ChronWatchFaceService.class.getSimpleName();
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public CanvasWatchFaceService.Engine onCreateEngine() {
        return new WatchfaceEngine();
    }

    class WatchfaceEngine extends Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private static final int MSG_UPDATE_TIME = 0;

        boolean mAmbient;
        boolean mMute;

        private ChronWatch chronWatch;

        /** Handler to update the time once a second in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (isVisible()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                chronWatch.clearTime(intent.getStringExtra("time-zone"));
                chronWatch.setTimeToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateFormat();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateFormat();
            }

            private void updateFormat() {
                chronWatch.set24HourModeEnabled(DateFormat.is24HourFormat(ChronWatchFaceService.this));
                invalidate();
            }
        };
        boolean mRegisteredFormatChangeObserver = false;

        GoogleApiClient mGoogleApiClient;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            chronWatch = new ChronWatch(getApplicationContext(), true);

            setWatchFaceStyle(new WatchFaceStyle.Builder(ChronWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setHotwordIndicatorGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setViewProtection(WatchFaceStyle.PROTECT_HOTWORD_INDICATOR | WatchFaceStyle.PROTECT_STATUS_BAR)
                    .setShowSystemUiTime(false)
                    .build());

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                chronWatch.setInteractive(!mAmbient);
                invalidate();
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                // TODO: add alpha to certain elements
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Utils.logd("onSurfaceChanged(%d, %d)", width, height);
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            int flatTireHeight = insets.getStableInsetBottom();
            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" + (flatTireHeight > 0 ? " w/ flat tire of height " + flatTireHeight : "") : "square"));
            if (flatTireHeight > 0 && insets.isRound()) {
                int[] screenDimens = Utils.getScreenDimensPx(getApplicationContext());
                int maxDimen = Math.max(screenDimens[0], screenDimens[1]);
                chronWatch.setSize(maxDimen, maxDimen);
            }
        }


        @Override
        public void onDraw(Canvas canvas) {
            chronWatch.setTime(System.currentTimeMillis());
            chronWatch.draw(canvas);

            // Draw every frame as long as we're visible.
            if (isVisible()) {
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                Utils.logd("became visible");
                mGoogleApiClient.connect();

                registerReceiver();
                registerObserver();

                // Update time zone in case it changed while we weren't visible.
                chronWatch.clearTime(TimeZone.getDefault().getID());
                chronWatch.setTimeToNow();
                chronWatch.set24HourModeEnabled(DateFormat.is24HourFormat(ChronWatchFaceService.this));

                invalidate(); // for sweepSeconds
                //mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME); // for ticking seconds
            } else {
                Utils.logd("became invisible");
                mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
                unregisterReceiver();
                unregisterObserver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ChronWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ChronWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void registerObserver() {
            if (mRegisteredFormatChangeObserver) {
                return;
            }
            mRegisteredFormatChangeObserver = true;
            final ContentResolver resolver = ChronWatchFaceService.this.getContentResolver();
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }

        private void unregisterObserver() {
            if (!mRegisteredFormatChangeObserver) {
                return;
            }
            mRegisteredFormatChangeObserver = false;
            final ContentResolver resolver = ChronWatchFaceService.this.getContentResolver();
            resolver.unregisterContentObserver(mFormatChangeObserver);
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected: " + connectionHint);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
            }
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            updateConfigDataItemAndUiOnStartup();
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

        private void updateConfigDataItemAndUiOnStartup() {
            Utils.logd("updateConfigDataItemAndUiOnStartup()");
            ChronConfigUtil.fetchConfigDataMap(mGoogleApiClient,
                    new ChronConfigUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing, use the default values.
                            boolean addedKey = setDefaultValuesForMissingConfigKeys(startupConfig);
                            if (addedKey) {
                                ChronConfigUtil.putConfigDataItem(mGoogleApiClient, startupConfig);
                            }

                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
            );
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

                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                Log.d(TAG, "Config DataItem updated:" + config);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                }
                updateUiForConfigDataMap(config);
            }
        }

        private void updateUiForConfigDataMap(final DataMap config) {
            boolean uiUpdated = false;
            for (String configKey : config.keySet()) {
                if (!config.containsKey(configKey)) {
                    continue;
                }
                int color = config.getInt(configKey);
                Log.d(TAG, "Found watch face config key: " + configKey + " -> " + Integer.toHexString(color));
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                }
                if (chronWatch.updateUiForKey(configKey, color)) {
                    uiUpdated = true;
                }
            }
            if (uiUpdated) {
                invalidate();
            }
        }

        // Returns true if we added a missing key; otherwise, false
        private boolean setDefaultValuesForMissingConfigKeys(DataMap config) {
            boolean addedKey = false;
            addedKey |= addIntKeyIfMissing(config, Constants.KEY_PRIMARY_COLOR, Constants.DEFAULT_PRIMARY_COLOR);
            addedKey |= addIntKeyIfMissing(config, Constants.KEY_ACCENT_COLOR, Constants.DEFAULT_ACCENT_COLOR);
            return addedKey;
        }

        // Returns true if we added a missing key; otherwise, false
        private boolean addIntKeyIfMissing(DataMap config, String key, int color) {
            boolean addedKey = false;
            if (!config.containsKey(key)) {
                config.putInt(key, color);
                addedKey = true;
            }
            return addedKey;
        }
    }
}
