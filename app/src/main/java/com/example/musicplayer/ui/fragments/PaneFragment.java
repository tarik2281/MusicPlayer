package com.example.musicplayer.ui.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * Created by Tarik on 27.05.2016.
 */
public class PaneFragment extends Fragment {

    public static final int STATE_HIDDEN = 0;
    public static final int STATE_INACTIVE = 1;
    public static final int STATE_ACTIVE = 2;

    private TwoPaneLayout mParent;

    private int mWidth;
    private int mState = STATE_HIDDEN;
    private boolean mCreated = false;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCreated = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCreated = false;
    }

    public int getType() {
        return 0;
    }

    public String getTitle(Resources resources) {
        return null;
    }

    public int getTitleIcon() {
        return 0;
    }

    public int getState() {
        return mState;
    }

    public int getWidth() {
        return mWidth;
    }

    public TwoPaneLayout getParent() {
        return mParent == null ? getRootPaneFragment().mParent : mParent;
    }

    public PaneFragment getRootPaneFragment() {
        Fragment parentFrag = getParentFragment();
        if (parentFrag != null && parentFrag instanceof PaneFragment)
            return ((PaneFragment)parentFrag).getRootPaneFragment();

        return this;
    }

    public boolean isSinglePane() {
        TwoPaneLayout parent = getParent();

        return parent == null || parent.isSinglePane();
    }

    public void requestNextFragment(PaneFragment fragment) {
        TwoPaneLayout parent = getParent();

        if (parent.getRightFragment() == this)
            parent.pushFragment(fragment);
        else if (parent.getLeftFragment() == this)
            parent.setRightFragment(fragment);
    }

    public void requestBack() {
        TwoPaneLayout parent = getParent();

        if (parent != null)
            parent.requestBack();
    }

    public void setState(int state) {
        if (created() && state != mState)
            onStateChanged(state);

        mState = state;
    }

    public void setWidth(int width) {
        boolean changed = mWidth != width;
        mWidth = width;

        if (changed)
            onResize(width);
    }

    public void setParent(TwoPaneLayout parent) {
        mParent = parent;
    }


    protected boolean created() {
        return mCreated;
    }

    protected void onStateChanged(int state) {

    }

    protected void onResize(int width) {

    }
}