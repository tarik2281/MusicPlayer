package com.example.musicplayer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by Tarik on 30.05.2016.
 */
public class OffsetLayout extends FrameLayout {

    private boolean mUseOffset;
    private int mOffset;

    public OffsetLayout(Context context) {
        super(context);
    }

    public OffsetLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OffsetLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        offset();
    }

    public void setUseOffset(boolean useOffset) {
        mUseOffset = useOffset;

        offset();
    }

    public void setOffset(int offset) {
        mOffset = offset;

        offset();
    }

    private void offset() {
        if (mUseOffset)
            offsetLeftAndRight(-getLeft() + mOffset);
    }
}
