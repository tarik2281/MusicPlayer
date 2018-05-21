package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.Observable;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.adapters.SongListAdapter;
import com.example.musicplayer.ui.dialogs.NumberPickerDialog;
import com.example.musicplayer.ui.fragments.TwoPaneLayout;
import com.example.musicplayer.ui.fragments.options.PlaylistOptionsHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by Tarik on 27.05.2016.
 */
public class PlaylistFragment extends SongListFragment implements View.OnClickListener,
        OptionsAdapter.OnLongClickListener, ActionMode.Callback {

    private class SearchTask extends ItemsTask {
        ArrayList<Song> copy;
        boolean editEnabled;
        Playlist playlist;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            copy = new ArrayList<>(mPlaylist.getSongList());
            editEnabled = mEditModeEnabled;
            playlist = mPlaylist;
        }

        @Override
        protected Collection<Song> doInBackground(Void... params) {
            MusicLibrary lib = MusicLibrary.getInstance();

            if (editEnabled)
                return playlist.getSongList();

            if (Util.stringIsEmpty(filter)) {
                return lib.getSongsForPlaylist(playlist, sorting, reversed);
            }
            else {
                TreeSet<Song> result = lib.getAllSongs(filter, sorting, reversed);

                if (sorting == Sorting.Custom) {
                    TreeMap<Integer, Song> songs = new TreeMap<>();

                    for (Song song : result) {
                        for (int i = 0; i < copy.size(); i++) {
                            if (copy.get(i).getId() == song.getId()) {
                                songs.put(i, song);
                            }
                        }
                    }

                    return songs.values();
                } else {
                    ArrayList<Song> songs = new ArrayList<>(result.size() > copy.size() ? copy.size() : result.size());

                    int index = -1;
                    for (Song song : result) {
                        while ((index = copy.indexOf(song)) != -1) {
                            songs.add(copy.remove(index));
                        }
                    }

                    return songs;
                }
            }
        }
    }

    private class Callback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            mPlaylist.moveSong(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mPlaylist.removeSong(viewHolder.getAdapterPosition());
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return mEditModeEnabled && mPlaylist.isMutable();
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            if (mDragging) {
                mPlaylist.endMove();
                mDragging = false;
            }
        }
    }

    private final Playlist.Observer mPlaylistObserver = new Playlist.Observer() {
        @Override
        public void update(Observable sender, Playlist.ObserverData data) {
            if (data.action != null) {
                mUndoActions.add(data.action);
                if (!mRedoActions.isEmpty())
                    mRedoActions.clear();
            }

            switch (data.type) {
                case Added:
                    mAdapter.notifyItemRangeInserted(data.position, data.size);
                    updateActionState(mActionMode != null ? mActionMode.getMenu() : null);
                    break;
                case Removed:
                    mAdapter.notifyItemRangeRemoved(data.position, data.size);
                    updateActionState(mActionMode != null ? mActionMode.getMenu() : null);
                    break;
                case Moved:
                    mAdapter.notifyItemMoved(data.oldPosition, data.position);
                    updateActionState(mActionMode != null ? mActionMode.getMenu() : null);
                    break;
                case Changed:
                    mAdapter.notifyDataSetChanged();
                    break;
                case Renamed:
                    getDelegate().invalidateTitle();
                    break;
                case Deleted:
                    requestBack();
                    break;
                case Invalidated:
                    mPlaylist.load();
                    if (mAdapter != null)
                        mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    public static final int TYPE = Util.HashFNV1a32("Playlist");

    private static final String TAG_EDIT_DIALOG = "EDIT_DIALOG";

    private static final String KEY_EDIT_ENABLED = "edit_enabled";
    private static final String KEY_UNDO_ACTIONS = "UNDO_ACTIONS";
    private static final String KEY_REDO_ACTIONS = "REDO_ACTIONS";

    private static final Sorting[] SORTINGS;

    private ItemTouchHelper mTouchHelper;
    private Callback mTouchHelperCallback;
    private FloatingActionButton mEditButton;

    private View mActionLayout;
    private View mUndoAction;
    private View mRedoAction;
    private View mAddAction;

    private boolean mEditModeEnabled;

    private boolean mTempFinish;
    private Playlist mPlaylist;
    private Toast mRemoveToast;

    private boolean mDragging;
    private ArrayList<Playlist.Action> mUndoActions;
    private ArrayList<Playlist.Action> mRedoActions;

    private ActionMode mActionMode;

    static {
        SORTINGS = new Sorting[SongListFragment.SORTINGS.length + 1];

        System.arraycopy(SongListFragment.SORTINGS, 0, SORTINGS, 0, SongListFragment.SORTINGS.length);

        SORTINGS[SongListFragment.SORTINGS.length] = Sorting.Custom;
    }

    public PlaylistFragment() {
        super();
        mDragging = false;
    }

    @Override
    public int getTitleIcon() {
        return R.drawable.ic_reorder_black_24dp;
    }

    @Override
    protected Sorting getDefaultSorting() {
        return Sorting.Custom;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUndoActions = savedInstanceState.getParcelableArrayList(KEY_UNDO_ACTIONS);
            mRedoActions = savedInstanceState.getParcelableArrayList(KEY_REDO_ACTIONS);
            mEditModeEnabled = savedInstanceState.getBoolean(KEY_EDIT_ENABLED);
        }
        else {
            mUndoActions = new ArrayList<>();
            mRedoActions = new ArrayList<>();
        }

        setPlaylist(MusicLibrary.getInstance().getPlaylistById(getObjectId()));

        setOptionsHandlerClass(PlaylistOptionsHandler.class);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_playlist;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (mTouchHelperCallback == null)
            mTouchHelperCallback = new Callback();

        mTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        mTouchHelper.attachToRecyclerView(getRecyclerView());

        mEditButton = (FloatingActionButton)v.findViewById(R.id.button_edit);
        mEditButton.setOnClickListener(this);
        mEditButton.setVisibility((editButtonVisible() ? View.VISIBLE : View.INVISIBLE));

        mActionLayout = v.findViewById(R.id.layout_actions);
        mActionLayout.setVisibility(View.GONE);

        mUndoAction = v.findViewById(R.id.action_undo);
        mRedoAction = v.findViewById(R.id.action_redo);
        mAddAction = v.findViewById(R.id.action_add);

        mUndoAction.setOnClickListener(this);
        mRedoAction.setOnClickListener(this);
        mAddAction.setOnClickListener(this);

        mOptionsHandler.setPlaylist(mPlaylist);

        updateActionState(null);

        if (!isSinglePane())
            addButtonSpace(mEditButton);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null && mEditModeEnabled && getState() == STATE_ACTIVE)
            startActionMode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        setPlaylist(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_EDIT_ENABLED, mEditModeEnabled);
        outState.putParcelableArrayList(KEY_UNDO_ACTIONS, mUndoActions);
        outState.putParcelableArrayList(KEY_REDO_ACTIONS, mRedoActions);
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public String getTitle(Resources resources) {
        return mPlaylist.getName();
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        super.initializeAdapter();

        mAdapter.setOnDragHandleTouchListener(new SongListAdapter.OnDragHandleTouchListener() {
            @Override
            public void onDragHandleTouch(RecyclerView.ViewHolder holder) {
                mDragging = true;
                mPlaylist.startMove(holder.getAdapterPosition());

                if (mTouchHelper != null && mPlaylist.isMutable())
                    mTouchHelper.startDrag(holder);
            }
        });

        return mAdapter;
    }


    /*@Override
    protected void initializeAdapter() {
        mAdapter.setOnDragHandleTouchListener(new SongListAdapter.OnDragHandleTouchListener() {
            @Override
            public void onDragHandleTouch(RecyclerView.ViewHolder holder) {
                mDragging = true;
                mPlaylist.startMove(holder.getAdapterPosition());

                if (mTouchHelper != null && mPlaylist.isMutable())
                    mTouchHelper.startDrag(holder);
            }
        });

        super.initializeAdapter();
    }*/

    @Override
    public boolean hasBarMenu() {
        return true;
    }

    @Override
    public void onCreateBarMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.bar_playlist, menu);

        if (!mPlaylist.isMutable())
            menu.removeItem(R.id.item_edit);
    }

    @Override
    public void onBarItemClick(LibraryBar.Item item) {
        switch (item.getId()) {
            case R.id.item_playlist:
                setEditModeEnabled(false);
                break;
            case R.id.item_edit:
                setEditModeEnabled(true);
                break;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.actions_playlist, menu);

        updateActionState(menu);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                showEditorFragment();
                return true;
            }
            case R.id.action_undo: {
                undoAction();

                updateActionState(mode.getMenu());
                return true;
            }
            case R.id.action_redo: {
                redoAction();

                updateActionState(mode.getMenu());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        setEditModeEnabled(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_edit:
                setEditModeEnabled(!mEditModeEnabled);
                //showEditorFragment();
                break;
            case R.id.action_undo:
                undoAction();
                updateActionState(null);
                break;
            case R.id.action_redo:
                redoAction();
                updateActionState(null);
                break;
            case R.id.action_add:
                showEditorFragment();
                break;
        }
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        if (mEditModeEnabled) {
            if (mRemoveToast == null)
                mRemoveToast = Toast.makeText(getActivity(), R.string.toast_playlist_remove_song, Toast.LENGTH_SHORT);

            mRemoveToast.show();
            return;
        }

        super.onItemClick(v, position, id);
    }

    @Override
    public void onItemLongClick(View v, int position, long id) {
        mPlaylist.removeSong(position);
    }

    @Override
    protected void onResize(int width) {
        if (mEditButton != null)
            mEditButton.requestLayout();
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);

        if (!editButtonVisible()) {
            if (mEditModeEnabled && state != STATE_ACTIVE) {
                mTempFinish = true;
                mActionMode.finish();
                mActionMode = null;
            }
            else if (mEditModeEnabled) {
                mActionMode = getDelegate().startActionMode(this);
            }
        }
        else {
            if (state == STATE_ACTIVE) {
                mAddAction.setVisibility(View.VISIBLE);
                mEditButton.setVisibility(View.VISIBLE);
            }
            else if (state == STATE_INACTIVE) {
                mAddAction.setVisibility(View.GONE);
                mEditButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        setPlaylist(MusicLibrary.getInstance().getPlaylistById(id));

        setEditModeEnabled(false);

        super.onLibraryObjectChanged(type, id);
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        super.onDialogDismiss(dialog, tag);

        if (dialog instanceof NumberPickerDialog) {
            NumberPickerDialog d = (NumberPickerDialog)dialog;
            if (d.confirmed())
                mPlaylist.setValue(d.getValue());
        }
    }

    private void startActionMode() {
        mActionMode = getDelegate().startActionMode(this);
    }

    @Override
    protected int getNoItemsTextRes() {
        return R.string.no_items_playlist;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new SearchTask();
    }

    @Override
    public Sorting[] getSortings() {
        return SORTINGS;
    }

    public void setEditModeEnabled(boolean enabled) {
        if (enabled && !mPlaylist.isMutable()) {
            NumberPickerDialog.newInstance(mPlaylist.getEditingTitleRes(), mPlaylist.getValue()).show(getChildFragmentManager(), TAG_EDIT_DIALOG);
        }
        else if (enabled ^ mEditModeEnabled && !mTempFinish) {
            mEditModeEnabled = enabled;

            setDragHandlesVisible(enabled && mPlaylist.isMutable());
            setSearchAvailable(!enabled);
            updateItems();

            if (mAdapter != null)
                mAdapter.setOnLongClickListener(enabled ? this : null);

            if (!enabled)
                mPlaylist.save();

            if (editButtonVisible()) {
                mEditButton.setImageResource(enabled ? R.drawable.ic_close_black_36dp : R.drawable.ic_edit_black_36dp);
                mActionLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
            else {
                if (enabled) {
                    startActionMode();
                    setHighlightedBarItem(1);
                }
                else {
                    if (mActionMode != null)
                        mActionMode.finish();
                    mActionMode = null;
                    setHighlightedBarItem(0);
                }
            }
        }

        mTempFinish = false;
    }

    public boolean canUndo() {
        return mUndoActions.size() > 0;
    }

    public boolean canRedo() {
        return mRedoActions.size() > 0;
    }

    public void undoAction() {
        Playlist.Action action = mUndoActions.remove(mUndoActions.size() - 1);
        action.undo();
        mRedoActions.add(action);
    }

    public void redoAction() {
        Playlist.Action action = mRedoActions.remove(mRedoActions.size() - 1);
        action.redo();
        mUndoActions.add(action);
    }

    private void setPlaylist(Playlist playlist) {
        if (mPlaylist != null)
            mPlaylist.removeObserver(mPlaylistObserver);

        mPlaylist = playlist;

        if (mOptionsHandler != null)
            mOptionsHandler.setPlaylist(mPlaylist);

        if (mPlaylist != null)
            mPlaylist.addObserver(mPlaylistObserver);
    }

    private void updateActionState(Menu menu) {
        if (!editButtonVisible()) {
            if (menu == null)
                return;

            int undoAlpha = canUndo() ? 255 : 127;
            int redoAlpha = canRedo() ? 255 : 127;

            menu.findItem(R.id.action_undo).setEnabled(canUndo()).getIcon().setAlpha(undoAlpha);
            menu.findItem(R.id.action_redo).setEnabled(canRedo()).getIcon().setAlpha(redoAlpha);
        }
        else {
            mUndoAction.setEnabled(canUndo());
            mUndoAction.setAlpha(canUndo() ? 1.0f : 0.5f);
            mRedoAction.setEnabled(canRedo());
            mRedoAction.setAlpha(canRedo() ? 1.0f : 0.5f);
        }
    }

    private void showEditorFragment() {
        TwoPaneLayout parent = getParent();
        if (parent == null)
            return;

        TwoPaneLayout.Entry nextEntry = parent.findNextEntry(this);
        if (nextEntry == null || nextEntry.getFragment().getType() != AddSongsFragment.TYPE) {
            AddSongsFragment fragment = new AddSongsFragment();
            fragment.setEditingPlaylist(mPlaylist);
            if (nextEntry != null)
                nextEntry.setFragment(fragment);
            else
                parent.pushFragment(fragment);
        }
    }

    private boolean editButtonVisible() {
        return !isSinglePane();
    }
}
