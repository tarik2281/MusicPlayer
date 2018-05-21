package com.example.musicplayer.ui;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.musicplayer.R;

/**
 * Created by 19tar on 11.09.2017.
 */

public class TwoPageDragLayout extends RelativeLayout {

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mArrowView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            mIsDragIdle = state == ViewDragHelper.STATE_IDLE;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mDragOffset = left;

            mLeftView.offsetLeftAndRight(dx);
            mRightView.offsetLeftAndRight(dx);

            setArrowRotation(mDragOffset);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (xvel < -VELOCITY_TOLERANCE)
                moveToRight(true);
            else if (xvel > VELOCITY_TOLERANCE)
                moveToLeft(true);
            else if (mArrowView.getLeft() - getMinDragPosition() < getDragRange() / 2)
                moveToRight(true);
            else
                moveToLeft(true);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return 0;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return 0;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return Math.min(Math.max(left, getMinDragPosition()), getMaxDragPosition());
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getDragRange();
        }
    }

    public TwoPageDragLayout(Context context) {
        super(context);
    }

    public TwoPageDragLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoPageDragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    // minimum velocity for swiping
    private static final float VELOCITY_TOLERANCE = 5.0f;

    private View mLeftView;
    private View mRightView;
    private int mCurrentPage;

    private ImageView mArrowView;
    private int mOffset;

    private boolean mIsDragIdle;
    private int mDragOffset;

    private ViewDragHelper mDragHelper;

    public void initialize() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mArrowView = (ImageView)inflater.inflate(R.layout.arrow_image_view, this, false);

        addView(mArrowView);

        mOffset = getResources().getDimensionPixelSize(R.dimen.two_page_drag_offset);

        LayoutParams leftParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        leftParams.setMargins(0, 0, mOffset, 0);

        LayoutParams rightParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rightParams.setMargins(mOffset, 0, 0, 0);

        addView(mLeftView, leftParams);
        addView(mRightView, rightParams);

        mDragHelper = ViewDragHelper.create(this, 2.0f, new DragHelperCallback());
        mIsDragIdle = true;

        mArrowView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int page, boolean animate) {
        switch (page) {
            case LEFT:
                moveToLeft(animate);
                break;
            case RIGHT:
                moveToRight(animate);
                break;
        }
    }

    public void setLeftView(View view) {
        mLeftView = view;
    }

    public void setRightView(View view) {
        mRightView = view;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // all views positioned from zero
        int leftOffset = 0;
        int arrowOffset = 0;
        int rightOffset = 0;

        int minDrag = getMinDragPosition();
        int maxDrag = getMaxDragPosition();

        if (!mIsDragIdle) {
            // reposition views when layout changed while dragging
            leftOffset = mDragOffset - maxDrag;
            arrowOffset = mDragOffset;
            rightOffset = mDragOffset + mOffset;
        }
        else {
            switch (mCurrentPage) {
                case LEFT:
                    leftOffset = minDrag;
                    arrowOffset = maxDrag;
                    rightOffset = maxDrag + mOffset;
                    break;
                case RIGHT:
                    leftOffset = -maxDrag;
                    arrowOffset = minDrag;
                    rightOffset = minDrag + mOffset;
                    break;
            }

            setArrowRotation(arrowOffset);
        }

        mLeftView.offsetLeftAndRight(leftOffset - mLeftView.getLeft());
        mArrowView.offsetLeftAndRight(arrowOffset - mArrowView.getLeft());
        mRightView.offsetLeftAndRight(rightOffset - mRightView.getLeft());
    }

    private int getMinDragPosition() {
        return 0;
    }

    private int getMaxDragPosition() {
        return getMeasuredWidth() - mOffset;
    }

    private int getDragRange() {
        return getMaxDragPosition() - getMinDragPosition();
    }

    private void setArrowRotation(int position) {
        position -= getMinDragPosition(); // transform to dragRange space
        int dragRange = getDragRange();

        float pct = (float)Math.abs(dragRange - position) / (float)dragRange;
        mArrowView.setRotation(180.0f * pct);
    }

    private void moveToLeft(boolean animate) {
        if (animate) {
            mDragHelper.smoothSlideViewTo(mArrowView, getMaxDragPosition(), 0);
            invalidate();
        }

        mCurrentPage = LEFT;
    }

    private void moveToRight(boolean animate) {
        if (animate) {
            mDragHelper.smoothSlideViewTo(mArrowView, getMinDragPosition(), 0);
            invalidate();
        }

        mCurrentPage = RIGHT;
    }
}
