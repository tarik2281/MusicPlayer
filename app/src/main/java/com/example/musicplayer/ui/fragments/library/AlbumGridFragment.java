package com.example.musicplayer.ui.fragments.library;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowAlbumRequest;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.Album;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.AlbumGridAdapter;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.fragments.options.AlbumOptionsHandler;

import java.util.Collection;

public class AlbumGridFragment extends LibraryAdapterFragment<Album> implements ViewTreeObserver.OnGlobalLayoutListener {

    public class Task extends ItemsTask {
        @Override
        protected Collection<Album> doInBackground(Void... params) {
            MusicLibrary lib = MusicLibrary.getInstance();

            switch (objectType) {
                case LibraryObject.GENRE:
                    return lib.getAlbumsForGenre(objectId, filter, sorting, reversed);
                case LibraryObject.ARTIST:
                    return lib.getAlbumsForArtist(objectId, filter, sorting, reversed);
                case LibraryObject.NONE:
                    return null;
                default:
                    return lib.getAllAlbums(filter, sorting, reversed);
            }
        }
    }

    private class SpaceDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);

            float halfSpace = mSpacing / 2.0f;
            int leftSpace = (int)Math.floor(halfSpace);
            int rightSpace = (position % mNumColumns == 0) ? (int)Math.floor(mSpacing - leftSpace) : (int)Math.ceil(mSpacing - leftSpace);

            outRect.left = leftSpace;
            outRect.right = rightSpace;
            outRect.top = 0;
            outRect.bottom = 0;
        }
    }

    public static final int TYPE = Util.HashFNV1a32("AlbumGrid");

    private static final String TAG_HANDLER = "HANDLER";

    private static final Sorting[] SORTINGS = new Sorting[] {
            Sorting.Title, Sorting.Artist
    };

    private GridLayoutManager mLayoutManager;
    private AlbumGridAdapter mAdapter;
    private SpaceDecoration mSpaceDecoration;

    private AlbumOptionsHandler mOptionsHandler;

    private int mNumColumns;
    private float mSpacing;

	public AlbumGridFragment() {

	}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSorting() == Sorting.ID)
            setSorting(Sorting.Title, false);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, parent, savedInstanceState);

        v.getViewTreeObserver().addOnGlobalLayoutListener(this);

        calculateColumns();

        mLayoutManager = new GridLayoutManager(getActivity(), mNumColumns);
        mLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        getRecyclerView().setLayoutManager(mLayoutManager);

        mSpaceDecoration = new SpaceDecoration();
        getRecyclerView().addItemDecoration(mSpaceDecoration);

        mOptionsHandler = (AlbumOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);

        if (mOptionsHandler == null) {
            mOptionsHandler = new AlbumOptionsHandler();
            mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
        }

        mOptionsHandler.setShowArtist(showArtist());

        if (mAdapter != null)
            mAdapter.setOnOptionsClickListener(mOptionsHandler);

		return v;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        mLayoutManager = null;
        mSpaceDecoration = null;
    }

    @Override
    public void onGlobalLayout() {
        if (getWidth() == 0) {
            int width = getRecyclerView().getMeasuredWidth();

            if (width > 0) {
                setWidth(width);
                Util.removeLayoutListener(getView(), this);
            }
        }
    }

    @Override
    public void onResize(int width) {
        calculateColumns();
    }

    @Override
    public void onItemClick(View v, int position, int id) {
        ShowAlbumRequest request = new ShowAlbumRequest(id);
        request.setSenderFragment(getRootPaneFragment());
        RequestManager.getInstance().pushRequest(request);

        setHighlightedPosition(position);
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

                setItems(getObjectType(), getObjectId(), getSorting(), isSortingReversed());

                //if ((id = updateItems(id)) != 0) {
                //    ShowAlbumRequest request = new ShowAlbumRequest(id);
                //    request.setSenderFragment(getRootPaneFragment());
                //    RequestManager.getInstance().pushRequest(request);
                //}

                break;
            }
        }
    }*/

    @Override
    public int getSortingTitleRes() {
        return R.string.dialog_title_sorting_albums;
    }

    @Override
    public Sorting[] getSortings() {
        return SORTINGS;
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.title_albums);
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    protected int getNoItemsTextRes() {
        return R.string.no_items_albums;
    }

    @Override
    protected int getNoItemsFoundTextRes() {
        return R.string.search_no_albums;
    }

    @Override
    protected OptionsAdapter initializeAdapter() {
        mAdapter = new AlbumGridAdapter();
        mAdapter.setOnOptionsClickListener(mOptionsHandler);
        mAdapter.setShowArtist(showArtist());
        return mAdapter;
    }

    @Override
    protected ItemsTask getItemsTask() {
        return new Task();
    }

    private int getItemSize() {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(density * (140 + 8));
    }

    private void calculateColumns() {
        if (getActivity() == null)
            return;

        float itemSize = (float)getItemSize();

        mNumColumns = (int)Math.floor((float)getWidth() / itemSize);

        if (mNumColumns < 1)
            mNumColumns = 1;

        mSpacing = (float)getWidth() / (float)mNumColumns - itemSize;

        if (mLayoutManager != null)
            mLayoutManager.setSpanCount(mNumColumns);

        if (mSpaceDecoration != null)
            getRecyclerView().invalidateItemDecorations();
    }

    private boolean showArtist() {
        return getObjectType() != LibraryObject.ARTIST;
    }
}
