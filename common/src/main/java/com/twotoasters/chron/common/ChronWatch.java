package com.twotoasters.chron.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import timber.log.Timber;

public class ChronWatch {

    private static final String FORMAT_12_HOUR = "hh";
    private static final String FORMAT_24_HOUR = "HH";
    private static final String FORMAT_DATE = "MMM dd";

    private static Typeface timeTypeface;

    private Path hourOutlinePath;
    private Paint bitmapPaint, hourTimePaint, minTimePaint, datePaint;
    private Paint hourRingPaint, minRingPaint, secRingPaint;
    private Paint outerMajorTickPaint, outerMinorTickPaint, innerMajorTickPaint, innerMinorTickPaint;
    private Paint hourOutlinePaint;
    private SimpleDateFormat sdfHour12, sdfHour24;
    private SimpleDateFormat sdfDate;

    private Time time;
    private int millisecond; // current millisecond on clock

    boolean is24HourMode;
    boolean mAmbient;
    boolean mLowBitAmbient;
    boolean mBurnInProtection;
    boolean mMute;
    boolean mRound;
    boolean mVisible;

    float width;
    float height;

    private boolean sweepSeconds;

    // TODO: implement amoled state
    // TODO: implement screen burn-in protection

    private int primaryColor, accentColor;
    private Bitmap background;

    private float hourRingThickness, minuteSecondRingThickness, ringShadowSize;
    private float outerTickInnerRadius, outerMajorTickLength, outerMinorTickLength, innerTickInnerRadius, innerMajorTickLength, innerMinorTickLength;

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
        appContext = context.getApplicationContext();
        res = appContext.getResources();

        this.sweepSeconds = sweepSeconds;
        this.width = width;
        this.height = height;

        if (width == 0 || height == 0) {
            int[] screenDimens = Utils.getScreenDimensPx(appContext);
            this.width = screenDimens[0];
            this.height = screenDimens[1];
        }

        loadUnloadedFonts();

        sdfHour12 = new SimpleDateFormat(FORMAT_12_HOUR);
        sdfHour24 = new SimpleDateFormat(FORMAT_24_HOUR);
        sdfDate = new SimpleDateFormat(FORMAT_DATE);
        is24HourMode = DateFormat.is24HourFormat(appContext);
        time = new Time();

        primaryColor = res.getColor(R.color.teal);
        accentColor = res.getColor(R.color.orange);

