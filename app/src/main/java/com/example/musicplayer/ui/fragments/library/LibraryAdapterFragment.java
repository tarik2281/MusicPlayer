package com.example.musicplayer.ui.fragments.library;

import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.musicplayer.ui.FastScroller;
import com.example.musicplayer.Observable;
import com.example.musicplayer.OnItemClickListener;
import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Sorting;
import com.example.musicplayer.ui.adapters.OptionsAdapter;
import com.example.musicplayer.ui.dialogs.OnDialogDismissListener;
import com.example.musicplayer.ui.dialogs.SortingDialog;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

/**
 * Created by 19tarik97 on 30.08.16.
 */
public abstract class LibraryAdapterFragment<T extends LibraryObject> extends LibraryFragment implements OnItemClickListener,
        SearchView.OnQueryTextListener, OnDialogDismissListener {

    public abstract class ItemsTask extends AsyncTask<Void, Void, Collection<T>> {
        protected String filter;
        protected int objectType;
        protected int objectId;
        protected Sorting sorting;
        protected boolean reversed;

        @Override
        protected void onPreExecute() {
            filter = getSearchQuery();
            objectType = getObjectType();
            objectId = getObjectId();
            sorting = getSorting();
            reversed = isSortingReversed();
        }

        @Override
        protected void onPostExecute(Collection<T> items) {
            if (items != null) {
                setItems(items);

                if (items.size() == 0) {
                    String emptyText = null;

                    if (!Util.stringIsEmpty(filter))
                        emptyText = MessageFormat.format(getString(getNoItemsFoundTextRes()), filter);
                    else
                        emptyText = getString(getNoItemsTextRes());

                    setEmptyText(emptyText);
                }
            }
        }
    }

    private class FilterScrollListener extends RecyclerView.OnScrollListener {

        private static final int UP = 0;
        private static final int DOWN = 1;

        private int currentPoint = 0;
        private int lastDirection;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                mFilterView.setTranslationY(currentPoint = 0);
                return;
            }

            int nextPoint = currentPoint + dy;

            if (nextPoint < 0)
                nextPoint = 0;

            if (nextPoint > mFilterView.getMeasuredHeight())
                nextPoint = mFilterView.getMeasuredHeight();

            lastDirection = dy > 0 ? UP : DOWN;

            currentPoint = nextPoint;

            mFilterView.setTranslationY(-currentPoint);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                int scrollOffset = recyclerView.computeVerticalScrollOffset();

                boolean scrollFit = scrollOffset >= mFilterView.getMeasuredHeight();

                if (lastDirection == UP && scrollFit) {
                    currentPoint = mFilterView.getMeasuredHeight();
                } else if (lastDirection == DOWN || !scrollFit) {
                    currentPoint = 0;
                }

                if (!mFastScrolling)
                    mFilterView.animate().translationY(-currentPoint);
                else
                    mFilterView.setTranslationY(currentPoint = 0);
            }
        }
    }

    private class FilterSpaceDecor extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);

            RecyclerView.LayoutManager manager = parent.getLayoutManager();

            int spanCount = 1;

            if (manager instanceof GridLayoutManager) {
                spanCount = ((GridLayoutManager)manager).getSpanCount();
            }

            outRect.set(0, 0, 0, 0);

            if (position < spanCount)
                outRect.top = mFilterView.getMeasuredHeight();
        }
    }

    private class ButtonSpaceDecor extends RecyclerView.ItemDecoration {

        private View mView;

        public ButtonSpaceDecor(View view) {
            mView = view;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);

            outRect.set(0, 0, 0, 0);

            if (position == mAdapter.getItemCount() - 1)
                outRect.bottom = mView.getMeasuredHeight() + mView.getPaddingTop() + mView.getPaddingBottom();
        }
    }

    private static final String KEY_SORTING = "SORTING";
    private static final String KEY_SORTING_REVERSE = "SORTING_REVERSE";
    private static final String KEY_HIGHLIGHT = "HIGHLIGHT";
    private static final String KEY_QUERY = "query";

    private static final String TAG_SORTING_DIALOG = "sorting_dialog";

    private Sorting mSorting = Sorting.ID;
    private boolean mSortingReversed = false;
    private int mHighlightedPosition = -1;
    private String mSearchQuery;

    private RecyclerView mRecyclerView;
    private OptionsAdapter<T, ?> mAdapter;

    private TextView mEmptyText;
    private int mEmptyVisibility = View.VISIBLE;
    private String mEmptyString;

    private boolean mSearchAvailable = true;
    private boolean mFastScrolling = false;
    private View mFilterView;
    private FilterScrollListener mFilterScrollListener;
    private FilterSpaceDecor mFilterSpaceDecor;
    private ItemsTask mItemsTask;
    private SearchView mSearchView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSorting = Sorting.values()[savedInstanceState.getInt(KEY_SORTING)];
            mSortingReversed = savedInstanceState.getBoolean(KEY_SORTING_REVERSE);
            mHighlightedPosition = savedInstanceState.getInt(KEY_HIGHLIGHT);
            mSearchQuery = savedInstanceState.getString(KEY_QUERY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(getLayoutResource(), null);

        FastScroller scroller = (FastScroller) v.findViewById(R.id.scroller);
        mRecyclerView = (RecyclerView)v.findViewById(R.id.list);
        mEmptyText = (TextView)v.findViewById(R.id.text_empty);

        if (mEmptyText != null) {
            mEmptyText.setVisibility(mEmptyVisibility);
            mEmptyText.setText(mEmptyString);
        }

        if (mRecyclerView != null) {
            mRecyclerView.setHasFixedSize(true);

            if (scroller != null)
                scroller.attachRecyclerView(mRecyclerView);

            if (mAdapter != null)
                mRecyclerView.setAdapter(mAdapter);
        }

        if (scroller != null) {
            scroller.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        mFastScrolling = true;
                    else if (event.getAction() == MotionEvent.ACTION_UP ||
                            event.getAction() == MotionEvent.ACTION_CANCEL)
                        mFastScrolling = false;

                    return false;
                }
            });
        }

        mFilterScrollListener = new FilterScrollListener();
        mFilterSpaceDecor = new FilterSpaceDecor();

        mFilterView = inflater.inflate(R.layout.view_filter, (ViewGroup) v, false);
        mSearchView = (SearchView) mFilterView.findViewById(R.id.view_search);
        mSearchView.setOnQueryTextListener(this);

        View sortingView = mFilterView.findViewById(R.id.view_sorting);

        if (getSortings() == null || getSortings().length == 0) {
            sortingView.setVisibility(View.GONE);
        }
        else {
            sortingView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SortingDialog.newInstance(getSortingTitleRes(), getSortings(), getSorting(),
                            isSortingReversed()).show(getChildFragmentManager(), TAG_SORTING_DIALOG);
                }
            });
        }

        int fastScrollerIndex = 0;
        int childCount = ((ViewGroup) v).getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (((ViewGroup) v).getChildAt(i) == scroller) {
                fastScrollerIndex = i;
                break;
            }
        }

        ((ViewGroup) v).addView(mFilterView, fastScrollerIndex);

        if (mSearchAvailable) {
            mRecyclerView.addOnScrollListener(mFilterScrollListener);
            mRecyclerView.addItemDecoration(mFilterSpaceDecor);
        }
        else {
            mFilterView.setVisibility(View.GONE);
            mFilterView.setEnabled(false);
        }

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateItems();
    }

    @Override
    public void onStart() {
        super.onStart();

        notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_SORTING, mSorting.ordinal());
        outState.putBoolean(KEY_SORTING_REVERSE, mSortingReversed);
        outState.putInt(KEY_HIGHLIGHT, mHighlightedPosition);
        outState.putString(KEY_QUERY, mSearchQuery);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mItemsTask != null)
            mItemsTask.cancel(false);
    }

    @Override
    public void onStateChanged(int state) {
        if (state == STATE_ACTIVE) {
            setHighlightedPosition(-1);
            notifyDataSetChanged();
        }
    }

    @Override
    protected void onLibraryObjectChanged(int type, int id) {
        setHighlightedPosition(-1);
        updateItems();
    }

    /**
     * Item click callback for the adapter
     * @param v triggering view
     * @param position item adapter position
     * @param id item id
     */
    @Override
    public void onItemClick(View v, int position, int id) {

    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof SortingDialog) {
            SortingDialog sortingDialog = (SortingDialog)dialog;
            if (sortingDialog.getSorting() != null)
                setSorting(sortingDialog.getSorting(), sortingDialog.isSortingReversed());
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchQuery = query;
        onSearchQuery(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchQuery = newText;
        onSearchQuery(newText);
        return true;
    }

    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        switch (data.type) {
            case LibraryUpdated:
                updateItems();
                break;
        }
    }

    public Sorting getSorting() {
        return mSorting;
    }

    public boolean isSortingReversed() {
        return mSortingReversed;
    }

    public int getSortingTitleRes() {
        return 0;
    }

    public Sorting[] getSortings() {
        return null;
    }

    public int getHighlightedPosition() {
        return mHighlightedPosition;
    }

    public void setSorting(Sorting sorting, boolean reversed) {
        boolean changed = created() && (mSorting != sorting || mSortingReversed != reversed);

        mSorting = sorting;
        mSortingReversed = reversed;

        if (changed)
            updateItems();
    }

    public void setItems(Collection<T> items) {
        if (mAdapter == null) {
            setAdapter(initializeAdapter());
        }

        if (items instanceof List)
            mAdapter.setItems((List<T>)items);
        else
            mAdapter.setItems(items);

        notifyDataSetChanged();
    }

    public void setAdapter(OptionsAdapter adapter) {
        if (mAdapter != null)
            mAdapter.setOnItemClickListener(null);

        mAdapter = adapter;

        if (mAdapter != null) {
            mAdapter.setOnItemClickListener(this);

            setHighlightedPosition(mHighlightedPosition);
        }

        if (mRecyclerView != null)
            mRecyclerView.setAdapter(mAdapter);

        updateEmptyVisibility();
    }

    public void setEmptyText(String emptyText) {
        mEmptyString = emptyText;

        if (mEmptyText != null)
            mEmptyText.setText(mEmptyString);
    }

    /**
     * Highlight an item. This has no effect in single pane mode.
     * @param position item adapter position
     */
    public void setHighlightedPosition(int position) {
        if (!highlightItems())
            return;

        mHighlightedPosition = position;

        if (mAdapter != null)
            mAdapter.setHighlightedItem(position);
    }

    public void notifyDataSetChanged() {
        updateEmptyVisibility();

        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    public void requestLayout() {
        if (mRecyclerView != null)
            mRecyclerView.setAdapter(mAdapter);
    }


    protected void addButtonSpace(View view) {
        getRecyclerView().addItemDecoration(new ButtonSpaceDecor(view));
    }

    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.fragment_library;
    }

    @StringRes
    protected int getNoItemsTextRes() {
        return 0;
    }

    @StringRes
    protected int getNoItemsFoundTextRes() {
        return 0;
    }

    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    protected abstract OptionsAdapter initializeAdapter();

    protected abstract ItemsTask getItemsTask();

    protected void setSearchAvailable(boolean available) {
        mSearchAvailable = available;

        if (!available) {
            mSearchQuery = null;
            if (mSearchView != null)
                mSearchView.setQuery(null, false);
        }

        if (mFilterView != null) {
            if (!mSearchAvailable) {
                mRecyclerView.removeOnScrollListener(mFilterScrollListener);
                mRecyclerView.removeItemDecoration(mFilterSpaceDecor);
                mFilterView.setVisibility(View.GONE);
                mFilterView.setEnabled(false);
            }
            else {
                mRecyclerView.addOnScrollListener(mFilterScrollListener);
                mRecyclerView.addItemDecoration(mFilterSpaceDecor);
                mFilterView.setVisibility(View.VISIBLE);
                mFilterView.setEnabled(true);
            }
        }
    }

    protected String getSearchQuery() {
        return mSearchQuery;
    }

    protected long updateItems(long currentItemId) {
        if (currentItemId != 0) {
            int position = getHighlightedPosition();

            boolean found = false;

            for (int i = 0; i < mAdapter.getItemCount() && !found; i++) {
                LibraryObject object = mAdapter.getItem(i);

                if (currentItemId == object.getId()) {
                    found = true;

                    if (i != position)
                        setHighlightedPosition(i);
                }
            }

            if (!found) {
                if (mAdapter.getItemCount() == 0)
                    requestBack();
                else {
                    while (position >= mAdapter.getItemCount())
                        position--;

                    setHighlightedPosition(position);

                    return mAdapter.getId(position);
                }
            }
        }

        return 0;
    }


    protected void updateItems() {
        if (mItemsTask != null)
            mItemsTask.cancel(false);

        if (created()) {
            mItemsTask = getItemsTask();

            if (mItemsTask != null)
                mItemsTask.execute();
        }
    }

    private void updateEmptyVisibility() {
        mEmptyVisibility = (mAdapter != null && mAdapter.getItemCount() > 0) ? View.INVISIBLE : View.VISIBLE;

        if (mEmptyText != null)
            mEmptyText.setVisibility(mEmptyVisibility);
    }

    private boolean highlightItems() {
        return !isSinglePane();
    }

    private void onSearchQuery(String query) {
        updateItems();
    }
}
