package com.twotoasters.chron.service;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
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
import com.twotoasters.chron.common.ChronDataMapUtils;
import com.twotoasters.chron.common.ChronWatch;
import com.twotoasters.chron.common.Constants;
import com.twotoasters.chron.common.Utils;

import java.util.TimeZone;

import timber.log.Timber;

public class ChronWatchFaceService extends CanvasWatchFaceService {

    @Override
    public CanvasWatchFaceService.Engine onCreateEngine() {
        return new WatchfaceEngine();
    }

    class WatchfaceEngine extends Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private static final long INTERACTIVE_UPDATE_RATE_MS = 1000;
        private static final int MSG_UPDATE_TIME = 0;

        /** Handler to update the time periodically in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
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
        ChronWatch chronWatch;

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
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            int flatTireHeight = insets.getStableInsetBottom();
            boolean isRound = insets.isRound();
            Timber.d("onApplyWindowInsets: " + (isRound ? "round" + (flatTireHeight > 0 ? " w/ flat tire of height " + flatTireHeight : "") : "square"));
            chronWatch.setRound(isRound);
            if (flatTireHeight > 0 && isRound) {
                int[] screenDimens = Utils.getScreenDimensPx(getApplicationContext());
                int maxDimen = Math.max(screenDimens[0], screenDimens[1]);
                chronWatch.setSize(maxDimen, maxDimen);
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            boolean uiUpdated = false;

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            if (chronWatch.isBurnInProtection() != burnInProtection) {
                chronWatch.setBurnInProtection(burnInProtection);
                uiUpdated = true;
            }

            boolean lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (chronWatch.isLowBitAmbient() != lowBitAmbient) {
                chronWatch.setLowBitAmbient(lowBitAmbient);
                uiUpdated = true;
                chronWatch.updatePaintAntiAliasFlag(!(chronWatch.isLowBitAmbient() && chronWatch.isAmbient()));
            }

            if (uiUpdated) {
                invalidate();
            }

            Timber.d("onPropertiesChanged: burn-in protection = %b, low-bit ambient = %b", burnInProtection, lowBitAmbient);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (chronWatch.isAmbient() != inAmbientMode) {
                chronWatch.setAmbient(inAmbientMode);
                chronWatch.updatePaintAntiAliasFlag(!(chronWatch.isLowBitAmbient() && chronWatch.isAmbient()));
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            chronWatch.setTime(System.currentTimeMillis());
            chronWatch.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                Timber.d("became visible");
                mGoogleApiClient.connect();

                registerTimeZoneReceiver();
                registerTimeFormatObserver();

                // Update time zone in case it changed while we weren't visible.
                chronWatch.clearTime(TimeZone.getDefault().getID());
                chronWatch.setTimeToNow();
                chronWatch.set24HourModeEnabled(DateFormat.is24HourFormat(ChronWatchFaceService.this));
            } else {
                Timber.d("became invisible");
                unregisterTimeZoneReceiver();
                unregisterTimeFormatObserver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerTimeZoneReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ChronWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ChronWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void registerTimeFormatObserver() {
            if (mRegisteredFormatChangeObserver) {
                return;
            }
            mRegisteredFormatChangeObserver = true;
            final ContentResolver resolver = ChronWatchFaceService.this.getContentResolver();
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }

        private void unregisterTimeFormatObserver() {
            if (!mRegisteredFormatChangeObserver) {
                return;
            }
            mRegisteredFormatChangeObserver = false;
            final ContentResolver resolver = ChronWatchFaceService.this.getContentResolver();
            resolver.unregisterContentObserver(mFormatChangeObserver);
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Timber.d("onConnected: " + connectionHint);
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            updateConfigDataItemAndUiOnStartup();
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Timber.d("onConnectionSuspended: " + cause);
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Timber.d("onConnectionFailed: " + result);
        }

        private void updateConfigDataItemAndUiOnStartup() {
            ChronDataMapUtils.fetchConfigDataMap(mGoogleApiClient,
                    new ChronDataMapUtils.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing, use the default values.
                            boolean addedKey = setDefaultValuesForMissingConfigKeys(startupConfig);
                            if (addedKey) {
                                ChronDataMapUtils.putConfigDataItem(mGoogleApiClient, startupConfig);
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
                if (!dataItem.getUri().getPath().equals(Constants.CONFIG_PATH)) {
                    continue;
                }

                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                Timber.d("Config DataItem updated:" + config);
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
                Timber.d("Found watch face config key: " + configKey + " -> " + Integer.toHexString(color));
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
            addedKey |= addIntKeyIfMissing(config, Constants.KEY_PRIMARY_COLOR, Utils.colorForName(ChronWatchFaceService.this, getString(Constants.DEFAULT_PRIMARY_COLOR_NAME)));
            addedKey |= addIntKeyIfMissing(config, Constants.KEY_ACCENT_COLOR, Utils.colorForName(ChronWatchFaceService.this, getString(Constants.DEFAULT_ACCENT_COLOR_NAME)));
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
