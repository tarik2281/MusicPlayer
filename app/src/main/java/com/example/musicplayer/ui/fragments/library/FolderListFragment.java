package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicplayer.Observable;
import com.example.musicplayer.PreferenceManager;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowFolderRequest;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Folder;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.FolderListAdapter;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.fragments.options.FolderOptionsHandler;

import java.util.Collection;

/**
 * Created by Tarik on 06.08.2016.
 */
public class FolderListFragment extends LibraryAdapterFragment<Folder> {

    public class Task extends ItemsTask {
        @Override
        protected void onPostExecute(Collection<Folder> items) {
            super.onPostExecute(items);

            if (!isCancelled() && Util.stringIsEmpty(filter) && items.isEmpty() && objectType == LibraryObject.FOLDER) {
                setEmptyText(getString(R.string.folders_no_subfolders));
            }
        }

        @Override
        protected Collection<Folder> doInBackground(Void... params) {
            MusicLibrary lib = MusicLibrary.getInstance();

            switch (getObjectType()) {
                case LibraryObject.FOLDER:
                    return lib.getSubfoldersForFolder(objectId, filter, sorting, reversed);
                default:
                    return lib.getAllFolders(filter, sorting, reversed);
            }
        }
    }

    public static final int TYPE = Util.HashFNV1a32("FolderList");

    private static final String TAG_HANDLER = "HANDLER";

    private static final Sorting[] SORTINGS = new Sorting[] {
            Sorting.Name, Sorting.Path
    };

    private PreferenceManager.Observer mPreferenceObserver = new PreferenceManager.Observer() {
        @Override
        public void update(Observable sender, PreferenceManager.ObserverData data) {
            switch (data.type) {
                case PreferenceChange:
                    if (PreferenceManager.KEY_SHOW_PARENT_FOLDERS.equals(data.key)) {
                        mAdapter.setShowParents((boolean) data.value);
                        requestLayout();
                    }
                    break;
            }
        }
    };

    private FolderListAdapter mAdapter;
    private FolderOptionsHandler mOptionsHandler;

    public FolderListFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSorting() == Sorting.ID)
            setSorting(Sorting.Name, false);

        PreferenceManager.getInstance().addObserver(mPreferenceObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        getRecyclerView().setLayoutManager(layoutManager);

        mOptionsHandler = (FolderOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

        if (mOptionsHandler == null) {
            mOptionsHandler = new FolderOptionsHandler();
            mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
        }

        if (mAdapter != null)
            mAdapter.setOnOptionsClickListener(mOptionsHandler);

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferenceManager.getInstance().removeObserver(mPreferenceObserver);
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public int getSortingTitleRes() {
        return R.string.dialog_title_sorting_folders;
    }

    @Override
    public Sorting[] getSortings() {
        return SORTINGS;
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.title_folders);
    }

    @Override
    protected int getNoItemsTextRes() {
        return getObjectType() == LibraryObject.FOLDER ? R.string.folders_no_subfolders : R.string.no_items_folders;
    }

    @Override
    protected int getNoItemsFoundTextRes() {
        return R.string.search_no_folders;
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        ShowFolderRequest request = new ShowFolderRequest(id);
        request.setSenderFragment(getRootPaneFragment());
        RequestManager.getInstance().pushRequest(request);

        setHighlightedPosition(position);
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        mAdapter = new FolderListAdapter();
        mAdapter.setOnOptionsClickListener(mOptionsHandler);
        mAdapter.setShowParents(getObjectType() != LibraryObject.FOLDER && PreferenceManager.getInstance().getBoolean(PreferenceManager.KEY_SHOW_PARENT_FOLDERS));
        return mAdapter;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new Task();
    }

    /*@Override
    public void update(Observable observable, Object o) {
        MusicLibrary.ObserverData data = (MusicLibrary.ObserverData)o;

        switch (data.type) {
            case LibraryUpdated: {
                //long id = 0;
                //int position = getHighlightedPosition();
                //if (position > -1)
                //    id = mAdapter.getId(position);

                setItems(getSorting(), isSortingReversed());

                //if ((id = updateItems(id)) != 0) {
                //    ShowFolderRequest request = new ShowFolderRequest(id);
                //    request.setSenderFragment(getRootPaneFragment());
                //    RequestManager.getInstance().pushRequest(request);
                //}

                break;
            }
        }
    }*/
}
