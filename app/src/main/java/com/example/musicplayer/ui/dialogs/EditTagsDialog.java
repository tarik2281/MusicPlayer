package com.example.musicplayer.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.musicplayer.CacheManager;
import com.example.musicplayer.library.Thumbnails;
import com.example.musicplayer.ui.adapters.ItemAdapter;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.Util;
import com.example.musicplayer.io.Decoder;
import com.example.musicplayer.io.MediaTag;
import com.example.musicplayer.io.Metadata;
import com.example.musicplayer.library.Album;
import com.example.musicplayer.library.Artist;
import com.example.musicplayer.library.EqualizerPreset;
import com.example.musicplayer.library.Genre;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 Copyright 2017 Tarik

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
public class EditTagsDialog extends DialogFragment implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, DialogInterface.OnShowListener, ViewTreeObserver.OnGlobalLayoutListener, OnDialogDismissListener {

    private class ExportTask extends AsyncTask<Void, Void, Void> {

        private boolean mResult;
        private String mFilePath;

        @Override
        protected Void doInBackground(Void... params) {
            mFilePath = null;
            Song temp = null;
            MediaTag tag = new MediaTag();

            switch (mEditMode) {
                case EDIT_MODE_SINGLE:
                    temp = mSong;
                    break;
                case EDIT_MODE_MULTIPLE:
                    Collection<Song> songs = MusicLibrary.getInstance().getSongsForObject
                            (LibraryObject.ALBUM, mAlbumId, null, Sorting.ID, false);

                    for (Song song : songs) {
                        if (temp != null)
                            break;

                        if (tag.open(song.getInfo().getFilePath())) {
                            if (tag.hasAlbumArt())
                                temp = song;

                            tag.close();
                        }
                    }

                    break;
            }

            if (tag.open(temp.getInfo().getFilePath())) {
                if (tag.hasAlbumArt()) {
                    File exportFolder = new File(Environment.getExternalStorageDirectory(), EXPORT_FOLDER);
                    if (exportFolder.exists() || exportFolder.mkdir()) {
                        mFilePath = exportFolder.getAbsolutePath() + "/" + mAlbumTitle;
                        String mimeType = null;

                        switch (tag.getAlbumArtType()) {
                            case JPEG:
                                mFilePath += ".jpg";
                                mimeType = "image/jpeg";
                                break;
                            case PNG:
                                mFilePath += ".png";
                                mimeType = "image/png";
                                break;
                            case BMP:
                                mFilePath += ".bmp";
                                mimeType = "image/bmp";
                                break;
                            case GIF:
                                mFilePath += ".gif";
                                mimeType = "image/gif";
                                break;
                            default:
                                break;
                        }

                        if ((mResult = tag.extractAlbumArt(mFilePath))) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                            values.put(MediaStore.MediaColumns.DATA, mFilePath);

                            getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        }
                    }
                }

                tag.close();
            }

            tag.release();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            String message;

            if (mResult)
                message = MessageFormat.format(getString(R.string.toast_edit_export_success), mFilePath);
            else
                message = getString(R.string.toast_edit_export_error);

            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public static class WarnDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

        private static final String KEY_NUM_SONGS = "num_songs";

        private CheckBox mCheckBox;
        private boolean mResult = false;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            int numSongs = getArguments().getInt(KEY_NUM_SONGS);

            mCheckBox = (CheckBox) LayoutInflater.from(getContext()).inflate(R.layout.dialog_check_box, null);
            mCheckBox.setText(R.string.dialog_save_changes_remember);

            int spacing = getResources().getDimensionPixelSize(R.dimen.dialog_view_spacing);
            mCheckBox.setPadding(spacing, spacing, spacing, spacing);
            builder.setView(mCheckBox);

            builder.setTitle(R.string.dialog_save_changes_title);
            builder.setMessage(MessageFormat.format(getString(R.string.dialog_save_changes_message), numSongs));
            builder.setPositiveButton(R.string.dialog_button_yes, this);
            builder.setNegativeButton(R.string.dialog_button_no, null);

            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (mCheckBox.isChecked())
                        CacheManager.getInstance().setBoolean(KEY_CACHE_REMEMBER, true);

