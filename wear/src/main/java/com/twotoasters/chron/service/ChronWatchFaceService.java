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
import android.view.Gravity;
import android.view.SurfaceHolder;

import com.twotoasters.chron.common.ChronWatch;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ChronWatchFaceService extends CanvasWatchFaceService {

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public CanvasWatchFaceService.Engine onCreateEngine() {
        return new WatchfaceEngine();
    }

    class WatchfaceEngine extends Engine {

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

        // TODO: what does this do?
        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
//                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
//                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
//                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
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
                registerReceiver();
                registerObserver();

                // Update time zone in case it changed while we weren't visible.
                chronWatch.clearTime(TimeZone.getDefault().getID());
                chronWatch.setTimeToNow();
                chronWatch.set24HourModeEnabled(DateFormat.is24HourFormat(ChronWatchFaceService.this));

                invalidate(); // for sweepSeconds
                //mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME); // for ticking seconds
            } else {
                mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
                unregisterReceiver();
                unregisterObserver();
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
    }
}
