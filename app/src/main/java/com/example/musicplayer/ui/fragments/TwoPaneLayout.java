package com.example.musicplayer.ui.fragments;

import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.example.musicplayer.ui.OffsetLayout;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Tarik on 25.05.2016.
 */
public class TwoPaneLayout extends RelativeLayout {

    public interface Callback {
        void onActiveFragmentChanged(PaneFragment fragment);
    }

    public class Entry {
        private OffsetLayout mLayout;
        private PaneFragment mFragment;
        private int mWidth;

        public Entry(int id) {
            mLayout = new OffsetLayout(getContext());
            mLayout.setId(id);
            addView(mLayout, makeLayoutParams());

            mFragment = null;
            mWidth = 0;
        }

        public PaneFragment getFragment() {
            return mFragment;
        }

        public void setFragment(PaneFragment fragment) {
            if (mFragment == fragment)
                return;

            FragmentTransaction transaction = mFragmentManager.beginTransaction();

            if (mFragment != null) {
                transaction.remove(mFragment);
                mFragment.setParent(null);
            }

            if (fragment != null) {
                transaction.add(mLayout.getId(), fragment, String.valueOf(mLayout.getId()));
                fragment.setParent(TwoPaneLayout.this);

                if (mWidth > 0)
                    fragment.setWidth(mWidth);
            }

            mFragment = fragment;

            if (fragment != null && this == mRightEntry && mCallback != null) {
                mCallback.onActiveFragmentChanged(fragment);
                // TODO: consistency when setting fragment through the entry
            }

            transaction.commit();
        }

        public void restoreFragment() {
            mFragment = (PaneFragment)mFragmentManager.findFragmentByTag(String.valueOf(mLayout.getId()));
            mFragment.setParent(TwoPaneLayout.this);
        }

        public void setWidth(int width) {
            mFragment.setWidth(width);

            mWidth = width;

            mLayout.getLayoutParams().width = width;
        }

        public void setPosition(int position) {
            mLayout.setUseOffset(false);
            mLayout.setOffset(0);
            margin(position);
        }

        public void setOffset(int offset) {
            margin(0);
            mLayout.setUseOffset(true);
            mLayout.setOffset(offset);
        }

        public void setState(int state) {
            mFragment.setMenuVisibility(state == PaneFragment.STATE_ACTIVE);

            if (mFragment != null)
                mFragment.setState(state);
        }

        public void show() {
            mLayout.setVisibility(VISIBLE);
        }

        public void hide() {
            mLayout.setVisibility(GONE);
        }

        public void release() {
            setFragment(null);
            removeView(mLayout);
        }

        private void margin(int margin) {
            ((LayoutParams)mLayout.getLayoutParams()).setMargins(margin, 0, 0, 0);
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return !mSinglePane && mPreviousEntry != null && (child == mLeftEntry.mLayout || child == mRightEntry.mLayout);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_IDLE)
                onIdle();
            else if (mPreviousEntry != null && mCurrentState != STATE_ADDING)
                mPreviousEntry.show();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            invalidate();

            if (capturedChild == mRightEntry.mLayout)
                mDragHelper.captureChildView(mLeftEntry.mLayout, activePointerId);

            mCurrentState = STATE_BUSY;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mSinglePane ? 2 * mLeftWidth : mRightWidth;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (child == mLeftEntry.mLayout)
                return Math.max(Math.min(left, mLeftWidth), 0);
            else if (child == mRightEntry.mLayout)
                return Math.max(Math.min(left, 2 * mLeftWidth), mLeftWidth);

            return 0;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {

            if (!mSinglePane) {
                float diff = (float) (mRightWidth - mLeftWidth) * (float) Math.abs(left) / (float) mLeftWidth;

                if (left >= 0) {
                    int leftWidth = (int) (mLeftWidth + diff);
                    mPreviousEntry.setWidth(mLeftWidth);
                    mPreviousEntry.setOffset(-mLeftWidth + left);

                    mLeftEntry.setWidth(leftWidth);
                    mLeftEntry.setOffset(left);

                    mRightEntry.setWidth(mRightWidth);
                    mRightEntry.setOffset(left + leftWidth);
                } else {
                    int rightWidth = (int) (mRightWidth - diff);
                    int right = left + mLeftWidth;

                    mLeftEntry.setWidth(mLeftWidth);
                    mLeftEntry.setOffset(left);

                    mRightEntry.setWidth(rightWidth);
                    mRightEntry.setOffset(right);

                    mNextEntry.setWidth(mRightWidth);
                    mNextEntry.setOffset(right + rightWidth);
                }
            }
            else {
                if (mPreviousEntry != null) {
                    mPreviousEntry.setWidth(mPreviousWidth);
                    mPreviousEntry.setOffset(-mPreviousWidth + left);
                }

                if (mRightEntry != null) {
                    mRightEntry.setWidth(mRightWidth);
                    mRightEntry.setOffset(left);
                }

                if (mNextEntry != null) {
                    mNextEntry.setWidth(mRightWidth);
                    mNextEntry.setOffset(mRightWidth + left);
                }
            }

            requestLayout();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (releasedChild == mLeftEntry.mLayout) {
                if (xvel > VELOCITY_TOLERANCE)
                    back();
                else if (xvel < -VELOCITY_TOLERANCE)
                    settle();
                else if (mLeftEntry.mLayout.getLeft() > mLeftWidth / 2)
                    back();
                else if (mLeftEntry.mLayout.getLeft() <= mLeftWidth / 2)
                    settle();
            }
        }
    }

