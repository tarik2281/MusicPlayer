package com.example.musicplayer.ui.adapters;

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.SideBar;

import java.util.ArrayList;

/**
 * Created by 19tarik97 on 20.09.16.
 */
public class LayoutEditorAdapter extends RecyclerView.Adapter<LayoutEditorAdapter.ViewHolder> {

    public interface OnDragHandleTouchListener {
        void onDragHandleTouch(RecyclerView.ViewHolder holder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnTouchListener {

        private View mView;
        private ImageButton mHomeButton;
        private ImageView mIconView;
        private TextView mTextView;
        private ImageView mDragHandle;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mHomeButton = (ImageButton)mView.findViewById(R.id.button_home);
            mIconView = (ImageView)mView.findViewById(R.id.icon);
            mTextView = (TextView)mView.findViewById(R.id.text_title);
            mDragHandle = (ImageView)mView.findViewById(R.id.drag_handle);

            mView.setOnClickListener(this);
            mDragHandle.setOnTouchListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mClickListener != null)
                mClickListener.onItemClick(v, getAdapterPosition(), mItems.get(getAdapterPosition()));
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (mTouchListener != null)
                        mTouchListener.onDragHandleTouch(this);

                    return true;
            }

            return false;
        }
    }

    private ArrayList<Integer> mItems;
    private OnItemClickListener mClickListener;
    private OnDragHandleTouchListener mTouchListener;

    private int mStartingItem = -1;
    private int mStartingPosition = -1;

    public LayoutEditorAdapter(ArrayList<Integer> items) {
        mItems = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.entry_layout_editor, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int item = getItem(position);

        holder.mHomeButton.setAlpha((mStartingItem == item ? 1.0f : 0.25f));
        holder.mIconView.setImageResource(SideBar.getDrawableForItem(item));
        holder.mTextView.setText(SideBar.getTitleForItem(item));
    }

    public int getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mClickListener = listener;
    }

    public void setOnTouchListener(OnDragHandleTouchListener listener) {
        mTouchListener = listener;
    }

    public void setStartingItem(int item) {
        if (mStartingItem == item)
            return;

        mStartingItem = item;

        if (mStartingPosition != -1)
            notifyItemChanged(mStartingPosition);

        mStartingPosition = -1;
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i) == item) {
                mStartingPosition = i;
                notifyItemChanged(mStartingPosition);
                break;
            }
        }
    }

    public int getStartingItem() {
        return mStartingItem;
    }

    public void addItem(int item) {
        mItems.add(item);
        notifyItemInserted(mItems.size() - 1);
    }

    public void moveItem(int oldPosition, int newPosition) {
        int item = mItems.remove(oldPosition);
        mItems.add(newPosition, item);
        notifyItemMoved(oldPosition, newPosition);

        for (int i = 0; i < mItems.size(); i++) {
            if (mStartingItem == mItems.get(i)) {
                mStartingPosition = i;
                break;
            }
        }
    }

    public void removeItem(int position) {
        mItems.remove(position);
        notifyItemRemoved(position);

        if (mStartingPosition == position) {
            mStartingPosition = 0;
            mStartingItem = mItems.get(0);
            notifyItemChanged(0);
        }
        else {
            for (int i = 0; i < mItems.size(); i++) {
                if (mStartingItem == mItems.get(i)) {
                    mStartingPosition = i;
                    break;
                }
            }
        }
    }
}
