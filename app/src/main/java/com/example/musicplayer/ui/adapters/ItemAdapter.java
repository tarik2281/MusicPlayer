package com.example.musicplayer.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.musicplayer.R;

import java.util.List;

/**
 * Created by 19tarik97 on 21.03.16.
 */
public class ItemAdapter<T> extends BaseAdapter {

    private Context mContext;
    private T[] mItems;
    private List<T> mItemList;

    public ItemAdapter(Context context, List<T> items) {
        mContext = context;
        mItemList = items;
    }

    public ItemAdapter(Context context, T[] items) {
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount() {
        if (mItemList != null)
            return mItemList.size();

        return mItems != null ? mItems.length : 0;
    }

    @Override
    public T getItem(int position) {
        if (mItemList != null)
            return mItemList.get(position);

        return mItems != null ? mItems[position] : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = getLayoutInflater();

            v = inflater.inflate(R.layout.entry_item, null);
        }

        TextView text = (TextView)v.findViewById(R.id.text_item);

        T item = getItem(position);

        text.setText(item.toString());

        return v;
    }

    public void setItems(T[] items) {
        mItems = items;
    }

    protected LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(mContext);
    }
}
