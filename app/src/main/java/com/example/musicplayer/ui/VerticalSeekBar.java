package com.example.musicplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Tarik on 23.12.2015.
 */
public class VerticalSeekBar extends AppCompatSeekBar {

    private OnSeekBarChangeListener mOnChangeListener;
    private boolean mIsDragging;

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        this.mOnChangeListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                attemptClaimDrag();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                }
                else {
                    //setPressed(true);
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    attemptClaimDrag();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    //setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    //setPressed(false);
                }

                invalidate();
                break;
        }
        return true;
    }

    private void trackTouchEvent(MotionEvent event) {
        final int height = getHeight();
        final int available = height - getPaddingTop() - getPaddingBottom();
        final int y = (int)event.getY();

        float scale;

        if (y < getPaddingTop()) {
            scale = 1.0f;
        }
        else if (y > height - getPaddingBottom()) {
            scale = 0.0f;
        }
        else {
            scale = (float)((getHeight() - y) - getPaddingTop()) / (float)available;
        }

        final int max = getMax();
        setProgressInternal((int)(scale * max), true);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    private void setProgressInternal(int progress, boolean fromUser) {
        setProgress(progress);
        if (mOnChangeListener != null)
            mOnChangeListener.onProgressChanged(this, progress, fromUser);
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void onStartTrackingTouch() {
        mIsDragging = true;

        if (mOnChangeListener != null) {
            mOnChangeListener.onStartTrackingTouch(this);
        }
    }

    private void onStopTrackingTouch() {
        mIsDragging = false;

        if (mOnChangeListener != null) {
            mOnChangeListener.onStopTrackingTouch(this);
        }
    }

}