    private static class SavedState implements Parcelable {

        private int[] mPreviousIds;
        private int mPreviousId;
        private int mLeftId;
        private int mRightId;

        public SavedState() {

        }

        protected SavedState(Parcel in) {
            mPreviousId = in.readInt();
            mLeftId = in.readInt();
            mRightId = in.readInt();
            mPreviousIds = in.createIntArray();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(mPreviousId);
            parcel.writeInt(mLeftId);
            parcel.writeInt(mRightId);
            parcel.writeIntArray(mPreviousIds);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Transition delay to the next fragment in milliseconds
     */
    private static final int TRANSITION_DELAY = 250;

    /**
     * Sensitivity for drag starting
     */
    private static final float SENSITIVITY = 0.3f;
    private static final float VELOCITY_TOLERANCE = 5.0f;
    private static final float LEFT_WIDTH_RATIO = 2.0f / 5.0f;

    private static final int STATE_IDLE = 0;
    private static final int STATE_ADDING = 1;
    private static final int STATE_REMOVING = 2;
    private static final int STATE_BUSY = 3;

    private ViewDragHelper mDragHelper;
    private FragmentManager mFragmentManager;

    private boolean mSinglePane;
    private int mCurrentState;
    private int mNumBackRequests;

    private int mPreviousWidth;
    private int mLeftWidth;
    private int mRightWidth;

    // in single pane mode left entry is not used
    private LinkedList<Entry> mPreviousEntries;
    private Entry mPreviousEntry;
    private Entry mLeftEntry;
    private Entry mRightEntry;
    private Entry mNextEntry;

    private Callback mCallback;
    private Handler mHandler;

    private Runnable mNextTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCallback != null)
                mCallback.onActiveFragmentChanged(mNextEntry.mFragment);
            mNextEntry.setState(PaneFragment.STATE_ACTIVE);

            if (!mSinglePane) {
                mLeftEntry.setState(PaneFragment.STATE_HIDDEN);
                mRightEntry.setState(PaneFragment.STATE_INACTIVE);

                mDragHelper.smoothSlideViewTo(mLeftEntry.mLayout, -mLeftWidth, 0);
            }
            else {
                mRightEntry.setState(PaneFragment.STATE_HIDDEN);

                mDragHelper.smoothSlideViewTo(mRightEntry.mLayout, -mPreviousWidth, 0);
            }

