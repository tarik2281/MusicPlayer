package com.example.musicplayer.ui;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.DimenRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.example.musicplayer.R;

/**
 * Created by Tarik on 07.04.2016.
 */
public class DragLayout extends RelativeLayout {

    public interface Callback {
        void onDragStarted();
        void onDragEnded();
        void onDragPositionChanged(float relX);
        void onDragLayoutToggled(boolean isOpen);
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mDragView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            mIsDragIdle = state == ViewDragHelper.STATE_IDLE;

            if (mCallback != null) {
                if (mIsDragIdle)
                    mCallback.onDragEnded();
                else
                    mCallback.onDragStarted();
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            int position;
            float dragRange;

            if (isVertical()) {
                position = top;
                dragRange = getVerticalDragRange();
            }
            else {
                position = left;
                dragRange = getHorizontalDragRange();
            }

            float pct = Math.abs(position) / dragRange;

            mDragOffset = position;

            centerDragView();

            if (mCallback != null)
                mCallback.onDragPositionChanged(pct);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            boolean inverse = false;
            float sign = 1.0f;

            switch (mGravity) {
                case GRAVITY_TOP:
                case GRAVITY_LEFT:
                    inverse = true;
                    sign = -1.0f;
                    break;
            }

            float velocity;
            int position;
            int halfDragRange;

            if (isVertical()) {
                velocity = yvel;
                position = mDragView.getTop();
                halfDragRange = getVerticalDragRange() / 2;
            }
            else {
                velocity = xvel;
                position = mDragView.getLeft();
                halfDragRange = getHorizontalDragRange() / 2;
            }

            if ((velocity < sign * -VELOCITY_TOLERANCE) ^ inverse)
                open();
            else if ((velocity > sign * VELOCITY_TOLERANCE) ^ inverse)
                close();
            else if ((position - getMinDragPosition() < halfDragRange) ^ inverse)
                open();
            else
                close();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return isVertical() ? 0 : getHorizontalDragRange();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (isVertical())
                return 0;

            return Math.min(Math.max(left, getMinDragPosition()), getMaxDragPosition());
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return isVertical() ? getVerticalDragRange() : 0;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (!isVertical())
                return 0;

            return Math.min(Math.max(top, getMinDragPosition()), getMaxDragPosition());
        }
    }

    public static final int GRAVITY_LEFT = 0x1;
    public static final int GRAVITY_RIGHT = 0x2;
    public static final int GRAVITY_TOP = 0x4;
    public static final int GRAVITY_BOTTOM = 0x8;

    private enum ScrollBarState {
        Visible, Hidden, Showing, Hiding
    }

    private static final float VELOCITY_TOLERANCE = 5.0f;

    private View mDragView;
    private View mContentView;
    private View mProgressBar;
    private int mGravity;
    private int mOffset;
    private Callback mCallback;
    private ViewDragHelper mDragHelper;

    private float mProgressBarOffset;
    private ScrollBarState mProgressBarState;
    private long mLastUpdateTime;

    private boolean mIsOpen;
    private boolean mIsDragIdle;
    private int mDragOffset;

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDragView = null;
        mContentView = null;
        mGravity = GRAVITY_LEFT;
        mOffset = 0;
        mCallback = null;
        mDragHelper = null;
        mIsOpen = false;
        mIsDragIdle = true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        centerDragView();

        int offset = 0;
        switch (mGravity) {
            case GRAVITY_LEFT:
                offset += mOffset / 2;
                break;
            case GRAVITY_RIGHT:
                offset -= mOffset / 2;
                break;
            case GRAVITY_TOP:
            case GRAVITY_BOTTOM:
                break;
        }
        offset += (getMeasuredWidth() - mProgressBar.getMeasuredWidth()) / 2;
        mProgressBar.offsetLeftAndRight(-mProgressBar.getLeft() + offset);

        switch (mProgressBarState) {
            case Hidden:
                mProgressBarOffset = -mProgressBar.getMeasuredHeight();
                mProgressBar.offsetTopAndBottom(-mProgressBar.getTop() - mProgressBar.getMeasuredHeight());
                break;
            case Visible:
                mProgressBarOffset = 0;
                mProgressBar.offsetTopAndBottom(-mProgressBar.getTop());
                break;
            case Showing:
            case Hiding:
                mProgressBar.offsetTopAndBottom(-mProgressBar.getTop() + (int)mProgressBarOffset);
                break;
        }

