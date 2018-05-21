package com.example.musicplayer.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

/**
 * Created by Tarik on 10.06.2016.
 */
public class ColorTransitionDrawable extends Drawable implements Animatable {

    private final Runnable mScheduleCallback = new Runnable() {
        @Override
        public void run() {
            update();
        }
    };

    private static final float OVERLAY_RATIO = 0.5f;
    private static final float COLOR_RATIO = 1.0f - OVERLAY_RATIO;
    private static final float ANIMATION_STEP = 0.1f;

    private Paint mPaint;

    private int mCurrentColor;
    private int mNextColor;
    private int mEnqueuedColor;
    private int mResultColor;
    private int mOverlayColor;

    private boolean mHasCurrentColor;
    private boolean mHasNextColor;
    private boolean mHasEnqueuedColor;

    private float mOpacity;

    private boolean mIsRunning;

    public ColorTransitionDrawable() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);

        mCurrentColor = 0;
        mNextColor = 0;
        mEnqueuedColor = 0;

        mHasCurrentColor = false;
        mHasNextColor = false;
        mHasEnqueuedColor = false;

        setResultColor(mCurrentColor);
    }

    @Override
    public void draw(Canvas canvas) {
        mPaint.setColor(mResultColor);
        canvas.drawRect(getBounds(), mPaint);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void start() {
        mIsRunning = true;
        schedule();
    }

    @Override
    public void stop() {
        mIsRunning = false;
        unscheduleSelf(mScheduleCallback);
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    public void setCurrentColor(int color) {
        mCurrentColor = color;
        mHasCurrentColor = true;
        setResultColor(mCurrentColor);
        invalidateSelf();
    }

    public void enqueueColor(int color) {
        if (!mHasCurrentColor) {
            setCurrentColor(color);
        }
        else if (!mHasNextColor) {
            mNextColor = color;
            mHasNextColor = true;
            mOpacity = 0.0f;
            start();
        }
        else {
            mEnqueuedColor = color;
            mHasEnqueuedColor = true;
        }
    }

    public void setOverlayColor(int color) {
        mOverlayColor = color;
    }

    private void update() {
        mOpacity = Math.min(1.0f, mOpacity + ANIMATION_STEP);

        float invOpacity = 1.0f - mOpacity;

        float r = Color.red(mCurrentColor) * invOpacity + Color.red(mNextColor) * mOpacity;
        float g = Color.green(mCurrentColor) * invOpacity + Color.green(mNextColor) * mOpacity;
        float b = Color.blue(mCurrentColor) * invOpacity + Color.blue(mNextColor) * mOpacity;

        setResultColor(r, g, b);

        if (mOpacity >= 1.0f) {
            mCurrentColor = mNextColor;
            mNextColor = mEnqueuedColor;
            mHasNextColor = mHasEnqueuedColor;
            mOpacity = 0.0f;

            mHasEnqueuedColor = false;

            if (!mHasNextColor)
                mIsRunning = false;
        }

        schedule();

        invalidateSelf();
    }

    private void setResultColor(int color) {
        setResultColor(Color.red(color), Color.green(color), Color.blue(color));
    }

    private void setResultColor(float r, float g, float b) {
        r = r * COLOR_RATIO + Color.red(mOverlayColor) * OVERLAY_RATIO;
        g = g * COLOR_RATIO + Color.green(mOverlayColor) * OVERLAY_RATIO;
        b = b * COLOR_RATIO + Color.blue(mOverlayColor) * OVERLAY_RATIO;

        mResultColor = Color.rgb((int)r, (int)g, (int)b);
    }

    private void schedule() {
        if (isRunning())
            scheduleSelf(mScheduleCallback, SystemClock.uptimeMillis() + 1);
    }
}
