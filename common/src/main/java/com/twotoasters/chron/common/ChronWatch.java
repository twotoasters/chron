package com.twotoasters.chron.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ChronWatch {

    private static final boolean IS_24_HOUR = false; // TODO: accomodate the setting

    private Paint bitmapPaint, hourTimePaint, minTimePaint, datePaint;
    private SimpleDateFormat sdfHour12, sdfHour24;
    private SimpleDateFormat sdfDate;

    private Calendar time;

    private int width;
    private int height;
    private boolean sweepSeconds;

    private boolean visible = true;
    private boolean active = true;  // TODO: implement non-active state

    // TODO: implement amoled state
    // TODO: implement screen burn-in protection

    private Bitmap background;
    private Bitmap hourRing, minuteRing, secondRing;

    private Context appContext;
    private Resources res;

    public ChronWatch(Context context) {
        this(context, false);
    }

    public ChronWatch(Context context, boolean sweepSeconds) {
        this(context, sweepSeconds, context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels);
    }

    public ChronWatch(Context context, boolean sweepSeconds, int width, int height) {
        this.sweepSeconds = sweepSeconds;

        appContext = context.getApplicationContext();
        res = appContext.getResources();

        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setAntiAlias(true);

        minTimePaint = new Paint();
        minTimePaint.setAntiAlias(true);
        minTimePaint.setTextAlign(Align.CENTER);
        minTimePaint.setTextSize(res.getDimension(R.dimen.font_size_time));
        minTimePaint.setTypeface(Utils.loadTypeface(appContext, R.string.font_share_tech_mono_regular));

        hourTimePaint = new Paint(minTimePaint);

        datePaint = new Paint(minTimePaint);
        datePaint.setTextSize(res.getDimension(R.dimen.font_size_date));

        sdfHour12 = new SimpleDateFormat("hh");
        sdfHour24 = new SimpleDateFormat("HH");
        sdfDate = new SimpleDateFormat("MMM dd");

        time = Calendar.getInstance();

        setSize(width, height);
        loadImageResources();
        setColorResources();
    }

    private void loadImageResources() {
        int backgroundResId, hourResId, minuteResId, secondResId;
        if (active) {
            backgroundResId =  R.drawable.watch_bg_normal;
            hourResId = R.drawable.hand_hour_normal;
            minuteResId = R.drawable.hand_minute_normal;
            secondResId = R.drawable.hand_second_normal;
        } else {
            boolean hasAmoled = Utils.hasAmoledScreen();
            backgroundResId = hasAmoled ? R.drawable.watch_bg_dimmed_amoled : R.drawable.watch_bg_dimmed;
            hourResId = hasAmoled ? R.drawable.hand_hour_dimmed_amoled : R.drawable.hand_hour_dimmed;
            minuteResId = hasAmoled ? R.drawable.hand_minute_dimmed_amoled : R.drawable.hand_minute_dimmed;
            secondResId = hasAmoled ? R.drawable.hand_second_dimmed_amoled : R.drawable.hand_second_dimmed;
        }
        background = Utils.loadScaledBitmapRes(res, backgroundResId, width, height);
        hourRing = Utils.loadScaledBitmapRes(res, hourResId, width, height);
        minuteRing = Utils.loadScaledBitmapRes(res, minuteResId, width, height);
        secondRing = Utils.loadScaledBitmapRes(res, secondResId, width, height);
    }

    private void setColorResources() {
        hourTimePaint.setColor(res.getColor(active ? R.color.orange : R.color.white));
        minTimePaint.setColor(res.getColor(active ? R.color.teal : R.color.white));
        datePaint.setColor(res.getColor(active ? R.color.teal : R.color.white));

        float shadowRadius = active ? res.getDimension(R.dimen.text_shadow_radius) : 0f;
        hourTimePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.orange_shadow));
        minTimePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.teal_shadow));
        datePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.teal_shadow));
    }

    public void draw(Canvas canvas) {
        drawFace(canvas);
        drawHands(canvas);
        drawTime(canvas);
    }

    private void drawFace(Canvas canvas) {
        drawCenteredBitmapAtAngle(canvas, background, 0);
    }

    private void drawHands(Canvas canvas) {
        int hour = time.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int millisecond = time.get(Calendar.MILLISECOND);

        float hourRotation = 30 * (hour + (minute / 60f) + (second / 3600f));
        float minuteRotation = 6 * (minute + (second / 60f));
        float secondRotation = 6 * (second + (sweepSeconds ? (millisecond / 1000f) : 0));

        drawCenteredBitmapAtAngle(canvas, hourRing, hourRotation);
        drawCenteredBitmapAtAngle(canvas, minuteRing, minuteRotation);
        drawCenteredBitmapAtAngle(canvas, secondRing, secondRotation);
    }

    private void drawCenteredBitmapAtAngle(Canvas canvas, Bitmap bitmap, float rotationAngle) {
        canvas.save();
        canvas.rotate(rotationAngle, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        canvas.drawBitmap(bitmap,
                canvas.getWidth() / 2f - bitmap.getWidth() / 2f,
                canvas.getHeight() / 2f - bitmap.getHeight() / 2f,
                bitmapPaint);
        canvas.restore();
    }

    private void drawTime(Canvas canvas) {
        int cx = canvas.getWidth() / 2, cy = canvas.getHeight() / 2;

        int min = time.get(Calendar.MINUTE);
        int sec = time.get(Calendar.SECOND);

        float[] screenDimensDp = Utils.getScreenDimensDp(appContext);
        float offsetScale = screenDimensDp[0] / 186.666f;

        float vertOffset = res.getDimension(R.dimen.time_vert_offset) * offsetScale;
        float hourOffset = res.getDimension(R.dimen.hour_center_offset) * offsetScale;
        float secondOffset = res.getDimension(R.dimen.hour_center_offset) * offsetScale;
        float dateOffset = res.getDimension(R.dimen.date_center_offset) * offsetScale;

        canvas.drawText((IS_24_HOUR ? sdfHour24 : sdfHour12).format(time.getTimeInMillis()), cx - hourOffset, cy + vertOffset, hourTimePaint);
        canvas.drawText(Utils.twoDigitNum(min), cx, cy + vertOffset, minTimePaint);
        canvas.drawText(active ? Utils.twoDigitNum(sec) : "--", cx + secondOffset, cy + vertOffset, minTimePaint);

        canvas.drawText(sdfDate.format(time.getTimeInMillis()).toUpperCase(), cx, cy + dateOffset, datePaint);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setTime(long timeInMillis) {
        time.setTimeInMillis(timeInMillis);
    }

    public void setVisibility(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setActive(boolean active) {
        boolean updateResources = this.active != active;
        this.active = active;
        if (updateResources) {
            loadImageResources();
            setColorResources();
        }
    }
}
