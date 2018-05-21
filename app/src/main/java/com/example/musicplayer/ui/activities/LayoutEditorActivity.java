package com.example.musicplayer.ui.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.SideBar;
import com.example.musicplayer.ui.adapters.LayoutEditorAdapter;
import com.example.musicplayer.ui.dialogs.BaseDialogFragment;
import com.example.musicplayer.ui.dialogs.OnDialogDismissListener;
import com.example.musicplayer.ui.fragments.MainFragment;

import java.util.ArrayList;

/**
 * Created by 19tarik97 on 20.09.16.
 */
public class LayoutEditorActivity extends BaseActivity implements OnItemClickListener, LayoutEditorAdapter.OnDragHandleTouchListener, OnDialogDismissListener {

    private RecyclerView mRecyclerView;
    private LayoutEditorAdapter mAdapter;
    private ItemTouchHelper mTouchHelper;
    private ArrayList<Integer> mEntryList;
    private MenuItem mAddButton;

    private ItemTouchHelper.Callback mTouchCallback = new ItemTouchHelper.Callback() {
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            mAdapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mAdapter.removeItem(viewHolder.getAdapterPosition());
            updateAddButton();
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return mEntryList.size() > 1;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_layout_editor);

        String layoutString = PreferenceManager.getInstance().getString(PreferenceManager.KEY_START_PAGE_LAYOUT);
        int startingItem = PreferenceManager.getInstance().getInt(PreferenceManager.KEY_START_PAGE_LAUNCH);
        String[] layoutNumbers = layoutString.split(";");

        mEntryList = new ArrayList<>(layoutNumbers.length);
        for (String item : layoutNumbers)
            mEntryList.add(Integer.parseInt(item));

        mRecyclerView = (RecyclerView)findViewById(R.id.list);

        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new LayoutEditorAdapter(mEntryList);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setStartingItem(startingItem);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnTouchListener(this);

        mTouchHelper = new ItemTouchHelper(mTouchCallback);
        mTouchHelper.attachToRecyclerView(mRecyclerView);

        showBackButton(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            StringBuilder builder = new StringBuilder();
            for (Integer item : mEntryList) {
                builder.append(item);
                builder.append(';');
            }
            builder.deleteCharAt(builder.length() - 1);

            PreferenceManager prefs = PreferenceManager.getInstance();
            prefs.setString(PreferenceManager.KEY_START_PAGE_LAYOUT, builder.toString());
            prefs.setInt(PreferenceManager.KEY_START_PAGE_LAUNCH, mAdapter.getStartingItem());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_layout_editor, menu);

        mAddButton = menu.findItem(R.id.option_add);
        updateAddButton();

        return true;
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        mAdapter.setStartingItem(id);
    }

    @Override
    public void onDragHandleTouch(RecyclerView.ViewHolder holder) {
        if (mTouchHelper != null)
            mTouchHelper.startDrag(holder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_add: {
                handleAddButton();
                return true;
            }
            case R.id.option_reset: {
                resetItems();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleAddButton() {
        int[] items = new int[missingItemsCount()];

        int currentIndex = 0;
        for (int i = 0; i < 6; i++) {
            if (!mEntryList.contains(i))
                items[currentIndex++] = i;
        }

        AddItemDialog.newInstance(items).show(getSupportFragmentManager(), "add_item_dialog");
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof AddItemDialog) {
            int item = ((AddItemDialog)dialog).getItem();
            if (item > -1) {
                mAdapter.addItem(item);
                updateAddButton();
            }
        }
    }

    private void resetItems() {
        mEntryList.clear();
        mEntryList.add(MainFragment.ARTIST_FRAGMENT);
        mEntryList.add(MainFragment.ALBUM_FRAGMENT);
        mEntryList.add(MainFragment.SONG_FRAGMENT);
        mEntryList.add(MainFragment.GENRE_FRAGMENT);
        mEntryList.add(MainFragment.FOLDER_FRAGMENT);
        mEntryList.add(MainFragment.PLAYLIST_FRAGMENT);
        mAdapter.setStartingItem(MainFragment.ARTIST_FRAGMENT);
        mAdapter.notifyDataSetChanged();
    }

    private int missingItemsCount() {
        return MainFragment.PAGES_COUNT - mEntryList.size();
    }

    private void updateAddButton() {
        boolean visible = missingItemsCount() != 0;
        mAddButton.setEnabled(visible).setVisible(visible);
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, LayoutEditorActivity.class);
        context.startActivity(starter);
    }


    public static class AddItemDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {
        private static final String KEY_ITEMS = "items";

        private int[] mItems;
        private int mChosenItem;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mChosenItem = -1;

            Bundle args = getArguments();
            if (args != null)
                mItems = args.getIntArray(KEY_ITEMS);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            String[] items = new String[mItems.length];
            for (int i = 0; i < items.length; i++) {
                items[i] = getString(SideBar.getTitleForItem(mItems[i]));
            }

            builder.setTitle(R.string.dialog_title_add_item);
            builder.setItems(items, this);

            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mChosenItem = mItems[which];
        }

        public int getItem() {
            return mChosenItem;
        }

        public static AddItemDialog newInstance(int[] items) {
            AddItemDialog dialog = new AddItemDialog();

            Bundle args = new Bundle();
            args.putIntArray(KEY_ITEMS, items);
            dialog.setArguments(args);

            return dialog;
        }
    }
}
