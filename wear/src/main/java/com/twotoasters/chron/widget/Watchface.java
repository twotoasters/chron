package com.twotoasters.chron.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.twotoasters.chron.common.ChronWatch;
import com.twotoasters.watchface.gears.widget.IWatchface;
import com.twotoasters.watchface.gears.widget.Watch;

import java.util.Calendar;

import hugo.weaving.DebugLog;

public class Watchface extends FrameLayout implements IWatchface {

    private Watch mWatch;
    private ChronWatch mChronWatch;

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
        mWatch = new Watch(this);
        mChronWatch = new ChronWatch(context);
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
        mChronWatch.draw(canvas);
    }

    @Override
    public void onTimeChanged(Calendar time) {
        mChronWatch.setTime(time.getTimeInMillis());
        invalidate();
    }

    @Override
    public boolean handleSecondsInDimMode() {
        return false;
    }

    @Override
    @DebugLog
    public void onActiveStateChanged(boolean active) {
        mChronWatch.setActive(active);
    }
}
