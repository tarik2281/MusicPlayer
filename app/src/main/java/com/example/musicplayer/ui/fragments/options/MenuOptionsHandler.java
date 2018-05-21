package com.example.musicplayer.ui.fragments.options;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.example.musicplayer.request.PlaySongRequest;
import com.example.musicplayer.playback.PlaybackList;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.TimeUtil;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.dialogs.AddToPlaylistDialog;
import com.example.musicplayer.ui.dialogs.DeleteDialog;
import com.example.musicplayer.ui.dialogs.EditTagsDialog;
import com.example.musicplayer.ui.dialogs.HideDialog;

import java.util.Locale;

/**
 * Created by Tarik on 15.06.2016.
 */
public abstract class MenuOptionsHandler<T extends LibraryObject> extends Fragment implements OptionsAdapter.OnOptionsClickListener,
        PopupMenu.OnMenuItemClickListener {

    private class LoadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            loadInformation();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            StringBuilder message = new StringBuilder();
            getInformationMessage(message, true);
            mInfoDialog.setMessage(message.toString());
        }
    }

    private static final String TAG_ADD_TO_PLAYLIST = "options_handler_add_to_playlist";
    private static final String TAG_EDIT = "EDIT";
    private static final String TAG_DELETE = "options_handler_delete";
    private static final String TAG_HIDE = "options_handle_hide";

    private static final String KEY_TYPE = "TYPE";
    private static final String KEY_ID = "ID";

    private T mItem;
    private Class<T> mClass;

    private long[] mSongsInfo;
    private AlertDialog mInfoDialog;

    public MenuOptionsHandler(Class<T> clazz) {
        mClass = clazz;
    }

    public void attach(FragmentManager fragmentManager, String tag) {
        fragmentManager.beginTransaction().add(this, tag).commit();
    }

    public void detach() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            int type = savedInstanceState.getInt(KEY_TYPE, LibraryObject.UNKNOWN);
            int id = savedInstanceState.getInt(KEY_ID, 0);

            setItem(MusicLibrary.getInstance().getLibraryObject(type, id));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mItem != null) {
            outState.putInt(KEY_TYPE, mItem.getType());
            outState.putInt(KEY_ID, mItem.getId());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        onCreateMenu(inflater, menu);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return onMenuItemSelected(menuItem);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return isMenuVisible() && onMenuItemSelected(item);
    }

    @Override
    public void onItemOptionsClick(View v, Object item) {
        setItem(item);

        showPopup(v);
    }

    public void setItem(Object item) {
        try {
            mItem = mClass.cast(item);
        }
        catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    public void showPopup(View anchor) {
        PopupMenu menu = new PopupMenu(getActivity(), anchor);
        onCreateMenu(menu.getMenuInflater(), menu.getMenu());

        menu.setOnMenuItemClickListener(this);

        menu.show();
    }

    public T getItem() {
        return mItem;
    }

    protected abstract void onCreateMenu(MenuInflater inflater, Menu menu);

    protected boolean onMenuItemSelected(MenuItem item) {
        MusicLibrary lib = MusicLibrary.getInstance();
        PlaybackState state = PlaybackState.getInstance();

        switch (item.getItemId()) {
            case R.id.option_shuffle: {
                if (getItem() != null) {
                    PlaySongRequest request = new PlaySongRequest(getItem(), 0, getSorting(), false, PlaySongRequest.Mode.Shuffle, false);
                    RequestManager.getInstance().pushRequest(request);
                }
                return true;
            }
            case R.id.option_play_next: {
                if (getItem() != null) {
                    PlaybackList list = state.getPlaybackList();
                    if (list != null)
                        //list.addAllToQueueFirst(lib.getSongsForObject(getItem(), null, getSorting(), false));
                        list.addAllToQueueLast(lib.getSongsForObject(getItem(), null, getSorting(), false));
                }
                return true;
            }
            /*case R.id.option_add_to_queue: {
                if (getItem() != null) {
                    PlaybackList list = state.getPlaybackList();
                    if (list != null)
                        list.addAllToQueueLast(lib.getSongsForObject(getItem(), null, getSorting(), false));
                }
                return true;
            }*/
            case R.id.option_add_to_playlist: {
                if (getItem() != null)
                    AddToPlaylistDialog.newInstance(getItem()).show(getChildFragmentManager(), TAG_ADD_TO_PLAYLIST);
                return true;
            }
            case R.id.option_edit_tags: {
                if (getItem() != null)
                    EditTagsDialog.newInstance(getItem()).show(getChildFragmentManager(), TAG_EDIT);
                return true;
            }
            case R.id.option_hide: {
                if (getItem() != null)
                    HideDialog.newInstance(getItem()).show(getChildFragmentManager(), TAG_HIDE);
                return true;
            }
            case R.id.option_delete: {
                if (getItem() != null)
                    DeleteDialog.newInstance(getItem()).show(getChildFragmentManager(), TAG_DELETE);
                return true;
            }
            case R.id.option_information: {
                if (getItem() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getItem().toString());

                    StringBuilder message = new StringBuilder();
                    if (getInformationMessage(message, false))
                        new LoadTask().execute();
                    builder.setMessage(message.toString());
                    mInfoDialog = builder.create();
                    mInfoDialog.show();
                }

                return true;
            }
        }

        return false;
    }

    protected Sorting getSorting() {
        return Sorting.Title;
    }

    protected void addTextInfo(StringBuilder builder, @StringRes int key, String value) {
        if (value != null) {
            builder.append(getString(key));
            builder.append('\n');
            //if (value != null) {
                builder.append("  ");
                builder.append(value);
                builder.append('\n');
            //}
            builder.append('\n');
        }
    }

    protected void addNumberInfo(StringBuilder builder, @StringRes int key, int number, int count) {
        if (number > 0) {
            builder.append(getString(key));
            builder.append('\n');
            //if (number != -1) {
                builder.append("  ");
                builder.append(number);

                if (count != -1) {
                    builder.append('/');
                    builder.append(count);
                }

                builder.append('\n');
            //}
            builder.append('\n');
        }
    }

    protected String getLoadingString() {
        return getString(R.string.info_loading);
    }

    protected boolean getInformationMessage(StringBuilder builder, boolean loaded) {
        String loadingString = getLoadingString();

        String numSongs = loaded ? String.valueOf(mSongsInfo[MusicLibrary.INFORMATION_NUMSONGS]) : loadingString;
        addTextInfo(builder, R.string.info_num_songs, numSongs);

        String duration = loaded ? TimeUtil.durationToString(mSongsInfo[MusicLibrary.INFORMATION_DURATION]) : loadingString;
        addTextInfo(builder, R.string.info_duration, duration);

        String size = loaded ? String.format(Locale.getDefault(), "%.2f MB",
                (mSongsInfo[MusicLibrary.INFORMATION_SIZE] / 1024.0 / 1024.0)) : loadingString;
        addTextInfo(builder, R.string.info_size, size);

        mSongsInfo = null;

        return true;
    }

    protected void loadInformation() {
        mSongsInfo = MusicLibrary.getInstance().getSongsInformation(getItem());
    }
}
