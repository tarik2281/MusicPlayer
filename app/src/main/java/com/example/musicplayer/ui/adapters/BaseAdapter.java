package com.example.musicplayer.ui.adapters;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;

/**
 * Created by 19tarik97 on 04.01.17.
 */

public abstract class BaseAdapter<VH extends BaseAdapter.ViewHolder> extends RecyclerView.Adapter<VH> {

    public abstract class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public abstract View getItemView();

        protected void initialize() {
            View itemView = getItemView();
            if (itemView != null) {
                itemView.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            if (v == getItemView() && mClickListener != null)
                mClickListener.onItemClick(v, getAdapterPosition(), getId(getAdapterPosition()));
        }
    }

    private OnItemClickListener mClickListener;
    private int mHighlightedPosition = -1;

    private int mBackgroundResource = 0;
    private int mHighlightResource = 0;

    private int mItemColor = -1;
    private int mHighlightColor = -1;

    @Override
    public void onBindViewHolder(VH holder, int position) {
        View itemView = holder.getItemView();

        boolean highlight = position == mHighlightedPosition;

        if (itemView != null) {
            if (mBackgroundResource == 0) {
                mBackgroundResource = Util.getAttrResource(itemView.getContext(), R.attr.itemBackgroundStyle);
            }
            if (mHighlightResource == 0) {
                mHighlightResource = Util.getAttrResource(itemView.getContext(), R.attr.itemBackgroundHighlightStyle);
            }

            itemView.setBackgroundResource((highlight ? mHighlightResource : mBackgroundResource));
        }
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mClickListener = l;
    }

    public void setBackgroundResource(int resId) {
        mBackgroundResource = resId;
    }

    public int getHighlightedItem() {
        return mHighlightedPosition;
    }

    public void setHighlightedItem(int position) {
        if (mHighlightedPosition > -1)
            notifyItemChanged(mHighlightedPosition);

        mHighlightedPosition = position;

        if (mHighlightedPosition > -1)
            notifyItemChanged(mHighlightedPosition);
    }

    public abstract int getId(int position);

    protected int getItemColor(VH holder) {
        if (holder.getItemView() == null)
            return 0;

        boolean highlight = holder.getAdapterPosition() == mHighlightedPosition;
        if (highlight) {
            if (mHighlightColor == -1)
                mHighlightColor = Util.getAttrColor(holder.getItemView().getContext(), android.R.attr.textColorPrimaryInverse);

            return mHighlightColor;
        }
        else {
            if (mItemColor == -1)
                mItemColor = Util.getAttrColor(holder.getItemView().getContext(), android.R.attr.textColorPrimary);

            return mItemColor;
        }
    }

    protected ColorFilter getColorFilter(VH holder) {
        return new PorterDuffColorFilter(getItemColor(holder), PorterDuff.Mode.SRC_ATOP);
    }
}
