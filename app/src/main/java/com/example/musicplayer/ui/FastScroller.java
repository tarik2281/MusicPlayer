package com.example.musicplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.musicplayer.R;

/**
 * Created by 19tarik97 on 23.01.16.
 */
public class FastScroller extends View {

    private class ScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            computeScrollPosition();
            invalidate();
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    showScrollBar();
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                    hideScrollBar();
                    break;
            }
        }
    }

    private enum ScrollBarState {
        Visible, Hidden, Showing, Hiding
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mState = ScrollBarState.Hiding;
            ViewCompat.postInvalidateOnAnimation(FastScroller.this);
        }
    };

    private static final int HIDE_DELAY = 1000; // in milliseconds

    private static final float SCROLL_HANDLE_MIN_WIDTH = 8.0f;
    private static final float SCROLL_HANDLE_HEIGHT = 75.0f;
    private static final float FLOATING_SPEED = 0.75f;

    private Paint mPaint;
    private float mDensity;
    private int mThumbColor;

    private float mBarX;
    private float mScrollY;
    private float mScrollHandleWidth;
    private float mScrollHandleHeight;
    private float mOpacity;
    private ScrollBarState mState;

    private Handler mHandler;

    private ScrollListener mScrollListener;
    private RecyclerView mRecyclerView;

    public FastScroller(Context context) {
        this(context, null, 0);
    }

    public FastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.fastScrollerStyle);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FastScroller, defStyleAttr, 0);

        mThumbColor = array.getColor(R.styleable.FastScroller_thumbColor, Color.BLACK);

        array.recycle();

        mPaint = new Paint();
        mDensity = getResources().getDisplayMetrics().density;

        mScrollListener = new ScrollListener();
        mHandler = new Handler();

        mOpacity = 0.5f;
        mState = ScrollBarState.Hidden;
        mScrollHandleWidth = mDensity * SCROLL_HANDLE_MIN_WIDTH;
        mScrollHandleHeight = mDensity * SCROLL_HANDLE_HEIGHT;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float minw = mScrollHandleWidth * 1.5f;
        int w = resolveSizeAndState((int)minw, widthMeasureSpec, 1);

        float minh = mDensity * 100.0f;
        int h = resolveSizeAndState((int)minh, heightMeasureSpec, 1);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mState) {
            case Visible: {
                mBarX = 0.0f;
                break;
            }
            case Hidden: {
                mBarX = getWidth();
                break;
            }
            case Showing: {
                mBarX -= mDensity * FLOATING_SPEED;

                if (mBarX <= 0.0f) {
                    mBarX = 0.0f;
                    mState = ScrollBarState.Visible;
                }

                ViewCompat.postInvalidateOnAnimation(this);

                break;
            }
            case Hiding: {
                mBarX += mDensity * FLOATING_SPEED;

                if (mBarX >= getWidth()) {
                    mBarX = getWidth();
                    mState = ScrollBarState.Hidden;
                }

                ViewCompat.postInvalidateOnAnimation(this);

                break;
            }
        }


        mPaint.setARGB((int)(255.0f * mOpacity), Color.red(mThumbColor), Color.green(mThumbColor), Color.blue(mThumbColor));

        canvas.drawRect(mBarX + mScrollHandleWidth / 2.0f, mScrollY, getWidth(), mScrollHandleHeight + mScrollY, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOpacity = 1.0f;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE: {
                showScrollBar();

                RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
                if (adapter != null) {
                    float y = event.getY();
                    float pct = y / ((float)getHeight() - mScrollHandleHeight / 2.0f);
                    float adapterPosition = pct * adapter.getItemCount();
                    adapterPosition = Math.max(0, Math.min(adapterPosition, adapter.getItemCount() - 1));
                    mRecyclerView.scrollToPosition((int)adapterPosition);
                    invalidate();
                }

                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                mOpacity = 0.5f;
                invalidate();
                hideScrollBar();
                break;
            }
        }
        return true;
    }

    public void attachRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.addOnScrollListener(mScrollListener);
    }


    private void showScrollBar() {
        mState = ScrollBarState.Showing;
        ViewCompat.postInvalidateOnAnimation(this);
        mHandler.removeCallbacks(mHideRunnable);
    }

    private void hideScrollBar() {
        mHandler.postDelayed(mHideRunnable, HIDE_DELAY);
    }

    private void computeScrollPosition() {
        float offset = mRecyclerView.computeVerticalScrollOffset();
        float range = mRecyclerView.computeVerticalScrollRange() - mRecyclerView.getHeight();
        mScrollY = offset / range * ((float) getHeight() - mScrollHandleHeight);
    }
}
