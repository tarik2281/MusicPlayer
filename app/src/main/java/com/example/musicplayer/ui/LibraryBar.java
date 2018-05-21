package com.example.musicplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.R;

/**
 * Created by Tarik on 01.06.2016.
 */
public class LibraryBar extends LinearLayout {

    public interface Callback {
        void onBarItemClick(Item item);
        boolean onBarItemLongClick(Item item);
    }

    public class Item implements View.OnClickListener, View.OnLongClickListener {
        private int mId;
        private int mPosition;
        private CharSequence mTitle;
        private View mItemView;
        private TextView mTitleView;
        private ImageView mIconView;

        private Callback mCallback;

        private Item(View v, int position) {
            mPosition = position;

            mItemView = v;
            mItemView.setOnClickListener(this);
            mItemView.setOnLongClickListener(this);
            mTitleView = (TextView)v.findViewById(R.id.title);
            mIconView = (ImageView)v.findViewById(R.id.icon);

            mCallback = null;
        }

        public int getPosition() {
            return mPosition;
        }

        public int getId() {
            return mId;
        }

        public void setId(int id) {
            mId = id;
            mItemView.setId(id);
        }

        public void setTitle(CharSequence title) {
            mTitle = title;

            if (mTitleView != null)
                mTitleView.setText(title);
        }

        public void setTitle(@StringRes int titleRes) {
            setTitle(getResources().getString(titleRes));
        }

        public void setTitleVisible(boolean visible) {
            if (mTitleView != null)
                mTitleView.setVisibility(visible ? VISIBLE : GONE);
        }

        public void setIcon(Drawable drawable) {
            if (mIconView != null)
                mIconView.setImageDrawable(drawable);
        }

        public void setIcon(@DrawableRes int drawableRes) {
            if (mIconView != null)
                mIconView.setImageResource(drawableRes);
        }

        public void setIconColor(int color) {
            if (mIconView != null)
                mIconView.setColorFilter(color);
        }

        public void setCallback(Callback cb) {
            mCallback = cb;
        }

        public void requestHighlight() {
            setHighlightedItem(mPosition);
        }

        @Override
        public void onClick(View view) {
            if (mCallback != null)
                mCallback.onBarItemClick(this);
            else if (LibraryBar.this.mCallback != null)
                LibraryBar.this.mCallback.onBarItemClick(this);
        }

        @Override
        public boolean onLongClick(View view) {
            if (mCallback != null) {
                if (!mCallback.onBarItemLongClick(this))
                    showTitleToast(this);
                return true;
            }
            else if (LibraryBar.this.mCallback != null) {
                if (!LibraryBar.this.mCallback.onBarItemLongClick(this))
                    showTitleToast(this);
                return true;
            }

            return false;
        }
    }

    private enum HighlightStyle {
        Color, Background
    }

    private Menu mMenu;
    private Item[] mItems;
    private Callback mCallback;

    private int mItemLayout;
    private boolean mTitlesVisible;
    private HighlightStyle mStyle;
    private int mItemIconColor;
    private int mItemColor;
    private int mHighlightColor;
    private int mItemBackground;
    private int mHighlightBackground;
    private boolean mDimItemBackground;

    private Toast mTitleToast;

    private int mHighlightedPosition = -1;

    public LibraryBar(Context context) {
        super(context);
    }