        // repositioning when layout changed
        if (!mIsDragIdle) {
            if (isVertical())
                mDragView.offsetTopAndBottom(-mDragView.getTop() + mDragOffset);
            else
                mDragView.offsetLeftAndRight(-mDragView.getLeft() + mDragOffset);
        }
        else {
            int position = isOpen() ? getOpenPosition() : getClosedPosition();

            switch (mGravity) {
                case GRAVITY_LEFT:
                case GRAVITY_RIGHT:
                    mDragView.offsetLeftAndRight(-mDragView.getLeft() + position);
                    break;
                case GRAVITY_TOP:
                case GRAVITY_BOTTOM:
                    mDragView.offsetTopAndBottom(-mDragView.getTop() + position);
                    break;
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true) || mProgressBarState == ScrollBarState.Hiding || mProgressBarState == ScrollBarState.Showing)
            ViewCompat.postInvalidateOnAnimation(this);

        switch (mProgressBarState) {
            case Showing: {
                long delta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                mLastUpdateTime = SystemClock.elapsedRealtime();

                float ratio = (float) delta / 250.0f;

                mProgressBarOffset += ratio * (float) mProgressBar.getMeasuredHeight();

                if (mProgressBarOffset >= 0) {
                    mProgressBarOffset = 0;
                    mProgressBarState = ScrollBarState.Visible;
                }

                mProgressBar.offsetTopAndBottom(-mProgressBar.getTop() + (int) mProgressBarOffset);
                break;
            }
            case Hiding: {
                long delta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                mLastUpdateTime = SystemClock.elapsedRealtime();

                float ratio = (float) delta / 250.0f;

                mProgressBarOffset -= ratio * (float) mProgressBar.getMeasuredHeight();

                if (mProgressBarOffset <= -mProgressBar.getMeasuredHeight()) {
                    mProgressBarOffset = -mProgressBar.getMeasuredHeight();
                    mProgressBarState = ScrollBarState.Hidden;
                    mProgressBar.setVisibility(GONE);
                }

                mProgressBar.offsetTopAndBottom(-mProgressBar.getTop() + (int) mProgressBarOffset);
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && !mIsDragIdle)
            return true;

        return mDragHelper.shouldInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !mIsDragIdle)
            return true;

        mDragHelper.processTouchEvent(event);
        invalidate();

