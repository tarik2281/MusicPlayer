package com.example.musicplayer.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;

import java.io.File;

/**
 * Created by Tarik on 15.08.2016.
 */
public class FileListAdapter extends ItemAdapter<File> {

    private boolean mShowUpItem;

    private Resources mResources;

    public FileListAdapter(Context context, File[] items) {
        super(context, items);

        mShowUpItem = false;
        mResources = context.getResources();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null)
            v = getLayoutInflater().inflate(R.layout.entry_file, parent, false);

        ImageView iconView = (ImageView)v.findViewById(R.id.icon);
        TextView nameView = (TextView)v.findViewById(R.id.text_name);

        if (mShowUpItem && position == 0) {
            iconView.setImageResource(R.drawable.ic_arrow_upward_black_36dp);
            nameView.setText(mResources.getString(R.string.file_picker_go_up));
        }
        else {
            File file = getItem(position);

            iconView.setImageResource(file.isDirectory() ? R.drawable.ic_folder_black_36dp : R.drawable.ic_insert_drive_file_black_36dp);
            nameView.setText(file.getName());
        }

        return v;
    }

    public void setShowUpItem(boolean show) {
        mShowUpItem = show;
    }

    @Override
    public int getCount() {
        int itemCount = super.getCount();

        return mShowUpItem ? itemCount + 1 : itemCount;
    }

    @Override
    public File getItem(int position) {
        if (mShowUpItem)
            position--;

        return super.getItem(position);
    }
}
