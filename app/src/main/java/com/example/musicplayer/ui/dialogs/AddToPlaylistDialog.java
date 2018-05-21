package com.example.musicplayer.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.musicplayer.ui.adapters.ItemAdapter;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;

/**
 * Created by 19tarik97 on 08.10.16.
 */
public class AddToPlaylistDialog extends BaseDialogFragment implements DialogInterface.OnClickListener,
        DialogInterface.OnShowListener, View.OnClickListener, OnDialogDismissListener,
        AdapterView.OnItemClickListener {

    private static final String TAG_NEW_PLAYLIST = "add_to_playlist_new_playlist";

    private static final String KEY_TYPE = "type";
    private static final String KEY_ID = "id";

    private ItemAdapter<Playlist> mAdapter;
    private Collection<Song> mSongs;
    private int mPlaylistPosition;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View v = inflater.inflate(R.layout.dialog_choose_playlist, null);
        ListView listView = (ListView)v.findViewById(R.id.list);

        mAdapter = new ItemAdapter<>(getContext(), getArray());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        builder.setView(v);
        builder.setTitle(R.string.dialog_title_add_to_playlist);

        builder.setNegativeButton(R.string.dialog_button_cancel, this);
        builder.setPositiveButton(R.string.dialog_button_new, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog)dialog;

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // cancel button clicked
    }

    @Override
    public void onClick(View v) {
        // new playlist button clicked
        PlaylistNameDialog.newInstance().show(getChildFragmentManager(), TAG_NEW_PLAYLIST);
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        // update playlists
        if (dialog instanceof PlaylistNameDialog) {
            mAdapter.setItems(getArray());
            mAdapter.notifyDataSetChanged();
        }
        else if (dialog instanceof AddDuplicatesDialog) {
            addSongsToPlaylist(((AddDuplicatesDialog)dialog).getAddDuplicates());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MusicLibrary lib = MusicLibrary.getInstance();
        Bundle args = getArguments();

        int objectType = args.getInt(KEY_TYPE);
        int objectId = args.getInt(KEY_ID);

        mSongs = lib.getSongsForObject(objectType, objectId, null, Sorting.Default, false);
        mPlaylistPosition = position;
        Playlist playlist = mAdapter.getItem(position);
        if (!playlist.isLoaded())
            playlist.load();

        int state = PreferenceManager.getInstance().parseInt(PreferenceManager.KEY_PLAYLIST_ADD_DUPLICATE);

        switch (state) {
            case AddDuplicatesDialog.ADD_DUPLICATE_ALWAYS:
                addSongsToPlaylist(true);
                break;
            case AddDuplicatesDialog.ADD_DUPLICATE_NEVER:
                addSongsToPlaylist(false);
                break;
            case AddDuplicatesDialog.ADD_DUPLICATE_ASK: {
                if (playlist.containsOne(mSongs)) {
                    AddDuplicatesDialog.newInstance(mSongs.size() > 1).show(getChildFragmentManager(), "add_duplicates_dialog");
                    return;
                }
                else {
                    addSongsToPlaylist(true);
                }
                break;
            }
        }
    }

    private void addSongsToPlaylist(boolean addDuplicates) {
        Playlist playlist = mAdapter.getItem(mPlaylistPosition);

        if (!playlist.isLoaded())
            playlist.load();

        int numSongs = playlist.addSongs(mSongs, addDuplicates);
        playlist.save();

        MessageFormat format = new MessageFormat(getString(R.string.toast_added_songs_to_playlist), Locale.getDefault());
        double[] limits = { 0, 1, 2 };
        ChoiceFormat choice = new ChoiceFormat(limits, getResources().getStringArray(R.array.toast_added_songs_choice));
        format.setFormatByArgumentIndex(0, choice);
        Object[] formArgs = { numSongs, playlist.getName() };
        Toast.makeText(getContext(), format.format(formArgs), Toast.LENGTH_SHORT).show();

        mSongs = null;

        dismiss();
    }

    private Playlist[] getArray() {
        Collection<Playlist> playlists = MusicLibrary.getInstance().getEditablePlaylists(null);
        Playlist[] array = new Playlist[playlists.size()];
        return playlists.toArray(array);
    }

    public static AddToPlaylistDialog newInstance(int objectType, int objectId) {
        AddToPlaylistDialog dialog = new AddToPlaylistDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_TYPE, objectType);
        args.putInt(KEY_ID, objectId);

        dialog.setArguments(args);
        return dialog;
    }

    public static AddToPlaylistDialog newInstance(LibraryObject object) {
        return newInstance(object.getType(), object.getId());
    }
}
