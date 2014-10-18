package com.twotoasters.chron;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.service.wallpaper.WallpaperService;
import android.support.annotation.DrawableRes;
import android.support.wearable.watchface.WatchFaceService;

import com.twotoasters.watchface.gears.util.DeviceUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ChronWatchFace extends WatchFaceService {

    @Override
    public WallpaperService.Engine onCreateEngine() {
        return new ChronEngine();
    }

    class ChronEngine extends Engine {

        private static final boolean IS_24_HOUR = false; // TODO: accomodate the setting

        private Paint bitmapPaint, hourTimePaint, minTimePaint, datePaint;
        private SimpleDateFormat sdfHour12, sdfHour24;
        private SimpleDateFormat sdfDate;

        private Calendar time;

        private int[] screenDimensPx;

        private Bitmap background;
        private Bitmap hourRing, minuteRing, secondRing;

        ChronEngine() {
            Resources res = getResources();
            float[] screenDimensFloatPx = DeviceUtils.getScreenDimensPx(getApplicationContext());
            screenDimensPx = new int[] { (int) screenDimensFloatPx[0], (int) screenDimensFloatPx[1] };

            bitmapPaint = new Paint();

            minTimePaint = new Paint();
            minTimePaint.setAntiAlias(true);
            minTimePaint.setTextAlign(Align.CENTER);
            minTimePaint.setTextSize(res.getDimension(R.dimen.font_size_time));
            minTimePaint.setTypeface(loadTypeface(R.string.font_share_tech_mono_regular));

            hourTimePaint = new Paint(minTimePaint);

            datePaint = new Paint(minTimePaint);
            datePaint.setTextSize(res.getDimension(R.dimen.font_size_date));

            sdfHour12 = new SimpleDateFormat("hh");
            sdfHour24 = new SimpleDateFormat("HH");
            sdfDate = new SimpleDateFormat("MMM dd");

            time = Calendar.getInstance();

            background = loadScaledBitmapRes(res, R.drawable.watch_bg_normal);
            hourRing = loadScaledBitmapRes(res, R.drawable.hand_hour_normal);
            minuteRing = loadScaledBitmapRes(res, R.drawable.hand_minute_normal);
            secondRing = loadScaledBitmapRes(res, R.drawable.hand_second_normal);

            setColorResources(true); // TODO: implement non-active state
        }

        private void setColorResources(boolean isActive) {
            Resources res = getResources();
            hourTimePaint.setColor(res.getColor(isActive ? R.color.orange : R.color.white));
            minTimePaint.setColor(res.getColor(isActive ? R.color.teal : R.color.white));
            datePaint.setColor(res.getColor(isActive ? R.color.teal : R.color.white));

            float shadowRadius = isActive ? res.getDimension(R.dimen.text_shadow_radius) : 0f;
            hourTimePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.orange_shadow));
            minTimePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.teal_shadow));
            datePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.teal_shadow));
        }

        @Override
        public void onTimeTick() {
            Canvas canvas = getSurfaceHolder().lockCanvas();
            if (canvas == null) {
                return;
            }

            try {
                time.setTimeInMillis(System.currentTimeMillis()); // TODO: set this better
                draw(canvas);
            } finally {
                getSurfaceHolder().unlockCanvasAndPost(canvas);
            }
        }

        private void draw(Canvas canvas) {
            drawFace(canvas);
            drawTime(canvas);
        }

        public void drawFace(Canvas canvas) {
            drawCenteredBitmap(canvas, background);
            drawCenteredBitmap(canvas, hourRing);
            drawCenteredBitmap(canvas, minuteRing);
            drawCenteredBitmap(canvas, secondRing);
        }

        public void drawCenteredBitmap(Canvas canvas, Bitmap bitmap) {
            canvas.drawBitmap(bitmap,
                    canvas.getWidth() / 2f - bitmap.getWidth() / 2f,
                    canvas.getHeight() / 2f - bitmap.getHeight() / 2f,
                    bitmapPaint);
        }

        public void drawTime(Canvas canvas) {
            boolean isActive = true; // TODO: figure this out
            Resources res = getResources();
            int cx = canvas.getWidth() / 2, cy = canvas.getHeight() / 2;

            int min = time.get(Calendar.MINUTE);
            int sec = time.get(Calendar.SECOND);

            float[] screenDimensDp = DeviceUtils.getScreenDimensDp(getApplicationContext());
            float offsetScale = screenDimensDp[0] / 186.666f;

            float vertOffset = res.getDimension(R.dimen.time_vert_offset) * offsetScale;
            float hourOffset = res.getDimension(R.dimen.hour_center_offset) * offsetScale;
            float secondOffset = res.getDimension(R.dimen.hour_center_offset) * offsetScale;
            float dateOffset = res.getDimension(R.dimen.date_center_offset) * offsetScale;

            canvas.drawText((IS_24_HOUR ? sdfHour24 : sdfHour12).format(time.getTimeInMillis()), cx - hourOffset, cy + vertOffset, hourTimePaint);
            canvas.drawText(twoDigitNum(min), cx, cy + vertOffset, minTimePaint);
            canvas.drawText(isActive ? twoDigitNum(sec) : "--", cx + secondOffset, cy + vertOffset, minTimePaint);

            canvas.drawText(sdfDate.format(time.getTimeInMillis()).toUpperCase(), cx, cy + dateOffset, datePaint);
        }

        private String twoDigitNum(int num) {
            return String.format("%02d", num);
        }

        private Bitmap loadScaledBitmapRes(Resources res, @DrawableRes int resId) {
            Bitmap bitmap = BitmapFactory.decodeResource(res, resId);

            float xScale = screenDimensPx[0] / bitmap.getWidth();
            float yScale = screenDimensPx[1] / bitmap.getHeight();
            float scale = Math.min(xScale, yScale);

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        private Typeface loadTypeface(int typefaceNameResId) {
            String typefaceName = getResources().getString(typefaceNameResId);
            return Typeface.createFromAsset(getApplicationContext().getAssets(), typefaceName);
        }
    }
}
