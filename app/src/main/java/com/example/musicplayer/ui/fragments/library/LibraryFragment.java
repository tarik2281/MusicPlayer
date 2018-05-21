package com.example.musicplayer.ui.fragments.library;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import com.example.musicplayer.ui.LibraryBar;
import com.example.musicplayer.LibraryBarCreateCallback;
import com.example.musicplayer.LibraryDelegate;
import com.example.musicplayer.Observable;
import com.example.musicplayer.Util;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.fragments.FragmentCreateListener;
import com.example.musicplayer.ui.fragments.PaneFragment;
import com.example.musicplayer.ui.fragments.options.MenuOptionsHandler;

public abstract class LibraryFragment extends PaneFragment implements LibraryBarCreateCallback,
        LibraryBar.Callback, MusicLibrary.Observer {

    private static final String KEY_TYPE = "TYPE";
    private static final String KEY_ID = "ID";
    private static final String KEY_HIGHLIGHTED_BAR_ITEM = "HIGHLIGHTED_BAR_ITEM";

    private static final String TAG_HANDLER = "OPTIONS_HANDLER";

    private int mObjectType = LibraryObject.UNKNOWN;
    private int mObjectId = -1;
    private int mHighlightedItem = 0;

    private LibraryDelegate mDelegate;

    private Class mOptionsHandlerClass;
    private MenuOptionsHandler mOptionsHandler;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mObjectType = savedInstanceState.getInt(KEY_TYPE, LibraryObject.UNKNOWN);
            mObjectId = savedInstanceState.getInt(KEY_ID, -1);
            mHighlightedItem = savedInstanceState.getInt(KEY_HIGHLIGHTED_BAR_ITEM, 0);
        }

		super.onCreate(savedInstanceState);

		Fragment parent = getParentFragment();
		Activity activity = getActivity();

		if (parent != null && parent instanceof FragmentCreateListener)
			((FragmentCreateListener)parent).onCreateFragment(getClass(), this);

        if (activity != null && activity instanceof FragmentCreateListener)
			((FragmentCreateListener)activity).onCreateFragment(getClass(), this);

        MusicLibrary.getInstance().addObserver(this);
	}

	@SuppressLint("RestrictedApi")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mOptionsHandler = (MenuOptionsHandler)getChildFragmentManager().findFragmentByTag(TAG_HANDLER);
        if (mOptionsHandler == null && mOptionsHandlerClass != null) {
            try {
                mOptionsHandler = (MenuOptionsHandler) mOptionsHandlerClass.newInstance();
                mOptionsHandler.attach(getChildFragmentManager(), TAG_HANDLER);
            }
            catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (mOptionsHandler != null) {
            mOptionsHandler.setHasOptionsMenu(hasOptionsMenu());
            mOptionsHandler.setMenuVisibility(isMenuVisible());
            mOptionsHandler.setItem(MusicLibrary.getInstance().getLibraryObject(getObjectType(), getObjectId()));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MusicLibrary.getInstance().removeObserver(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_TYPE, mObjectType);
        outState.putInt(KEY_ID, mObjectId);
        outState.putInt(KEY_HIGHLIGHTED_BAR_ITEM, mHighlightedItem);
    }

    @Override
    public void setHasOptionsMenu(boolean hasMenu) {
        super.setHasOptionsMenu(hasMenu);

        if (mOptionsHandler != null)
            mOptionsHandler.setHasOptionsMenu(hasMenu);
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        if (mOptionsHandler != null)
            mOptionsHandler.setMenuVisibility(menuVisible);
    }

	@Override
	public boolean hasBarMenu() {
		return false;
	}

	@Override
	public void onCreateBarMenu(MenuInflater inflater, Menu menu) {

	}

	@Override
	public void onBarItemClick(LibraryBar.Item item) {

	}

	@Override
	public boolean onBarItemLongClick(LibraryBar.Item item) {
		return false;
	}

    /**
     * Observer callback for MusicLibrary<br/>
     * override this to receive library changes
     * @param sender {@link MusicLibrary} instance
     * @param data {@link MusicLibrary.ObserverData}
     */
    @Override
    public void update(Observable sender, MusicLibrary.ObserverData data) {
        Log.i("LibraryFragment", "update library: " + data.type.name());
    }

    public LibraryDelegate getDelegate() {
        return mDelegate;
    }

    @SuppressWarnings("unchecked")
    public <T extends MenuOptionsHandler> T getOptionsHandler() {
        return (T)mOptionsHandler;
    }

    public int getObjectType() {
        return mObjectType;
    }

    public int getObjectId() {
        return mObjectId;
    }

    public int getHighlightedBarItem() {
        return mHighlightedItem;
    }

    public void setDelegate(LibraryDelegate delegate) {
        mDelegate = delegate;
    }

    public void setLibraryObject(int type, int id) {
        boolean changed = created() && (mObjectType != type || mObjectId != id);

        mObjectType = type;
        mObjectId = id;

        if (changed)
            onLibraryObjectChanged(type, id);

        if (mOptionsHandler != null) {
            mOptionsHandler.setItem(MusicLibrary.getInstance().getLibraryObject(type, id));
        }
    }

    public void setLibraryObject(@Nullable LibraryObject object) {
        if (object == null) {
            Util.log("LibraryFragment", "setLibraryObject: received null");
            return;
        }

        setLibraryObject(object.getType(), object.getId());
    }

    public void setHighlightedBarItem(int position) {
        mHighlightedItem = position;

        if (getDelegate() != null && getState() == STATE_ACTIVE)
            getDelegate().setHighlightedItem(position);
    }


    protected void onLibraryObjectChanged(int type, int id) {

    }

    protected void setOptionsHandlerClass(Class handlerClass) {
        mOptionsHandlerClass = handlerClass;
        setHasOptionsMenu(true);
    }
}
