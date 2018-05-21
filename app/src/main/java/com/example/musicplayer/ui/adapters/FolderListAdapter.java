package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Folder;

import java.util.Collection;

/**
 * Created by Tarik on 06.08.2016.
 */
public class FolderListAdapter extends OptionsAdapter<Folder, FolderListAdapter.ViewHolder> {

    public class ViewHolder extends OptionsAdapter.OptionsHolder {

        private View mView;
        private TextView mNameView;
        private TextView mPathView;
        private ImageButton mOptionsView;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mNameView = (TextView)mView.findViewById(R.id.text_name);
            mPathView = (TextView)mView.findViewById(R.id.text_path);
            mOptionsView = (ImageButton)mView.findViewById(R.id.options_button);

            if (!mShowParents)
                mPathView.setVisibility(View.GONE);

            initialize();
        }

        @Override
        public View getItemView() {
            return mView;
        }

        @Override
        public View getOptionsView() {
            return mOptionsView;
        }
    }

    private boolean mShowParents;

    public FolderListAdapter() {
        super();
    }

    public FolderListAdapter(Collection<Folder> items) {
        super(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_folder, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Folder folder = getItem(position);

        holder.mNameView.setText(folder.getName());
        holder.mPathView.setText(folder.getPath());

        int itemColor = getItemColor(holder);
        holder.mNameView.setTextColor(itemColor);
        holder.mPathView.setTextColor(itemColor);

        super.onBindViewHolder(holder, position);
    }

    public void setShowParents(boolean show) {
        mShowParents = show;
    }
}
