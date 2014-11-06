package com.twotoasters.chron.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.text.format.DateFormat;
import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ChronWatch {

    private static final int[] orangeGradientColors = new int[]{Color.TRANSPARENT, Color.parseColor("#2e2a1d"), Color.parseColor("#796125"), Color.parseColor("#dcaf2f"), Color.parseColor("#ffbe35")};
    private static final int[] tealGradientColors = new int[]{Color.TRANSPARENT, Color.parseColor("#14292b"), Color.parseColor("#1b6165"), Color.parseColor("#1db9bd"), Color.parseColor("#04ffff")};
    private static final int[] grayGradientColors = new int[]{Color.TRANSPARENT, Color.parseColor("#070707"), Color.parseColor("#1a1a1a"), Color.parseColor("#404040"), Color.parseColor("#7f7f7f")};

    private static final String FORMAT_12_HOUR = "hh";
    private static final String FORMAT_24_HOUR = "HH";
    private static final String FORMAT_DATE = "MMM dd";

    private Paint bitmapPaint, hourTimePaint, minTimePaint, datePaint;
    private Paint hourRingPaint, minRingPaint, secRingPaint;
    private SimpleDateFormat sdfHour12, sdfHour24;
    private SimpleDateFormat sdfDate;

    private Time time;
    private int millisecond; // current millisecond on clock

    private int width;
    private int height;
    private boolean sweepSeconds;

    private boolean visible = true;
    private boolean interactive = true;
    private boolean is24HourMode = false;

    // TODO: implement amoled state
    // TODO: implement screen burn-in protection

    private Bitmap background;

    private float hourRingThickness, minuteSecondRingThickness, ringShadowSize;

    private RectF hourRingBounds, minuteRingBounds, secondRingBounds;

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
        setSize(width, height);

        appContext = context.getApplicationContext();
        res = appContext.getResources();

        time = new Time();
        is24HourMode = DateFormat.is24HourFormat(appContext);

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);

        minTimePaint = new Paint();
        minTimePaint.setAntiAlias(true);
        minTimePaint.setTextAlign(Align.CENTER);
        minTimePaint.setTextSize(res.getDimension(R.dimen.font_size_time));
        minTimePaint.setTypeface(Utils.loadTypeface(appContext, R.string.font_share_tech_mono_regular));

        hourTimePaint = new Paint(minTimePaint);

        datePaint = new Paint(minTimePaint);
        datePaint.setTextSize(res.getDimension(R.dimen.font_size_date));

        hourRingPaint = new Paint(hourTimePaint);
        hourRingPaint.setStrokeCap(Cap.ROUND);
        hourRingPaint.setStyle(Style.STROKE);
        hourRingPaint.setStrokeWidth(hourRingThickness);

        minRingPaint = new Paint(minTimePaint);
        minRingPaint.setStrokeCap(Cap.ROUND);
        minRingPaint.setStyle(Style.STROKE);
        minRingPaint.setStrokeWidth(minuteSecondRingThickness);

        secRingPaint = new Paint(minRingPaint);

        sdfHour12 = new SimpleDateFormat(FORMAT_12_HOUR);
        sdfHour24 = new SimpleDateFormat(FORMAT_24_HOUR);
        sdfDate = new SimpleDateFormat(FORMAT_DATE);

        loadImageResources();
        setColorResources();
    }

    private void calculateDimensions() {
        float smallestDimen = Math.min(width, height);
        float outerTickMarkLength = smallestDimen * 0.0406f;
        float ringSpacing = smallestDimen * 0.0188f;
        ringShadowSize = smallestDimen * 0.0107f;
        hourRingThickness = smallestDimen * 0.0188f;
        minuteSecondRingThickness = smallestDimen * 0.0125f;

        hourRingBounds = new RectF(0, 0, width, height);
        hourRingBounds.inset(outerTickMarkLength + ringSpacing / 2f, outerTickMarkLength + ringSpacing / 2f);

        minuteRingBounds = new RectF(hourRingBounds);
        minuteRingBounds.inset(hourRingThickness + ringSpacing, hourRingThickness + ringSpacing);

        secondRingBounds = new RectF(minuteRingBounds);
        secondRingBounds.inset(minuteSecondRingThickness + ringSpacing, minuteSecondRingThickness + ringSpacing);
    }

    private void loadImageResources() {
        int backgroundResId;
        if (interactive) {
            backgroundResId =  R.drawable.watch_bg_normal;
        } else {
            boolean hasAmoled = Utils.hasAmoledScreen();
            backgroundResId = hasAmoled ? R.drawable.watch_bg_dimmed_amoled : R.drawable.watch_bg_dimmed;
        }
        background = Utils.loadScaledBitmapRes(res, backgroundResId, width, height);
    }

    private void setColorResources() {
        // Fill colors
        hourTimePaint.setColor(res.getColor(interactive ? R.color.orange : R.color.white));
        minTimePaint.setColor(res.getColor(interactive ? R.color.teal : R.color.white));
        datePaint.setColor(res.getColor(interactive ? R.color.teal : R.color.white));

        hourRingPaint.setColor(res.getColor(interactive ? R.color.orange : R.color.white));
        minRingPaint.setColor(res.getColor(interactive ? R.color.teal : R.color.white));
        secRingPaint.setColor(res.getColor(interactive ? R.color.teal : R.color.white));

        // Shadow colors
        float shadowRadius = interactive ? res.getDimension(R.dimen.text_shadow_radius) : 0f;
        hourTimePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.orange_shadow));
        minTimePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.teal_shadow));
        datePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(R.color.teal_shadow));

        float ringShadowRadius = interactive ? ringShadowSize : 0f;
        hourRingPaint.setShadowLayer(ringShadowRadius, 0, 0, res.getColor(R.color.orange_shadow));
        minRingPaint.setShadowLayer(ringShadowRadius, 0, 0, res.getColor(R.color.teal_shadow));
        secRingPaint.setShadowLayer(ringShadowRadius, 0, 0, res.getColor(R.color.teal_shadow));

        // Gradient colors
        hourRingPaint.setShader(newSweepGradient(interactive ? orangeGradientColors : grayGradientColors));
        minRingPaint.setShader(newSweepGradient(interactive ? tealGradientColors : grayGradientColors));
        secRingPaint.setShader(newSweepGradient(interactive ? tealGradientColors : grayGradientColors));
    }

    private SweepGradient newSweepGradient(int[] gradientColors) {
        float[] gradientPositions = new float[]{0.0f, 0.08f, 0.25f, 0.5f, 1.0f};
        return new SweepGradient(width / 2, height / 2, gradientColors, gradientPositions);
    }

    public void draw(Canvas canvas) {
        drawFace(canvas);
        drawHands(canvas);
        drawTime(canvas);
    }

    private void drawFace(Canvas canvas) {
        canvas.drawBitmap(background,
                canvas.getWidth() / 2f - background.getWidth() / 2f,
                canvas.getHeight() / 2f - background.getHeight() / 2f,
                bitmapPaint);
    }

    private void drawHands(Canvas canvas) {
        int hour = time.hour;
        int minute = time.minute;
        int second = time.second;

        float hourRotation = 30 * (hour + (minute / 60f) + (second / 3600f));
        float minuteRotation = 6 * (minute + (second / 60f));
        float secondRotation = 6 * (second + (sweepSeconds ? (millisecond / 1000f) : 0));

        drawRing(canvas, hourRingBounds, hourRingPaint, hourRotation);
        drawRing(canvas, minuteRingBounds, minRingPaint, minuteRotation);
        if (interactive) {
            drawRing(canvas, secondRingBounds, secRingPaint, secondRotation);
        }
    }

    private void drawRing(Canvas canvas, RectF arcBounds, Paint ringPaint, float rotationAngle) {
        canvas.save();
        canvas.rotate(rotationAngle - 87, width / 2f, height / 2f);
        canvas.drawArc(arcBounds, 4, 352, false, ringPaint);
        canvas.restore();
    }

    private void drawTime(Canvas canvas) {
        int cx = canvas.getWidth() / 2, cy = canvas.getHeight() / 2;

        int min = time.minute;
        int sec = time.second;

        float[] screenDimensDp = Utils.getScreenDimensDp(appContext);
        float offsetScale = screenDimensDp[0] / 186.666f;

        float vertOffset = res.getDimension(R.dimen.time_vert_offset) * offsetScale;
        float hourOffset = res.getDimension(R.dimen.hour_center_offset) * offsetScale;
        float secondOffset = res.getDimension(R.dimen.hour_center_offset) * offsetScale;
        float dateOffset = res.getDimension(R.dimen.date_center_offset) * offsetScale;

        long now = time.toMillis(false);
        canvas.drawText((is24HourMode ? sdfHour24 : sdfHour12).format(now), cx - hourOffset, cy + vertOffset, hourTimePaint);
        canvas.drawText(Utils.twoDigitNum(min), cx, cy + vertOffset, minTimePaint);
        canvas.drawText(interactive ? Utils.twoDigitNum(sec) : "--", cx + secondOffset, cy + vertOffset, minTimePaint);

        canvas.drawText(sdfDate.format(now).toUpperCase(), cx, cy + dateOffset, datePaint);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        calculateDimensions();
    }

    public void setTime(long now) {
        time.set(now);
        millisecond = (int) (now % 1000);
    }

    public void setTimeToNow() {
        time.setToNow();
    }

    public void clearTime(String timeZone) {
        time.clear(timeZone);
        sdfHour12.setTimeZone(TimeZone.getTimeZone(timeZone));
        sdfHour24.setTimeZone(TimeZone.getTimeZone(timeZone));
        sdfDate.setTimeZone(TimeZone.getTimeZone(timeZone));
    }

    public void set24HourModeEnabled(boolean is24HourMode) {
        this.is24HourMode = is24HourMode;
    }

    public void setVisibility(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setInteractive(boolean interactive) {
        boolean updateResources = this.interactive != interactive;
        this.interactive = interactive;
        if (updateResources) {
            loadImageResources();
            setColorResources();
        }
    }
}