                    mResult = true;
                    break;
            }
        }

        public boolean confirmed() {
            return mResult;
        }

        public static WarnDialog newInstance(int numSongs) {
            WarnDialog dialog = new WarnDialog();

            Bundle args = new Bundle();
            args.putInt(KEY_NUM_SONGS, numSongs);
            dialog.setArguments(args);

            return dialog;
        }
    }

    private static final String KEY_CACHE_REMEMBER = "edit_tags_remember";

    private static final String KEY_OBJECT_TYPE = "object_type";
    private static final String KEY_OBJECT_ID = "object_id";
    private static final String KEY_META_TITLE = "meta_title";
    private static final String KEY_META_ARTIST = "meta_artist";
    private static final String KEY_META_ALBUMARTIST = "meta_albumartist";
    private static final String KEY_META_ALBUM = "meta_album";
    private static final String KEY_META_GENRE = "meta_genre";
    private static final String KEY_META_TITLENUMBER = "meta_titlenumber";
    private static final String KEY_META_NUMTITLES = "meta_numtitles";
    private static final String KEY_META_DISCNUMBER = "meta_discnumber";
    private static final String KEY_META_NUMDISCS = "meta_numdiscs";
    private static final String KEY_META_YEAR = "meta_year";
    private static final String KEY_PRESET_POSITION = "preset_position";
    private static final String KEY_SET_ALBUMART = "set_albumart";
    private static final String KEY_ALBUMART_URI = "albumart_uri";
    private static final String KEY_IMAGE_TYPE = "image_type";

    private static final int EDIT_MODE_SINGLE = 0;
    private static final int EDIT_MODE_MULTIPLE = 1;

    private static final int PICKER_REQUEST_CODE = 1;

    private static final String EXPORT_FOLDER = "Album Covers";

    private int mEditMode;
    private Song mSong;
    private Collection<Song> mSongs;

    private View mView;
    private EditText mEditTitle;
    private AutoCompleteTextView mEditArtist;
    private AutoCompleteTextView mEditAlbumArtist;
    private AutoCompleteTextView mEditAlbum;
    private AutoCompleteTextView mEditGenre;
    private EditText mEditTitleNumber;
    private EditText mEditNumTitles;
    private EditText mEditDiscNumber;
    private EditText mEditNumDiscs;
    private EditText mEditYear;
    private Spinner mPresetSpinner;
    private ArrayList<EqualizerPreset> mPresetList;
    private ItemAdapter<EqualizerPreset> mPresetAdapter;
    private ImageView mCoverView;
    private String mAlbumTitle;
    private int mAlbumId;

    private String mTitle;
    private String mArtist;
    private String mAlbumArtist;
    private String mAlbum;
    private String mGenre;
    private int mTitleNumber;
    private int mNumTitles;
    private int mDiscNumber;
    private int mNumDiscs;
    private int mYear;
    private int mPresetPosition;

    private boolean mSetAlbumArt;
    private Bitmap mAlbumArt;
    private Uri mAlbumArtUri;
    private MediaTag.ImageType mImageType;

    private View.OnClickListener mSaveClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mEditMode == EDIT_MODE_MULTIPLE && !CacheManager.getInstance().getBoolean(KEY_CACHE_REMEMBER, false)) {
                WarnDialog.newInstance(mSongs.size()).show(getChildFragmentManager(), "warn_dialog");
            }
            else {
                saveChanges();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlbumId = 0;
        mSetAlbumArt = false;
        mAlbumArtUri = null;
        mImageType = MediaTag.ImageType.Unknown;

        mTitle = null;
        mArtist = null;
        mAlbumArtist = null;
        mAlbum = null;
        mGenre = null;
        mTitleNumber = 0;
        mNumTitles = 0;
        mDiscNumber = 0;
        mNumDiscs = 0;
        mYear = 0;
        mPresetPosition = -1;

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(KEY_META_TITLE);
            mArtist = savedInstanceState.getString(KEY_META_ARTIST);
            mAlbumArtist = savedInstanceState.getString(KEY_META_ALBUMARTIST);
            mAlbum = savedInstanceState.getString(KEY_META_ALBUM);
            mGenre = savedInstanceState.getString(KEY_META_GENRE);
            mTitleNumber = savedInstanceState.getInt(KEY_META_TITLENUMBER);
            mNumTitles = savedInstanceState.getInt(KEY_META_NUMTITLES);
            mDiscNumber = savedInstanceState.getInt(KEY_META_DISCNUMBER);
            mNumDiscs = savedInstanceState.getInt(KEY_META_NUMDISCS);
            mYear = savedInstanceState.getInt(KEY_META_YEAR);
            mPresetPosition = savedInstanceState.getInt(KEY_PRESET_POSITION);

            mSetAlbumArt = savedInstanceState.getBoolean(KEY_SET_ALBUMART);
            String uri = savedInstanceState.getString(KEY_ALBUMART_URI);
            if (uri != null)
                mAlbumArtUri = Uri.parse(uri);
            mImageType = MediaTag.ImageType.values()[savedInstanceState.getInt(KEY_IMAGE_TYPE)];
        }

        Bundle args = getArguments();
        if (args != null) {
            int objectType = args.getInt(KEY_OBJECT_TYPE);
            int objectId = args.getInt(KEY_OBJECT_ID);

            if (objectType == LibraryObject.SONG) {
                mSong = MusicLibrary.getInstance().getSongById(objectId);
                mEditMode = EDIT_MODE_SINGLE;
            }
            else {
                mSongs = MusicLibrary.getInstance().getSongsForObject(objectType, objectId, null, Sorting.ID, false);

                if (mSongs.size() == 1) {
                    mSong = mSongs.iterator().next();
                    mEditMode = EDIT_MODE_SINGLE;
                    mSongs = null;
                }
                else
                    mEditMode = EDIT_MODE_MULTIPLE;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MusicLibrary lib = MusicLibrary.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        int layoutId = 0;

        switch (mEditMode) {
            case EDIT_MODE_SINGLE:
                layoutId = R.layout.dialog_edit_tags;
                break;
            case EDIT_MODE_MULTIPLE:
                layoutId = R.layout.dialog_edit_tags_multi;
                break;
        }

        mView = getActivity().getLayoutInflater().inflate(layoutId, null);
        mEditTitle = (EditText)mView.findViewById(R.id.edit_title);
        mEditArtist = (AutoCompleteTextView)mView.findViewById(R.id.edit_artist);
        mEditAlbumArtist = (AutoCompleteTextView)mView.findViewById(R.id.edit_album_artist);
        mEditAlbum = (AutoCompleteTextView)mView.findViewById(R.id.edit_album);
        mEditGenre = (AutoCompleteTextView)mView.findViewById(R.id.edit_genre);
        mEditTitleNumber = (EditText)mView.findViewById(R.id.edit_track_number);
        mEditNumTitles = (EditText)mView.findViewById(R.id.edit_num_tracks);
        mEditDiscNumber = (EditText)mView.findViewById(R.id.edit_disc_number);
        mEditNumDiscs = (EditText)mView.findViewById(R.id.edit_num_discs);
        mEditYear = (EditText)mView.findViewById(R.id.edit_year);
        mPresetSpinner = (Spinner)mView.findViewById(R.id.spinner_preset);
        mCoverView = (ImageView)mView.findViewById(R.id.cover_view);

        mPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPresetPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        mView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        mCoverView.setOnClickListener(this);

        switch (mEditMode) {
            case EDIT_MODE_SINGLE: {
                mAlbumTitle = Song.getAlbum(mSong);

                mPresetList = lib.getPresets(null);
                mPresetList.add(0, new EqualizerPreset(0, "None", null, false));
                mPresetAdapter = new ItemAdapter<>(getContext(), mPresetList);
                mPresetSpinner.setAdapter(mPresetAdapter);

                Song.Info info = mSong.getInfo();

                if (savedInstanceState == null) {
                    mEditTitle.setText(info.getTitle());
                    mEditArtist.setText(info.getArtist());
                    mEditAlbumArtist.setText(info.getAlbumArtist());
                    mEditAlbum.setText(info.getAlbum());
                    mEditGenre.setText(info.getGenre());
                    if (info.getTitleNumber() > 0)
                        mEditTitleNumber.setText(String.valueOf(info.getTitleNumber()));
                    if (info.getNumTitles() > 0)
                        mEditNumTitles.setText(String.valueOf(info.getNumTitles()));
                    if (info.getDiscNumber() > 0)
                        mEditDiscNumber.setText(String.valueOf(info.getDiscNumber()));
                    if (info.getNumDiscs() > 0)
                        mEditNumDiscs.setText(String.valueOf(info.getNumDiscs()));
                    if (info.getYear() > 0)
                        mEditYear.setText(String.valueOf(info.getYear()));

                    int selection = 0;
                    for (int i = 0; i < mPresetList.size(); i++) {
                        if (mPresetList.get(i).getId() == mSong.getPresetId()) {
                            selection = i;
                            break;
                        }
                    }
                    mPresetSpinner.setSelection(selection);
                }

                if (!mSetAlbumArt) {
                    MediaTag tag = new MediaTag();

                    if (tag.open(info.getFilePath())) {
                        if (tag.hasAlbumArt()) {
                            Decoder decoder = Thumbnails.getInstance().getDecoder();

                            Bitmap tempBitmap = decoder.readAlbumArt(tag);
                            if (tempBitmap != null)
                                setAlbumArt(tempBitmap.copy(Bitmap.Config.RGB_565, false));

                            decoder.release();
                        }

                        tag.close();
                    }

                    tag.release();
                }

                break;
            }

            case EDIT_MODE_MULTIPLE: {
                String artist = null;
                String albumArtist = null;
                int albumId = 0;
                int genreId = 0;
                int year = 0;

                mPresetList = lib.getPresets(null);
                mPresetList.add(0, new EqualizerPreset(-1, "None", null, false));
                mPresetList.add(0, new EqualizerPreset(-1, "", null, false));
                mPresetAdapter = new ItemAdapter<EqualizerPreset>(getContext(), mPresetList);
                mPresetSpinner.setAdapter(mPresetAdapter);

                boolean variousAlbums = false;

                for (Song song : mSongs) {
                    Song.Info info = song.getInfo();

                    if (artist == null) {
                        artist = info.getArtist();
                        if (savedInstanceState == null)
                            mEditArtist.setText(info.getArtist());
                    }
                    else if (!artist.equals(info.getArtist())) {
                        mEditArtist.setHint(R.string.tags_various);
                        if (savedInstanceState == null)
                            mEditArtist.setText(null);
                    }

                    if (albumArtist == null) {
                        albumArtist = info.getAlbumArtist();
                        if (savedInstanceState == null)
                            mEditAlbumArtist.setText(info.getAlbumArtist());
                    }
                    else if (!albumArtist.equals(info.getAlbumArtist())) {
                        mEditAlbumArtist.setHint(R.string.tags_various);
                        if (savedInstanceState == null)
                            mEditAlbumArtist.setText(null);
                    }

                    if (albumId == 0) {
                        albumId = info.getAlbumId();
                        if (savedInstanceState == null)
                            mEditAlbum.setText(info.getAlbum());
                        mAlbumTitle = Song.getAlbum(song);
                    }
                    else if (albumId != info.getAlbumId()) {
                        mEditAlbum.setHint(R.string.tags_various);
                        if (savedInstanceState == null)
                            mEditAlbum.setText(null);
                        variousAlbums = true;
                        mAlbumTitle = null;
                    }

                    if (genreId == 0) {
                        genreId = info.getGenreId();
                        if (savedInstanceState == null)
                            mEditGenre.setText(info.getGenre());
                    }
                    else if (genreId != info.getGenreId()) {
                        mEditGenre.setHint(R.string.tags_various);
                        if (savedInstanceState == null)
                            mEditGenre.setText(null);
                    }

                    if (year <= 0) {
                        year = info.getYear();
                        if (savedInstanceState == null)
                            mEditYear.setText(String.valueOf(info.getYear()));
                    }
                    else if (year != info.getYear()) {
                        mEditYear.setHint(R.string.tags_various);
                        if (savedInstanceState == null)
                            mEditYear.setText(null);
                    }
                }

                if (!variousAlbums) {
                    mAlbumId = albumId;

                    if (!mSetAlbumArt) {
                        File file = Thumbnails.getInstance().getFile(albumId);
                        if (file.exists()) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inPreferredConfig = Bitmap.Config.RGB_565;
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                            if (bitmap != null)
                                setAlbumArt(bitmap);
                        }
                    }
                }

                break;
            }
        }

        ArrayAdapter<Artist> artists = new ArrayAdapter<Artist>(getActivity(),
                android.R.layout.simple_list_item_1,
                lib.getAllArtists(null, Sorting.Name, false).toArray(new Artist[0]));
        ArrayAdapter<Album> albums = new ArrayAdapter<Album>(getActivity(),
                android.R.layout.simple_list_item_1,
                lib.getAllAlbums(null, Sorting.TitleExclusive, false).toArray(new Album[0]));
        ArrayAdapter<Genre> genres = new ArrayAdapter<Genre>(getActivity(),
                android.R.layout.simple_list_item_1,
                lib.getAllGenres(null, Sorting.Name, false).toArray(new Genre[0]));

        if (mEditArtist != null)
            mEditArtist.setAdapter(artists);
        if (mEditAlbumArtist != null)
            mEditAlbumArtist.setAdapter(artists);
        if (mEditAlbum != null)
            mEditAlbum.setAdapter(albums);
        if (mEditGenre != null)
            mEditGenre.setAdapter(genres);

        builder.setView(mView);

        builder.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setPositiveButton(R.string.dialog_button_save, null);

        if (mEditTitle != null)
            mEditTitle.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mTitle = s.toString();
                }
            });

        if (mEditArtist != null)
            mEditArtist.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditArtist.setHint(null);

                    mArtist = s.toString();
                }
            });

        if (mEditAlbumArtist != null)
            mEditAlbumArtist.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditAlbumArtist.setHint(null);

                    mAlbumArtist = s.toString();
                }
            });

        if (mEditAlbum != null)
            mEditAlbum.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditAlbum.setHint(null);

                    mAlbum = s.toString();
                }
            });

        if (mEditGenre != null)
            mEditGenre.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditGenre.setHint(null);

                    mGenre = s.toString();
                }
            });

        if (mEditTitleNumber != null)
            mEditTitleNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int value = Integer.valueOf(s.toString());
                        if (value == 0)
                            value = -1;

                        mTitleNumber = value;
                    }
                    else
                        mTitleNumber = -1;
                }
            });

        if (mEditNumTitles != null)
            mEditNumTitles.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int value = Integer.valueOf(s.toString());
                        if (value == 0)
                            value = -1;

                        mNumTitles = value;
                    }
                    else
                        mNumTitles = -1;
                }
            });

        if (mEditDiscNumber != null)
            mEditDiscNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int value = Integer.valueOf(s.toString());
                        if (value == 0)
                            value = -1;

                        mDiscNumber = value;
                    }
                    else
                        mDiscNumber = -1;
                }
            });

        if (mEditNumDiscs != null)
            mEditNumDiscs.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int value = Integer.valueOf(s.toString());
                        if (value == 0)
                            value = -1;

                        mNumDiscs = value;
                    }
                    else
                        mNumDiscs = -1;
                }
            });

        if (mEditYear != null)
            mEditYear.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        int value = Integer.parseInt(s.toString());
                        if (value == 0)
                            value = -1;

                        mYear = value;
                    }
                    else
                        mYear = -1;
                }
            });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog)dialog;
        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(mSaveClickListener);
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof WarnDialog) {
            if (((WarnDialog)dialog).confirmed())
                saveChanges();
        }
    }

    @Override
    public void onGlobalLayout() {
        if (mCoverView.getMeasuredWidth() > 0 && mCoverView.getMeasuredHeight() > 0) {
            if (mSetAlbumArt && mAlbumArtUri != null) {
                setAlbumArtFromUri(mAlbumArtUri);
            }

            Util.removeLayoutListener(mView, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_META_TITLE, mTitle);
        outState.putString(KEY_META_ARTIST, mArtist);
        outState.putString(KEY_META_ALBUMARTIST, mAlbumArtist);
        outState.putString(KEY_META_ALBUM, mAlbum);
        outState.putString(KEY_META_GENRE, mGenre);
        outState.putInt(KEY_META_TITLENUMBER, mTitleNumber);
        outState.putInt(KEY_META_NUMTITLES, mNumTitles);
        outState.putInt(KEY_META_DISCNUMBER, mDiscNumber);
        outState.putInt(KEY_META_NUMDISCS, mNumDiscs);
        outState.putInt(KEY_META_YEAR, mYear);
        outState.putInt(KEY_PRESET_POSITION, mPresetPosition);

        outState.putBoolean(KEY_SET_ALBUMART, mSetAlbumArt);
        if (mAlbumArtUri != null)
            outState.putString(KEY_ALBUMART_URI, mAlbumArtUri.toString());
        outState.putInt(KEY_IMAGE_TYPE, mImageType.ordinal());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            setAlbumArtFromUri(data.getData());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cover_view: {
                if (mAlbumArt != null) {
                    PopupMenu menu = new PopupMenu(getActivity(), v);
                    menu.inflate(R.menu.options_cover);
                    menu.setOnMenuItemClickListener(this);

                    menu.getMenu().findItem(R.id.option_extract).setVisible(!mSetAlbumArt);

                    menu.show();
                }
                else {
                    openImagePicker();
                }

                break;
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_extract: {
                postExportImage();
                return true;
            }
            case R.id.option_choose: {
                openImagePicker();
                return true;
            }
            case R.id.option_delete: {
                mSetAlbumArt = true;
                setAlbumArt(null);
                return true;
            }
        }

        return false;
    }

    private void saveChanges() {
        StorageManager storages = StorageManager.getInstance();

        boolean changed = mTitle != null ||
                mArtist != null ||
                mAlbumArtist != null ||
                mAlbum != null ||
                mGenre != null ||
                mTitleNumber != 0 ||
                mNumTitles != 0 ||
                mDiscNumber != 0 ||
                mNumDiscs != 0 ||
                mYear != 0;

        if (changed && storages.documentsApiSupported()) {
            switch (mEditMode) {
                case EDIT_MODE_SINGLE: {
                    if (storages.checkSDCardAccess(mSong.getInfo().getFilePath(), getChildFragmentManager()) == StorageManager.RESULT_REQUEST_ACCESS)
                        return;

                    break;
                }
                case EDIT_MODE_MULTIPLE: {
                    for (Song song : mSongs) {
                        if (storages.checkSDCardAccess(song.getInfo().getFilePath(), getChildFragmentManager()) == StorageManager.RESULT_REQUEST_ACCESS)
                            return;
                    }
                    break;
                }
            }
        }

        Collection<Song> songs = mSongs;
        if (songs == null)
            songs = Collections.singleton(mSong);

        Metadata metadata = new Metadata.Builder(true)
                .setTitle(mTitle)
                .setArtist(mArtist)
                .setAlbumArtist(mAlbumArtist)
                .setAlbum(mAlbum)
                .setGenre(mGenre)
                .setTitleNumber(mTitleNumber)
                .setNumTitles(mNumTitles)
                .setDiscNumber(mDiscNumber)
                .setNumDiscs(mNumDiscs)
                .setYear(mYear).build();

        PlaybackState state = PlaybackState.getInstance();

        for (Song song : songs) {
            if (song.equals(state.getCurrentSong())) {
                state.forceStopPlayback();
                break;
            }
        }

        boolean setPreset = mPresetPosition > (mEditMode == EDIT_MODE_MULTIPLE ? 0 : -1);

        int presetId = 0;
        if (setPreset)
            presetId = mPresetList.get(mPresetPosition).getId();
        MusicLibrary.getInstance().postEditSongs(songs, metadata, setPreset, presetId,
                mSetAlbumArt, mAlbumArtUri, mImageType);
        dismiss();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(intent, PICKER_REQUEST_CODE);
    }

    private void postExportImage() {
        ExportTask task = new ExportTask();
        task.execute();
    }

    private void setAlbumArtFromUri(Uri uri) {
        mAlbumArtUri = uri;
        InputStream inputStream = null;

        try {
            inputStream = getActivity().getContentResolver().openInputStream(mAlbumArtUri);

            if (inputStream != null) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, opts);
                inputStream.close();

                opts.inSampleSize = Util.calculateInSampleSize(opts, mCoverView.getMeasuredWidth(), mCoverView.getMeasuredHeight());

                inputStream = getActivity().getContentResolver().openInputStream(mAlbumArtUri);
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                opts.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, opts);

                switch (opts.outMimeType) {
                    case "image/jpg":
                    case "image/jpeg":
                        mImageType = MediaTag.ImageType.JPEG;
                        break;
                    case "image/png":
                        mImageType = MediaTag.ImageType.PNG;
                        break;
                    case "image/bmp":
                        mImageType = MediaTag.ImageType.BMP;
                        break;
                    case "image/gif":
                        mImageType = MediaTag.ImageType.GIF;
                        break;
                    default:
                        mImageType = MediaTag.ImageType.Unknown;
                        break;
                }

                setAlbumArt(bitmap);
            }

            mSetAlbumArt = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setAlbumArt(Bitmap bitmap) {
        if (mAlbumArt != null) {
            mAlbumArt.recycle();
            mAlbumArt = null;
        }
        System.gc();

        mAlbumArt = bitmap;
        if (mAlbumArt == null)
            mCoverView.setBackgroundResource(R.drawable.standard_cover);
        else
            Util.setBackground(mCoverView, new BitmapDrawable(getResources(), mAlbumArt));
    }


    public static EditTagsDialog newInstance(int objectType, int objectId) {
        EditTagsDialog dialog = new EditTagsDialog();

        Bundle args = new Bundle();
        args.putInt(KEY_OBJECT_TYPE, objectType);
        args.putInt(KEY_OBJECT_ID, objectId);
        dialog.setArguments(args);

        return dialog;
    }

    public static EditTagsDialog newInstance(LibraryObject object) {
        return newInstance(object.getType(), object.getId());
    }
}