        createBackgroundBitmap();

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);

        setupTimePaints();
        calculateDimensions();
        setColorResources();
    }

    private void loadUnloadedFonts() {
        if (timeTypeface == null) {
            timeTypeface = Utils.loadTypeface(appContext, R.string.font_share_tech_mono_regular);
        }
    }

    private void createBackgroundBitmap() {
        int backgroundResId;
        if (mAmbient) {
            backgroundResId = mLowBitAmbient ? R.drawable.watch_bg_dimmed_amoled : R.drawable.watch_bg_dimmed;
        } else {
            backgroundResId =  R.drawable.watch_bg_normal_no_ticks_no_hour_grayscale;
        }
        background = Utils.createColorizedImage(Utils.loadScaledBitmapRes(res, backgroundResId, width, height), getPrimaryColor());
    }

    private void setupTimePaints() {
        minTimePaint = new Paint();
        minTimePaint.setAntiAlias(true);
        minTimePaint.setTextAlign(Align.CENTER);
        minTimePaint.setTextSize(res.getDimension(R.dimen.font_size_time)); // TODO: convert to percentage
        minTimePaint.setTypeface(timeTypeface);

        hourTimePaint = new Paint(minTimePaint);

        hourOutlinePaint = new Paint(hourTimePaint);
        hourOutlinePaint.setStyle(Style.STROKE);

        datePaint = new Paint(minTimePaint);
        datePaint.setTextSize(res.getDimension(R.dimen.font_size_date));

        hourRingPaint = new Paint(hourTimePaint);
        hourRingPaint.setStrokeCap(Cap.ROUND);
        hourRingPaint.setStyle(Style.STROKE);

        minRingPaint = new Paint(minTimePaint);
        minRingPaint.setStrokeCap(Cap.ROUND);
        minRingPaint.setStyle(Style.STROKE);
        minRingPaint.setStrokeWidth(minuteSecondRingThickness);

        secRingPaint = new Paint(minRingPaint);

        outerMajorTickPaint = new Paint(hourRingPaint);
        outerMinorTickPaint = new Paint(outerMajorTickPaint);
        innerMajorTickPaint = new Paint(minRingPaint);
        innerMinorTickPaint = new Paint(innerMajorTickPaint);
    }

    public void updatePaintAntiAliasFlag(boolean antiAlias) {
        minTimePaint.setAntiAlias(antiAlias);
        hourTimePaint.setAntiAlias(antiAlias);
        hourOutlinePaint.setAntiAlias(antiAlias);
        datePaint.setAntiAlias(antiAlias);
        hourRingPaint.setAntiAlias(antiAlias);
        minRingPaint.setAntiAlias(antiAlias);
        secRingPaint.setAntiAlias(antiAlias);
        outerMajorTickPaint.setAntiAlias(antiAlias);
        outerMinorTickPaint.setAntiAlias(antiAlias);
        innerMajorTickPaint.setAntiAlias(antiAlias);
        innerMinorTickPaint.setAntiAlias(antiAlias);
    }

    private void calculateDimensions() {
        float smallestDimen = Math.min(width, height);
        float outerTickMarkLength = smallestDimen * 0.0406f;
        float ringSpacing = smallestDimen * 0.0188f;

        ringShadowSize = smallestDimen * 0.0107f;
        hourRingThickness = smallestDimen * 0.0188f;
        minuteSecondRingThickness = smallestDimen * 0.0125f;

        hourRingPaint.setStrokeWidth(hourRingThickness);
        minRingPaint.setStrokeWidth(minuteSecondRingThickness);
        secRingPaint.setStrokeWidth(minuteSecondRingThickness);

        hourOutlinePaint.setStrokeWidth(smallestDimen * 0.003125f);

        hourRingBounds = new RectF(0, 0, width, height);
        hourRingBounds.inset(outerTickMarkLength + ringSpacing / 2f, outerTickMarkLength + ringSpacing / 2f);

        minuteRingBounds = new RectF(hourRingBounds);
        minuteRingBounds.inset(hourRingThickness + ringSpacing, hourRingThickness + ringSpacing);

        ringSpacing += smallestDimen * .00625f;
        secondRingBounds = new RectF(minuteRingBounds);
        secondRingBounds.inset(minuteSecondRingThickness + ringSpacing, minuteSecondRingThickness + ringSpacing);

        outerTickInnerRadius = 0.9643f;
        outerMajorTickLength = smallestDimen * 0.025f;
        outerMinorTickLength = smallestDimen * 0.025f;
        innerTickInnerRadius = 0.6786f;
        innerMajorTickLength = smallestDimen * 0.0125f;
        innerMinorTickLength = smallestDimen * 0.0125f;

        outerMajorTickPaint.setStrokeWidth(smallestDimen * 0.00938f);
        outerMinorTickPaint.setStrokeWidth(smallestDimen * 0.00938f);
        innerMajorTickPaint.setStrokeWidth(smallestDimen * 0.00625f);
        innerMinorTickPaint.setStrokeWidth(smallestDimen * 0.00313f);

        hourOutlinePath = createHourOutlinePath();
    }

    private void setColorResources() {
        // Fill colors
        hourOutlinePaint.setColor(colorWithAlpha(getAccentColor(), 0.5f));

        hourTimePaint.setColor(getAccentColor());
        minTimePaint.setColor(getPrimaryColor());
        datePaint.setColor(getPrimaryColor());

        hourRingPaint.setColor(getAccentColor());
        minRingPaint.setColor(getPrimaryColor());
        secRingPaint.setColor(getPrimaryColor());

        outerMajorTickPaint.setColor(getAccentColor());
        outerMinorTickPaint.setColor(colorWithAlpha(getAccentColor(), 0.25f));
        innerMajorTickPaint.setColor(getPrimaryColor());
        innerMinorTickPaint.setColor(colorWithAlpha(getPrimaryColor(), 0.25f));

        // Shadow colors - // TODO: update shadow color relationship
        float shadowRadius = !mAmbient ? res.getDimension(R.dimen.text_shadow_radius) : 0f;
        hourTimePaint.setShadowLayer(shadowRadius, 0, 0, colorForShadow(getAccentColor()));
        minTimePaint.setShadowLayer(shadowRadius, 0, 0, colorForShadow(getPrimaryColor()));
        datePaint.setShadowLayer(shadowRadius, 0, 0, colorForShadow(getPrimaryColor()));

        float ringShadowRadius = !mAmbient ? ringShadowSize : 0f;
        hourRingPaint.setShadowLayer(ringShadowRadius, 0, 0, colorForShadow(getAccentColor()));
        minRingPaint.setShadowLayer(ringShadowRadius, 0, 0, colorForShadow(getPrimaryColor()));
        secRingPaint.setShadowLayer(ringShadowRadius, 0, 0, colorForShadow(getPrimaryColor()));

        // Gradient colors
        hourRingPaint.setShader(newSweepGradient(!mAmbient ? getAccentColor() : colorWithAlpha(Color.WHITE, 0.55f)));
        minRingPaint.setShader(newSweepGradient(!mAmbient ? getPrimaryColor() : colorWithAlpha(Color.WHITE, 0.55f)));
        secRingPaint.setShader(newSweepGradient(!mAmbient ? getPrimaryColor() : colorWithAlpha(Color.WHITE, 0.55f)));
    }

    private Path createHourOutlinePath() {
        float smallestDimen = Math.min(width, height);
        float w = smallestDimen * 0.175f;   // width of path
        float h = smallestDimen * 0.1643f;  // height of path
        float r = smallestDimen * 0.0107f;  // corner radius path rounded rect

        Matrix matrix = new Matrix();
        matrix.postTranslate(smallestDimen * 0.2f, smallestDimen * 0.4179f);

        Path path = new Path();
        path.moveTo(w - r, 0); // going CW from before top right curve
        path.rCubicTo(r * 0.55f, 0, 0, r * 0.55f, r, r); // top right
        path.lineTo(w, h * 0.25f);

        path.rCubicTo(-r, 0, -r, h * 0.25f, 0, h * 0.25f);
        path.rCubicTo(-r, 0, -r, h * 0.25f, 0, h * 0.25f);

        path.lineTo(w, h - r);
        path.rCubicTo(0, r * 0.55f, r * 0.55f, 0, -r, r); // bottom right
        path.lineTo(r, h);
        path.rCubicTo(-r * 0.55f, 0, 0, r * 0.55f, -r, -r); // bottom left
        path.lineTo(0, r);
        path.rCubicTo(0, -r * 0.55f, -r * 0.55f, 0, r, -r); // top left
        path.close();

        path.transform(matrix);
        return path;
    }

    private SweepGradient newSweepGradient(int gradientColor) {
        int transparentGradientColor = Color.argb(0, Color.red(gradientColor), Color.green(gradientColor), Color.blue(gradientColor));
        return new SweepGradient(width / 2, height / 2, new int[]{transparentGradientColor, gradientColor}, new float[]{0f, 1f});
    }

    /**
     * Updates the color of a UI item according to the given {@code configKey}. Does nothing if
     * {@code configKey} isn't recognized.
     *
     * @return whether UI has been updated
     */
    public boolean updateUiForKey(String configKey, int color) {
        boolean updated = false;
        if (!TextUtils.isEmpty(configKey)) {
            switch (configKey) {
                case Constants.KEY_PRIMARY_COLOR:
                    updated = this.primaryColor != color;
                    setPrimaryColor(color);
                    break;
                case Constants.KEY_ACCENT_COLOR:
                    updated = this.accentColor != color;
                    setAccentColor(color);
                    break;
                default:
                    Timber.w("Ignoring unknown config key: " + configKey);
            }

            if (updated) {
                setColorResources();
                createBackgroundBitmap();
            }
        }
        return updated;
    }

    public void draw(Canvas canvas) {
        drawFace(canvas);
        drawOuterTicks(canvas);
        drawInnerTicks(canvas);
        drawHands(canvas);
        drawTimeOutline(canvas);
        drawTime(canvas);
    }

    private void drawFace(Canvas canvas) {
        canvas.drawBitmap(background,
                width / 2f - background.getWidth() / 2f,
                height / 2f - background.getHeight() / 2f,
                bitmapPaint);
    }

    private void drawOuterTicks(Canvas canvas) {
        float cx = width / 2, cy = height / 2, r = cx;
        for (int degrees = 0; degrees < 360; degrees += 15) {
            boolean isMajor = degrees % 30 == 0;
            float startX = cx + (r * outerTickInnerRadius);
            canvas.save();
            canvas.rotate(degrees, cx, cy);
            canvas.drawLine(startX, cy, startX + (isMajor ? outerMajorTickLength : outerMinorTickLength), cy, (isMajor ? outerMajorTickPaint : outerMinorTickPaint));
            canvas.restore();
        }
    }

    private void drawInnerTicks(Canvas canvas) {
        float cx = width / 2, cy = height / 2, r = cx;
        for (int degrees = 0; degrees < 360; degrees += 6) {
            boolean isMajor = degrees % 30 == 0;
            float startX = cx + (r * innerTickInnerRadius);
            canvas.save();
            canvas.rotate(degrees, cx, cy);
            canvas.drawLine(startX, cy, startX + (isMajor ? innerMajorTickLength : innerMinorTickLength), cy, (isMajor ? innerMajorTickPaint : innerMinorTickPaint));
            canvas.restore();
        }
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
        if (!mAmbient) {
            drawRing(canvas, secondRingBounds, secRingPaint, secondRotation);
        }
    }

    private void drawRing(Canvas canvas, RectF arcBounds, Paint ringPaint, float rotationAngle) {
        canvas.save();
        canvas.rotate(rotationAngle - 87, width / 2f, height / 2f);
        canvas.drawArc(arcBounds, 4, 352, false, ringPaint);
        canvas.restore();
    }

    private void drawTimeOutline(Canvas canvas) {
        if (!mAmbient) {
            canvas.drawPath(hourOutlinePath, hourOutlinePaint);
        }
    }

    private void drawTime(Canvas canvas) {
        int cx = (int) (width / 2f), cy = (int) (height / 2f);

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
        canvas.drawText(Utils.formatTwoDigitNum(min), cx, cy + vertOffset, minTimePaint);
        canvas.drawText(!mAmbient ? Utils.formatTwoDigitNum(sec) : "--", cx + secondOffset, cy + vertOffset, minTimePaint);

        canvas.drawText(sdfDate.format(now).toUpperCase(), cx, cy + dateOffset, datePaint);
    }

    public void setSize(int width, int height) {
        boolean sizeChanged = this.width != width || this.height != height;
        this.width = width;
        this.height = height;
        if (sizeChanged) {
            calculateDimensions();
            createBackgroundBitmap();
        }
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
        mVisible = visible;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public boolean isAmbient() {
        return mAmbient;
    }

    public void setAmbient(boolean ambient) {
        boolean updateResources = mAmbient != ambient;
        mAmbient = ambient;
        if (updateResources) {
            setColorResources();
            createBackgroundBitmap();
        }
    }

    public boolean isLowBitAmbient() {
        return mLowBitAmbient;
    }

    public void setLowBitAmbient(boolean lowBitAmbient) {
        mLowBitAmbient = lowBitAmbient;
    }

    public boolean isBurnInProtection() {
        return mBurnInProtection;
    }

    public void setBurnInProtection(boolean burnInProtection) {
        mBurnInProtection = burnInProtection;
    }

    public void setRound(boolean round) {
        mRound = round;
    }

    public int getPrimaryColor() {
        return !mAmbient ? primaryColor : Color.WHITE;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    private int getAccentColor() {
        return !mAmbient ? accentColor : Color.WHITE;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    private int colorForShadow(int color) {
        float[] hsv = new float[3];
        hsv[1] *= 0.5f;
        hsv[2] *= 0.75f;
        Color.colorToHSV(color, hsv);
        return Color.HSVToColor(179, hsv);
    }

    public int colorWithAlpha(int color, float factor) {
        int alpha = Math.round(255 * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
