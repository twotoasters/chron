package com.twotoasters.chron.widget;

import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView.OnCenterProximityListener;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.twotoasters.chron.R;

public class WearableListItemLayout extends LinearLayout implements OnCenterProximityListener {

    private CircledImageView mCircle;
    private TextView mName;

    public WearableListItemLayout(Context context) {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(getContext(), R.layout.widget_wearable_list_item, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCircle = (CircledImageView) findViewById(R.id.circle);
        mName = (TextView) findViewById(R.id.name);
    }

    // OnCenterProximityListener

    @Override
    public void onCenterPosition(boolean animate) {
        if (animate) {
            mCircle.animate().scaleX(1.4f).scaleY(1.4f);
            mName.animate().alpha(1f);
        } else {
            mCircle.setScaleX(1.4f);
            mCircle.setScaleY(1.4f);
            mName.setAlpha(1f);
        }
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        if (animate) {
            mCircle.animate().scaleX(1f).scaleY(1f);
            mName.animate().alpha(0.6f);
        } else {
            mCircle.setScaleX(1f);
            mCircle.setScaleY(1f);
            mName.setAlpha(0.6f);
        }
    }
}