        return super.onTouchEvent(event);
    }

    public void initialize() {
        int topPad = 0;
        int bottomPad = 0;
        int leftPad = 0;
        int rightPad = 0;

        switch (mGravity) {
            case GRAVITY_LEFT:
                leftPad = mOffset;
                break;
            case GRAVITY_RIGHT:
                rightPad = mOffset;
                break;
            case GRAVITY_TOP:
                topPad = mOffset;
                break;
            case GRAVITY_BOTTOM:
                bottomPad = mOffset;
                break;
        }

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(leftPad, topPad, rightPad, bottomPad);

        addView(mContentView, params);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        mProgressBar = inflater.inflate(R.layout.progress_bar, this, false);

        addView(mProgressBar);
        mProgressBarState = ScrollBarState.Hidden;
        mProgressBar.setVisibility(GONE);

        addView(mDragView);

        mDragHelper = ViewDragHelper.create(this, 2.0f, new DragHelperCallback());
    }

    public void setDragView(View dragView) {
        mDragView = dragView;
    }

    public void setContentView(View contentView) {
        mContentView = contentView;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    public void setOffset(int offset) {
        mOffset = offset;
    }

    public void setOffsetResource(@DimenRes int resourceId) {
        mOffset = getResources().getDimensionPixelSize(resourceId);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void showProgressBar() {
        // TODO: animate progress bar with ViewPropertyAnimator
        mProgressBar.setVisibility(VISIBLE);
        mProgressBarState = ScrollBarState.Showing;
        invalidate();
        mLastUpdateTime = SystemClock.elapsedRealtime();
    }

    public void dismissProgressBar() {
        mProgressBarState = ScrollBarState.Hiding;
        invalidate();
        mLastUpdateTime = SystemClock.elapsedRealtime();
    }

    public void setProgressBarText(String text) {

    }

    public boolean isOpen() {
        return mIsOpen;
    }

    public void open() {
        boolean toggled = !mIsOpen;

        mIsOpen = true;

        int x = 0;
        int y = 0;

        switch (mGravity) {
            case GRAVITY_LEFT:
            case GRAVITY_RIGHT:
                x = getOpenPosition();
                y = getTop();
                break;
            case GRAVITY_TOP:
            case GRAVITY_BOTTOM:
                x = getLeft();
                y = getOpenPosition();
                break;
        }

        mDragHelper.smoothSlideViewTo(mDragView, x, y);
        invalidate();

        if (mCallback != null && toggled)
            mCallback.onDragLayoutToggled(true);
    }

    public void close() {
        boolean toggled = mIsOpen;

        mIsOpen = false;

        int x = 0;
        int y = 0;

        switch (mGravity) {
            case GRAVITY_LEFT:
            case GRAVITY_RIGHT:
                x = getClosedPosition();
                y = getTop();
                break;
            case GRAVITY_TOP:
            case GRAVITY_BOTTOM:
                x = getLeft();
                y = getClosedPosition();
                break;
        }

        mDragHelper.smoothSlideViewTo(mDragView, x, y);
        invalidate();

        if (mCallback != null && toggled)
            mCallback.onDragLayoutToggled(false);
    }

    public void closeForced() {
        boolean toggled = mIsOpen;

        mIsOpen = false;

        if (mCallback != null && toggled) {
            mCallback.onDragStarted();
            mCallback.onDragPositionChanged(1.0f);
            mCallback.onDragEnded();
            mCallback.onDragLayoutToggled(false);
        }

        requestLayout();
    }

    private int getOpenPosition() {
        switch (mGravity) {
            case GRAVITY_LEFT:
            case GRAVITY_TOP:
                return getMaxDragPosition();
            case GRAVITY_RIGHT:
            case GRAVITY_BOTTOM:
                return getMinDragPosition();
        }

        return 0;
    }

    private int getClosedPosition() {
        switch (mGravity) {
            case GRAVITY_LEFT:
            case GRAVITY_TOP:
                return getMinDragPosition();
            case GRAVITY_RIGHT:
            case GRAVITY_BOTTOM:
                return getMaxDragPosition();
        }

        return 0;
    }

    private void centerDragView() {
        if (isVertical())
            mDragView.offsetLeftAndRight(-mDragView.getLeft() + (getMeasuredWidth() - mDragView.getMeasuredWidth()) / 2);
        else
            mDragView.offsetTopAndBottom(-mDragView.getTop() + (getMeasuredHeight() - mDragView.getMeasuredHeight()) / 2);
    }

    private boolean isVertical() {
        switch (mGravity) {
            case GRAVITY_TOP:
            case GRAVITY_BOTTOM:
                return true;
        }

        return false;
    }

    private int getMinDragPosition() {
        switch (mGravity) {
            case GRAVITY_LEFT:
                return -mDragView.getMeasuredWidth() + mOffset;
            case GRAVITY_RIGHT:
                return getMeasuredWidth() - mDragView.getMeasuredWidth();
            case GRAVITY_TOP:
                return -mDragView.getMeasuredHeight() + mOffset;
            case GRAVITY_BOTTOM:
                return getMeasuredHeight() - mDragView.getMeasuredHeight();
        }

        return 0;
    }

    private int getMaxDragPosition() {
        switch (mGravity) {
            case GRAVITY_LEFT:
                return 0;
            case GRAVITY_RIGHT:
                return getMeasuredWidth() - mOffset;
            case GRAVITY_TOP:
                return 0;
            case GRAVITY_BOTTOM:
                return getMeasuredHeight() - mOffset;
        }

        return 0;
    }

    private int getHorizontalDragRange() {
        return mDragView.getMeasuredWidth() - mOffset;
    }

    private int getVerticalDragRange() {
        return mDragView.getMeasuredHeight() - mOffset;
    }
}
