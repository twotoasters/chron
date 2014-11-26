package com.twotoasters.chron.widget;

import android.content.Context;
import android.support.wearable.view.WearableListView.OnCenterProximityListener;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.twotoasters.chron.R;

public class WearableListItemLayout extends LinearLayout implements OnCenterProximityListener {

    private final float mFadedTextAlpha;
    private final int mFadedCircleColor;
    private final int mChosenCircleColor;
    private ImageView mCircle;
    private float mScale;
    private TextView mName;

    public WearableListItemLayout(Context context) {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
        mFadedCircleColor = getResources().getColor(R.color.grey);
        mChosenCircleColor = getResources().getColor(R.color.blue);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCircle = (ImageView) findViewById(R.id.circle);
        mName = (TextView) findViewById(R.id.name);
    }

    // OnCenterProximityListener

    @Override
    public void onCenterPosition(boolean b) {
        mCircle.animate().alpha(1f).scaleX(1.6f);
        mName.animate().alpha(1f);
    }

    @Override
    public void onNonCenterPosition(boolean b) {
        mCircle.animate().alpha(0.6f).scaleX(1f);
        mName.animate().alpha(0.6f);
    }
}