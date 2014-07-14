package com.twotoasters.chron.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.twotoasters.chron.R;
import com.twotoasters.watchface.gears.widget.IWatchface;
import com.twotoasters.watchface.gears.widget.Watch;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class Watchface extends FrameLayout implements IWatchface {

    @InjectView(R.id.face)              ImageView face;
    @InjectView(R.id.hand_hour)         ImageView handHour;
    @InjectView(R.id.hand_minute)       ImageView handMinute;
    @InjectView(R.id.hand_second)       ImageView handSecond;

    private Paint hourTimePaint, minTimePaint, datePaint;
    private SimpleDateFormat sdfDate;

    private Watch mWatch;

    private boolean mInflated;
    private boolean mActive;

    public Watchface(Context context) {
        super(context);
        init(context, null, 0);
    }

    public Watchface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public Watchface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    @DebugLog
    private void init(Context context, AttributeSet attrs, int defStyle) {
        Resources res = getResources();

        mWatch = new Watch(this);

        minTimePaint = new Paint();
        minTimePaint.setAntiAlias(true);
        minTimePaint.setTextAlign(Align.CENTER);
        minTimePaint.setTextSize(res.getDimension(R.dimen.font_size_time));
        minTimePaint.setTypeface(loadTypeface(R.string.font_share_tech_mono_regular));

        hourTimePaint = new Paint(minTimePaint);

        datePaint = new Paint(minTimePaint);
        datePaint.setTextSize(res.getDimension(R.dimen.font_size_date));

        sdfDate = new SimpleDateFormat("MMM dd");

        setColorResources();
    }

    @DebugLog
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this, getRootView());
        mInflated = true;
    }

    @DebugLog
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWatch.onAttachedToWindow();
    }

    @DebugLog
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWatch.onDetachedFromWindow();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawTime(canvas, mWatch.getTime(), mWatch.is24HourModeEnabled());
    }

    public void drawTime(Canvas canvas, Calendar time, boolean is24Hour) {
        Resources res = getResources();
        int cx = getWidth() / 2, cy = getHeight() / 2;

        int hr = is24Hour ? time.get(Calendar.HOUR) : time.get(Calendar.HOUR) % 12;
        int min = time.get(Calendar.MINUTE);
        int sec = time.get(Calendar.SECOND);

        // TODO: need to multiply all of these values by the scale factor (widthInPx / 213.333)
        float vertOffset = res.getDimension(R.dimen.time_vert_offset);
        float hourOffset = res.getDimension(R.dimen.hour_center_offset);
        float secondOffset = res.getDimension(R.dimen.hour_center_offset);
        float dateOffset = res.getDimension(R.dimen.date_center_offset);

        canvas.drawText(twoDigitNum(hr), cx - hourOffset, cy + vertOffset, hourTimePaint);
        canvas.drawText(twoDigitNum(min), cx, cy + vertOffset, minTimePaint);
        canvas.drawText(twoDigitNum(sec), cx + secondOffset, cy + vertOffset, minTimePaint); // TODO: evaluate what to do when not active

        canvas.drawText(sdfDate.format(time.getTimeInMillis()).toUpperCase(), cx, cy + dateOffset, datePaint);
    }

    private String twoDigitNum(int num) {
        return String.format("%02d", num);
    }

    private void rotateHands(int hour, int minute, int second) {
        int rotHr = (int) (30 * hour + 0.5f * minute);
        int rotMin = 6 * minute;
        int rotSec = 6 * second;

        handHour.setRotation(rotHr);
        handMinute.setRotation(rotMin);
        handSecond.setRotation(rotSec);
    }

    @Override
    public void onTimeChanged(Calendar time) {
        Timber.v("onTimeChanged()");

        int hr = time.get(Calendar.HOUR_OF_DAY) % 12;
        int min = time.get(Calendar.MINUTE);
        int sec = time.get(Calendar.SECOND);

        rotateHands(hr, min, sec);
        invalidate();
    }

    @Override
    @DebugLog
    public void onActiveStateChanged(boolean active) {
        this.mActive = active;
        setImageResources();
        setColorResources();
    }

    @DebugLog
    private void setImageResources() {
        if (mInflated) {
            face.setImageResource(mActive ? R.drawable.watch_bg_normal : R.drawable.watch_bg_dimmed);
            handHour.setImageResource(mActive ? R.drawable.hand_hour_normal : R.drawable.hand_hour_dimmed);
            handMinute.setImageResource(mActive ? R.drawable.hand_minute_normal : R.drawable.hand_minute_dimmed);
            handSecond.setImageResource(mActive ? R.drawable.hand_second_normal : R.drawable.hand_second_dimmed);
            handSecond.setVisibility(mActive ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @DebugLog
    private void setColorResources() {
        Resources res = getResources();
        hourTimePaint.setColor(res.getColor(mActive ? R.color.orange : R.color.white));
        minTimePaint.setColor(res.getColor(mActive ? R.color.teal : R.color.white));
        datePaint.setColor(res.getColor(mActive ? R.color.teal : R.color.white));
    }

    private Typeface loadTypeface(int typefaceNameResId) {
        String typefaceName = getResources().getString(typefaceNameResId);
        return Typeface.createFromAsset(getContext().getAssets(), typefaceName);
    }
}
