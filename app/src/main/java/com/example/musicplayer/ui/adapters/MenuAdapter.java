package com.example.musicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;

/**
 * Created by 19tarik97 on 27.08.16.
 */
public class MenuAdapter extends BaseAdapter<MenuAdapter.ViewHolder> {

    public class ViewHolder extends BaseAdapter.ViewHolder {

        private View mView;
        private ImageView mIconView;
        private TextView mTitleView;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mIconView = (ImageView)mView.findViewById(R.id.icon);
            mTitleView = (TextView)mView.findViewById(R.id.text_title);

            initialize();
        }

        @Override
        public View getItemView() {
            return mView;
        }
    }

    private Menu mMenu;

    public MenuAdapter(Menu menu) {
        mMenu = menu;
    }

    public void setMenu(Menu menu) {
        mMenu = menu;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.entry_menu, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MenuItem item = getItem(position);

        holder.mIconView.setImageDrawable(item.getIcon());
        holder.mTitleView.setText(item.getTitle());

        holder.mIconView.setColorFilter(getColorFilter(holder));
        holder.mTitleView.setTextColor(getItemColor(holder));

        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mMenu.size();
    }

    public MenuItem getItem(int position) {
        return mMenu.getItem(position);
    }

    @Override
    public int getId(int position) {
        return getItem(position).getItemId();
    }
}
