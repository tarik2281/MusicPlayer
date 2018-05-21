package com.example.musicplayer.ui.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;

/**
 * Created by Tarik on 21.10.2017.
 */

class IntroChooserDelegate implements View.OnClickListener {

    static final String KEY_SELECTION = "selection";

    static final int LEFT = 0;
    static final int RIGHT = 1;

    private Context mContext;

    private View mMainView;
    private TextView mWelcomeView;
    private TextView mInstructionTextView;

    private ImageView mLeftImage;
    private TextView mLeftText;
    private ImageView mRightImage;
    private TextView mRightText;

    private int mSelection;
    private int mHighlightPadding;

    IntroChooserDelegate(View mainView, Context context, Bundle savedInstanceState) {
        mContext = context;

        mMainView = mainView;
        mWelcomeView = (TextView)mMainView.findViewById(R.id.text_welcome);
        mInstructionTextView = (TextView)mMainView.findViewById(R.id.text_instruction);
        mLeftImage = (ImageView)mMainView.findViewById(R.id.image_left);
        mLeftText = (TextView)mMainView.findViewById(R.id.text_left);
        mRightImage = (ImageView)mMainView.findViewById(R.id.image_right);
        mRightText = (TextView)mMainView.findViewById(R.id.text_right);

        mLeftImage.setOnClickListener(this);
        mRightImage.setOnClickListener(this);

        int selection;

        if (savedInstanceState != null)
            selection = savedInstanceState.getInt(KEY_SELECTION);
        else
            selection = LEFT;

        select(selection);
    }

    void saveState(Bundle savedInstanceState) {
        savedInstanceState.putInt(KEY_SELECTION, mSelection);
    }

    void setWelcomeVisible(boolean visible) {
        mWelcomeView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    void setInstructionText(int textRes) {
        mInstructionTextView.setText(textRes);
    }

    void setLeftDrawable(int drawable) {
        mLeftImage.setImageResource(drawable);
    }

    void setLeftText(int textRes) {
        mLeftText.setText(textRes);
    }

    void setRightDrawable(int drawable) {
        mRightImage.setImageResource(drawable);
    }

    void setRightText(int textRes) {
        mRightText.setText(textRes);
    }

    void select(int item) {
        ImageView oldView = getImageView(mSelection);
        ImageView selectingView = getImageView(item);

        mSelection = item;

        if (oldView != null)
            unhighlightView(oldView);

        if (selectingView != null)
            highlightView(selectingView);
    }

    int getSelection() {
        return mSelection;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_left:
                if (mSelection != LEFT)
                    select(LEFT);
                break;
            case R.id.image_right:
                if (mSelection != RIGHT)
                    select(RIGHT);
                break;
        }
    }

    private void highlightView(ImageView view) {
        if (mHighlightPadding == 0)
            mHighlightPadding = mContext.getResources().getDimensionPixelSize(R.dimen.intro_chooser_padding);

        view.setPadding(mHighlightPadding, mHighlightPadding, mHighlightPadding, mHighlightPadding);
        Drawable border = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.border, null);
        ViewCompat.setBackground(view, border);
    }

    private void unhighlightView(ImageView view) {
        view.setPadding(0, 0, 0, 0);
        ViewCompat.setBackground(view, null);
    }

    private ImageView getImageView(int item) {
        switch (item) {
            case LEFT:
                return mLeftImage;
            case RIGHT:
                return mRightImage;
        }

        return null;
    }
}
