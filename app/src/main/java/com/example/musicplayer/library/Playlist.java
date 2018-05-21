package com.example.musicplayer.library;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.musicplayer.IObserver;
import com.example.musicplayer.Observable;
import com.example.musicplayer.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Playlist extends Observable<Playlist.ObserverData> implements LibraryObject {

	public interface Observer extends IObserver<ObserverData> { }

	public static class Action implements Parcelable {
		public enum Type {
			Added, Removed, Moved
		}
		
		public final Type type;
		public final Playlist playlist;
		public final Song[] items;
		public final int position;
		public final int size;
		public final int oldPosition;
		
		private Action(Type type, Playlist playlist, Song[] items, int position, int size, int oldPosition) {
			this.type = type;
			this.playlist = playlist;
			this.items = items;
			this.position = position;
			this.size = size;
			this.oldPosition = oldPosition;
		}

		protected Action(Parcel in) {
			MusicLibrary lib = MusicLibrary.getInstance();

			type = Type.values()[in.readInt()];
			playlist = lib.getPlaylistById(in.readInt());
			items = new Song[in.readInt()];

			for (int i = 0; i < items.length; i++)
				items[i] = lib.getSongById(in.readInt());

			position = in.readInt();
			size = in.readInt();
			oldPosition = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(type.ordinal());
			dest.writeInt(playlist.getId());

			if (items != null) {
				dest.writeInt(items.length);

				for (Song song : items)
					dest.writeInt(song.getId());
			}
			else
				dest.writeInt(0);

			dest.writeInt(position);
			dest.writeInt(size);
			dest.writeInt(oldPosition);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final Creator<Action> CREATOR = new Creator<Action>() {
			@Override
			public Action createFromParcel(Parcel in) {
				return new Action(in);
			}

			@Override
			public Action[] newArray(int size) {
				return new Action[size];
			}
		};

		public void undo() {
			switch (type) {
				case Added:
					playlist.removeSongs(position, size, false);
					break;
				case Removed:
					playlist.addSongs(Arrays.asList(items), position, true, false);
					break;
				case Moved:
					playlist.moveSong(position, oldPosition, false);
					break;
			}
		}
		
		public void redo() {
			switch (type) {
				case Added:
					playlist.addSongs(Arrays.asList(items), position, true, false);
					break;
				case Removed:
					playlist.removeSong(position, false);
					break;
				case Moved:
					playlist.moveSong(oldPosition, position, false);
					break;
			}
		}
	}

	public static class ObserverData {
		public enum Type {
			Added, Removed, Moved, Changed, Renamed, Deleted, Invalidated
		}

		public final Type type;
		public final Playlist playlist;
		public final Action action;
		public final Song[] items;
		public final int position;
		public final int size;
		public final int oldPosition;

		private ObserverData(Type type, Playlist playlist) {
			this.type = type;
			this.playlist = playlist;
			this.action = null;
			this.items = null;
			this.position = -1;
			this.size = 0;
			this.oldPosition = -1;
		}

		private ObserverData(Type type, Playlist playlist, Action action, Song[] items, int position, int size, int oldPosition) {
			this.type = type;
			this.playlist = playlist;
			this.action = action;
			this.items = items;
			this.position = position;
			this.size = size;
			this.oldPosition = oldPosition;
		}
	}
	
	public static class PlaylistComparator implements Comparator<Playlist> {

		@Override
		public int compare(Playlist lhs, Playlist rhs) {
            int res = Util.boolCompare(lhs.mIsMutable, rhs.mIsMutable);

			if (res == 0)
				res = Util.stringCompare(lhs.getName(), rhs.getName());

			if (res == 0)
				res = Util.longCompare(lhs.getId(), rhs.getId());

			return res;
		}
		
	}

	// TODO: redesign playlist structure
	// TODO: maybe put undo/redo actions in playlist (-> not bound to fragment)
    // will keep the actions in memory till application close or memory trim
    // TODO: show up undo action in SnackBar after adding through options menu
	protected int mId;
	protected String mName;
	protected String mImportPath;
	protected boolean mIsMutable;

	protected boolean mLoaded;
	protected ArrayList<Song> mSongs;

	private int mOldPosition;
	private int mNewPosition;

	private boolean mEnabled;

	private boolean mDeleted;

    // TODO: thread safe editing

	public Playlist(int id, String name) {
		this(id, name, null);
	}

	public Playlist(int id, String name, String importPath) {
		this(id, name, importPath, true);
	}

	protected Playlist(int id, String name, String importPath, boolean isMutable) {
		mId = id;
		mName = name;
		mImportPath = Util.stringIsEmpty(importPath) ? null : importPath;
		mIsMutable = isMutable;
		mLoaded = false;
		mSongs = new ArrayList<>();
		mOldPosition = -1;
		mNewPosition = -1;

		mDeleted = false;
		mEnabled = true;
	}
	
	@Override
	public int getType() {
		return PLAYLIST;
	}

	@Override
	public int getId() {
		return mId;
	}
	
	public String getName() {
		return mName;
	}

    void setName(String name) {
		mName = name;
	}

	public String getImportPath() {
		return mImportPath;
	}

	public boolean isMutable() {
		return mIsMutable;
	}

    public boolean isLoaded() {
        return mLoaded;
    }

	public List<Song> getSongList() {
		if (!mLoaded)
			load();

		return mSongs;
	}
	
	@Override
	public String toString() {
		return mName;
	}


	public Action addSong(Song song) {
		return addSong(song, mSongs.size());
	}

	public Action addSong(Song song, int position) {
		return addSong(song, position, true);
	}

	private Action addSong(Song song, int position, boolean createAction) {
		Action action = null;
		mSongs.add(position, song);

		Song[] items = new Song[] { song };

		if (createAction)
			action = new Action(Action.Type.Added, this, items, position, 1, -1);

		notifyObservers(new ObserverData(ObserverData.Type.Added, this, action, items, position, 1, -1));
		return action;
	}


	public int addSongs(Collection<Song> songs, boolean addDuplicate) {
		return addSongs(songs, mSongs.size(), addDuplicate);
	}

	public int addSongs(Collection<Song> songs, int position, boolean addDuplicate) {
		return addSongs(songs, position, addDuplicate, true);
	}

	private int addSongs(Collection<Song> songs, int position, boolean addDuplicate, boolean createAction) {
		Action action = null;
		int numSongs = 0;

		if (addDuplicate) {
			mSongs.addAll(position, songs);
			numSongs = songs.size();
		}
		else {
			int insertionPosition = position;

			for (Song song : songs) {
				if (!mSongs.contains(song)) {
					mSongs.add(insertionPosition++, song);
					numSongs++;
				}
			}
		}

		Song[] array = new Song[numSongs];
		mSongs.subList(position, position + numSongs).toArray(array);

		if (createAction) {
			action = new Action(Action.Type.Added, this, array, position, numSongs, -1);
		}

		notifyObservers(new ObserverData(ObserverData.Type.Added, this, action, array, position, numSongs, -1));

		return numSongs;
	}


	// returns true if this playlists contains at least one of the given songs
	public boolean containsOne(Collection<Song> songs) {
		for (Song song : songs) {
			if (mSongs.contains(song))
				return true;
		}

		return false;
	}


	public Action removeSong(int position) {
		return removeSong(position, true);
	}

    Action removeSong(int position, boolean createAction) {
		Action action = null;
		Song song = mSongs.remove(position);

		Song[] items = new Song[] { song };

		if (createAction)
			action = new Action(Action.Type.Removed, this, items, position, 1, -1);

		notifyObservers(new ObserverData(ObserverData.Type.Removed, this, action, items, position, 1, -1));
		return action;
	}


	// removes all occurrences of the given song
	void removeSongById(int id) {
        for (Iterator<Song> it = mSongs.iterator(); it.hasNext(); ) {
            if (it.next().getId() == id)
                it.remove();
        }
    }


	public Action removeSongs(int position, int size) {
		return removeSongs(position, size, true);
	}

    Action removeSongs(int position, int size, boolean createAction) {
		Action action = null;
		List<Song> subList = mSongs.subList(position, position + size);
		Song[] array = new Song[size];
		subList.toArray(array);
		if (createAction) {
			action = new Action(Action.Type.Removed, this, array, position, size, -1);
		}
		subList.clear();

		notifyObservers(new ObserverData(ObserverData.Type.Removed, this, action, array, position, size, -1));
		return action;
	}


	public void startMove(int position) {
		mOldPosition = position;
	}

	public Action endMove() {
		Action action = null;

		if (mOldPosition != -1 && mOldPosition != mNewPosition) {
			action = new Action(Action.Type.Moved, this, null, mNewPosition, 1, mOldPosition);
			notifyObservers(new ObserverData(ObserverData.Type.Moved, this, action, null, mNewPosition, 1, mNewPosition));
		}

		mOldPosition = -1;
		mNewPosition = -1;

		return action;
	}

	public Action moveSong(int oldPosition, int newPosition) {
		return moveSong(oldPosition, newPosition, mOldPosition == -1);
	}

    Action moveSong(int oldPosition, int newPosition, boolean createAction) {
		Action action = null;
		Song song = mSongs.remove(oldPosition);
		mSongs.add(newPosition, song);

		Song[] items = new Song[] { song };

		if (createAction)
			action = new Action(Action.Type.Moved, this, items, newPosition, 1, oldPosition);

		if (mOldPosition != -1)
			mNewPosition = newPosition;

		notifyObservers(new ObserverData(ObserverData.Type.Moved, this, action, items, newPosition, 1, oldPosition));
		return action;
	}


	public int getSongPosition(Song song) {
		for (int i = 0; i < mSongs.size(); i++) {
			if (mSongs.get(i).getId() == song.getId())
				return i;
		}

		return -1;
	}


	public synchronized void load() {
        MusicLibrary lib = MusicLibrary.getInstance();

        mSongs.clear();

        DataInputStream stream = null;

        try {
            stream = new DataInputStream(new FileInputStream(lib.getPlaylistFile(mId)));

            mName = stream.readUTF();

            int songsSize = stream.readInt();

            for (int i = 0; i < songsSize; i++) {
                Song song = lib.getSongById(stream.readInt());
                if (song != null)
                    mSongs.add(song);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mLoaded = true;
    }
	
	public synchronized void save() {
		if (!mLoaded)
			throw new IllegalStateException("Cannot save non-loaded playlist");

		if (mDeleted)
			return;

		MusicLibrary lib = MusicLibrary.getInstance();
		DataOutputStream stream = null;

		try {
			stream = new DataOutputStream(new FileOutputStream(lib.getPlaylistFile(mId)));

			stream.writeUTF(getName());
			stream.writeInt(mSongs.size());

			for (int i = 0; i < mSongs.size(); i++)
				stream.writeInt(mSongs.get(i).getId());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (stream != null)
					stream.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void delete() {
		MusicLibrary lib = MusicLibrary.getInstance();
		File file = lib.getPlaylistFile(mId);

		file.delete();
		mDeleted = true;

		notifyObservers(new ObserverData(ObserverData.Type.Deleted, this));
	}

	public void invalidate() {
        mLoaded = false;

        notifyObservers(new ObserverData(ObserverData.Type.Invalidated, this));
    }

    void notifyChanged() {
        notifyObservers(new ObserverData(ObserverData.Type.Changed, this));
    }

	public int getValue() {
		return 0;
	}

	public void setValue(int value) {

	}

	boolean isEnabled() {
		return mEnabled;
	}

	void setEnabled(boolean enabled) {
		mEnabled = enabled;
	}

	public int getEditingTitleRes() {
		return 0;
	}
}