            invalidate();
        }
    };

    public TwoPaneLayout(Context context) {
        super(context);
    }

    public TwoPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoPaneLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mLeftWidth == 0 || mRightWidth == 0) {

            if (!mSinglePane) {
                mPreviousWidth = mLeftWidth = (int) ((float) getMeasuredWidth() * LEFT_WIDTH_RATIO);
                mRightWidth = getMeasuredWidth() - mLeftWidth;
            }
            else {
                mLeftWidth = 0;
                mPreviousWidth = mRightWidth = getMeasuredWidth();
            }

            int previousOffset = -mPreviousWidth;

            if (mPreviousEntry != null) {
                mPreviousEntry.setOffset(previousOffset);
                mPreviousEntry.setWidth(mPreviousWidth);
            }

            if (mLeftEntry != null) {
                mLeftEntry.setOffset(0);
                mLeftEntry.setWidth(mLeftWidth);
            }

            if (mRightEntry != null) {
                mRightEntry.setOffset(mLeftWidth);
                mRightEntry.setWidth(mRightWidth);
            }

            for (Entry entry : mPreviousEntries) {
                entry.setOffset(previousOffset);
                entry.setWidth(mPreviousWidth);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mCurrentState != STATE_IDLE)
            return true;

        return mDragHelper.shouldInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mCurrentState != STATE_IDLE)
            return true;

        mDragHelper.processTouchEvent(event);

        invalidate();

        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
    }

    public boolean isSinglePane() {
        return mSinglePane;
    }

    public void setSinglePane(boolean singlePane) {
        mSinglePane = singlePane;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    public void initialize(FragmentManager fragmentManager) {
        mHandler = new Handler();
        mDragHelper = ViewDragHelper.create(this, SENSITIVITY, new DragHelperCallback());
        mFragmentManager = fragmentManager;

        mCurrentState = STATE_IDLE;

        mPreviousEntries = new LinkedList<>();
    }

    public void pushFragment(PaneFragment fragment) {
        if (!mSinglePane && mLeftEntry == null)
            setLeftFragment(fragment);
        else if (mRightEntry == null)
            setRightFragment(fragment);
        else
            requestNext(fragment);
    }

    public void setLeftFragment(PaneFragment fragment) {
        if (mSinglePane)
            throw new IllegalStateException("Setting left fragment in single pane mode is not allowed.");

        if (mLeftEntry == null) {
            mLeftEntry = makeEntry(fragment, PaneFragment.STATE_HIDDEN);
            mLeftEntry.setOffset(0);
            mLeftEntry.setWidth(mLeftWidth);
        }
        else
            mLeftEntry.setFragment(fragment);

        mLeftEntry.setState(PaneFragment.STATE_INACTIVE);
    }

    public void setRightFragment(PaneFragment fragment) {
        if (mRightEntry == null) {
            mRightEntry = makeEntry(fragment, PaneFragment.STATE_HIDDEN);
            mRightEntry.setOffset(mLeftWidth);
            mRightEntry.setWidth(mRightWidth);
        }
        else
            mRightEntry.setFragment(fragment);

        if (mCallback != null)
            mCallback.onActiveFragmentChanged(fragment);

        mRightEntry.setState(PaneFragment.STATE_ACTIVE);
    }

    public PaneFragment getLeftFragment() {
        if (mLeftEntry == null)
            return null;

        return mLeftEntry.mFragment;
    }

    public PaneFragment getRightFragment() {
        if (mRightEntry == null)
            return null;

        return mRightEntry.mFragment;
    }

    public PaneFragment getActiveFragment() {
        switch (mCurrentState) {
            case STATE_ADDING:
                return mNextEntry.getFragment();
            case STATE_BUSY:
            case STATE_IDLE:
                return mRightEntry.getFragment();
            case STATE_REMOVING:
                return mPreviousEntry.getFragment();
        }

        return null;
    }

    public PaneFragment findNextFragment(PaneFragment fragment) {
        Entry entry = findNextEntry(fragment);
        return entry != null ? entry.mFragment : null;
    }

    public Entry findNextEntry(PaneFragment fragment) {
        if (fragment == null || getRightFragment() == fragment)
            return null;

        if (getLeftFragment() == fragment)
            return mRightEntry;

        if (mPreviousEntry != null && mPreviousEntry.mFragment == fragment)
            return mLeftEntry;

        Iterator<Entry> iterator = mPreviousEntries.descendingIterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();

            if (entry.mFragment == fragment)
                return iterator.hasNext() ? iterator.next() : mPreviousEntry;
        }

        return null;
    }

    public void goToStart() {
        if (!backAvailable())
            return;

        while (mPreviousEntries.size() > 1)
            popHierarchy();

        back();
    }

    public boolean goBack() {
        boolean backAvailable = backAvailable();

        if (backAvailable && isIdle())
            back();

        return backAvailable;
    }

    public void requestBack() {
        if (backAvailable()) {
            if (isIdle())
                goBack();
            else
                mNumBackRequests++;
        }
    }

    public boolean backAvailable() {
        return mNextEntry != null || mPreviousEntry != null &&
                !(mCurrentState == STATE_REMOVING && mPreviousEntries.size() - mNumBackRequests == 0);
    }

    public Parcelable saveState() {
        if (mCurrentState == STATE_ADDING)
            pushHierarchy();

        SavedState savedState = new SavedState();

        savedState.mPreviousId = (mPreviousEntry != null ? mPreviousEntry.mLayout.getId() : 0);
        savedState.mLeftId = (mLeftEntry != null ? mLeftEntry.mLayout.getId() : 0);
        savedState.mRightId = (mRightEntry != null ? mRightEntry.mLayout.getId() : 0);

        savedState.mPreviousIds = new int[mPreviousEntries.size()];

        int index = 0;
        for (Entry entry : mPreviousEntries) {
            savedState.mPreviousIds[index] = entry.mLayout.getId();
            index++;
        }

        return savedState;
    }

    public void restoreState(Parcelable state) {
        SavedState savedState = (SavedState)state;

        if (savedState.mPreviousId != 0) {
            mPreviousEntry = restoreEntry(savedState.mPreviousId);
            mPreviousEntry.mLayout.setVisibility(GONE);
            mPreviousEntry.setState(PaneFragment.STATE_HIDDEN);
        }
        if (savedState.mLeftId != 0) {
            mLeftEntry = restoreEntry(savedState.mLeftId);
            mLeftEntry.setOffset(0);
            mLeftEntry.setWidth(mLeftWidth);
            mLeftEntry.setState(PaneFragment.STATE_INACTIVE);
        }
        if (savedState.mRightId != 0) {
            mRightEntry = restoreEntry(savedState.mRightId);
            mRightEntry.setOffset(mLeftWidth);
            mRightEntry.setWidth(mRightWidth);
            mRightEntry.setState(PaneFragment.STATE_ACTIVE);

            if (mCallback != null)
                mCallback.onActiveFragmentChanged(mRightEntry.mFragment);
        }

        for (int id : savedState.mPreviousIds) {
            Entry entry = restoreEntry(id);
            entry.mLayout.setVisibility(GONE);
            entry.setState(PaneFragment.STATE_HIDDEN);

            mPreviousEntries.push(entry);
        }
    }


    private boolean isIdle() {
        return mCurrentState == STATE_IDLE;
    }

    private Entry makeEntry(PaneFragment fragment, int state) {
        int viewId = 100;
        if (mPreviousEntry != null)
            viewId++;
        if (mLeftEntry != null)
            viewId++;
        if (mRightEntry != null)
            viewId++;

        viewId += mPreviousEntries.size();

        Entry entry = new Entry(viewId);

        entry.setFragment(fragment);
        entry.setState(state);

        return entry;
    }

    private Entry restoreEntry(int id) {
        Entry entry = new Entry(id);

        entry.restoreFragment();

        return entry;
    }

    private void onIdle() {
        switch (mCurrentState) {
            case STATE_ADDING:
                pushHierarchy();
                break;
            case STATE_REMOVING:
                popHierarchy();
                break;
        }

        if (mPreviousEntry != null)
            mPreviousEntry.hide();

        if (mLeftEntry != null) {
            mLeftEntry.setOffset(0);
            mLeftEntry.setWidth(mLeftWidth);
        }

        if (mRightEntry != null) {
            mRightEntry.setOffset(mLeftWidth);
            mRightEntry.setWidth(mRightWidth);
        }

        mCurrentState = STATE_IDLE;

        if (mNumBackRequests > 0) {
            mNumBackRequests--;
            back();
        }
    }

    private void pushHierarchy() {
        if (mPreviousEntry != null)
            mPreviousEntries.addFirst(mPreviousEntry);

        if (!mSinglePane) {
            mPreviousEntry = mLeftEntry;
            mLeftEntry = mRightEntry;
        }
        else {
            mPreviousEntry = mRightEntry;
        }

        mRightEntry = mNextEntry;
        mNextEntry = null;
    }

    private void popHierarchy() {
        if (mRightEntry != null)
            mRightEntry.release();

        mNextEntry = null;

        if (!mSinglePane) {
            mRightEntry = mLeftEntry;
            mLeftEntry = mPreviousEntry;
        }
        else {
            mRightEntry = mPreviousEntry;
        }

        mPreviousEntry = mPreviousEntries.isEmpty() ? null : mPreviousEntries.removeFirst();
    }

    private void requestNext(PaneFragment fragment) {
        if (isIdle()) {
            mNextEntry = makeEntry(fragment, PaneFragment.STATE_HIDDEN);
            mNextEntry.setOffset(mLeftWidth + mRightWidth);
            mNextEntry.setWidth(mRightWidth);

            mCurrentState = STATE_ADDING;

            mHandler.postDelayed(mNextTransitionRunnable, TRANSITION_DELAY);
        }
    }

    private void back() {
        mCurrentState = STATE_REMOVING;

        mRightEntry.setState(PaneFragment.STATE_HIDDEN);

        if (!mSinglePane) {
            mPreviousEntry.setState(PaneFragment.STATE_INACTIVE);
            mLeftEntry.setState(PaneFragment.STATE_ACTIVE);

            if (mCallback != null)
                mCallback.onActiveFragmentChanged(mLeftEntry.mFragment);
            mDragHelper.smoothSlideViewTo(mLeftEntry.mLayout, mLeftWidth, 0);
        }
        else {
            mPreviousEntry.setState(PaneFragment.STATE_ACTIVE);

            if (mCallback != null)
                mCallback.onActiveFragmentChanged(mPreviousEntry.mFragment);
            mDragHelper.smoothSlideViewTo(mRightEntry.mLayout, mRightWidth, 0);
        }

        invalidate();
    }

    private void settle() {
        mPreviousEntry.setState(PaneFragment.STATE_HIDDEN);
        mLeftEntry.setState(PaneFragment.STATE_INACTIVE);
        mRightEntry.setState(PaneFragment.STATE_ACTIVE);

        mCurrentState = STATE_BUSY;

        if (!mSinglePane)
            mDragHelper.smoothSlideViewTo(mLeftEntry.mLayout, 0, 0);
        else
            mDragHelper.smoothSlideViewTo(mRightEntry.mLayout, 0, 0);

        invalidate();
    }

    private LayoutParams makeLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