    public LibraryBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LibraryBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTitlesVisible = true;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LibraryBar);

        mItemLayout = a.getResourceId(R.styleable.LibraryBar_itemLayout, 0);
        mStyle = HighlightStyle.values()[a.getInteger(R.styleable.LibraryBar_itemStyleType, 0)];

        mItemIconColor = a.getColor(R.styleable.LibraryBar_itemIconColor, 0);

        switch (mStyle) {
            case Color:
                mItemColor = a.getColor(R.styleable.LibraryBar_itemStyleNormal, 0);
                mHighlightColor = a.getColor(R.styleable.LibraryBar_itemStyleHighlighted, 0);
                break;
            case Background:
                mItemBackground = a.getResourceId(R.styleable.LibraryBar_itemStyleNormal, 0);
                mHighlightBackground = a.getResourceId(R.styleable.LibraryBar_itemStyleHighlighted, 0);
                break;
        }

        a.recycle();
    }

    public void setDimItemBackground(boolean dim) {
        mDimItemBackground = dim;

        if (mItems != null) {
            for (int i = 0; i < mItems.length; i++) {
                if (mItems[i] != null)
                    applyItemStyle(mItems[i], i == mHighlightedPosition);
            }
        }
    }

    public void setMenu(Menu menu) {
        mMenu = menu;

        mHighlightedPosition = -1;
        removeAllViews();

        if (mMenu != null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());

            mItems = new Item[mMenu.size()];

            for (int i = 0; i < mMenu.size(); i++) {
                MenuItem item = mMenu.getItem(i);

                View v = inflater.inflate(mItemLayout, this, false);

                Item barItem = new Item(v, i);
                barItem.setId(item.getItemId());
                barItem.setTitle(item.getTitle());
                barItem.setIcon(item.getIcon());
                barItem.setTitleVisible(mTitlesVisible);

                if (mStyle != HighlightStyle.Color)
                    barItem.setIconColor(mItemIconColor);

                addView(v);

                applyItemStyle(barItem, false);

                mItems[i] = barItem;
            }
        }

        requestLayout();
    }

    public void setItemColor(int color, int colorHighlight) {
        mStyle = HighlightStyle.Color;
        mItemColor = color;
        mHighlightColor = colorHighlight;
    }

    public void setItemBackground(@DrawableRes int drawable, @DrawableRes int drawableHighlight) {
        mStyle = HighlightStyle.Background;
        mItemBackground = drawable;
        mHighlightBackground = drawableHighlight;
    }

    public Item getItem(int position) {
        if (mItems == null || position >= mItems.length)
            return null;

        return mItems[position];
    }

    public Item getItemById(int id) {
        for (Item item : mItems)
            if (item.mId == id)
                return item;

        return null;
    }

    public void setTitlesVisible(boolean visible) {
        mTitlesVisible = visible;

        if (mItems != null) {
            for (Item item : mItems)
                item.setTitleVisible(visible);
        }
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    private void showTitleToast(Item item) {
        if (mTitleToast == null)
            mTitleToast = Toast.makeText(getContext(), item.mTitle, Toast.LENGTH_SHORT);
        else {
            mTitleToast.setText(item.mTitle);
        }

        mTitleToast.setGravity(Gravity.TOP, 0, mTitleToast.getYOffset());

        mTitleToast.show();
    }

    public void setHighlightedItem(int position) {
        if (position == mHighlightedPosition)
            return;

        if (mHighlightedPosition != -1) {
            Item item = getItem(mHighlightedPosition);
            applyItemStyle(item, false);
        }

        if (mItems == null)
            return;

        mHighlightedPosition = position;

        if (mHighlightedPosition != -1) {
            Item item = getItem(mHighlightedPosition);
            applyItemStyle(item, true);
        }
    }

    private void applyItemStyle(Item item, boolean highlighted) {
        if (item == null)
            return;

        switch (mStyle) {
            case Color: {
                int color = highlighted ? mHighlightColor : mItemColor;

                if (item.mIconView != null)
                    item.mIconView.setColorFilter(color);
                if (item.mTitleView != null)
                    item.mTitleView.setTextColor(color);

                break;
            }
            case Background: {
                int drawable = highlighted ? mHighlightBackground : mItemBackground;

                item.mItemView.setBackgroundResource(drawable);
                if (mDimItemBackground)
                    item.mItemView.getBackground().setAlpha(127);
                else
                    item.mItemView.getBackground().setAlpha(255);

                break;
            }
        }
    }
}
