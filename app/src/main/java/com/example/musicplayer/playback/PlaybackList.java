package com.example.musicplayer.playback;

import com.example.musicplayer.IObserver;
import com.example.musicplayer.Observable;
import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.library.Sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public class PlaybackList extends Observable<PlaybackList.ObserverData> {

    public interface Observer extends IObserver<ObserverData> { }

	public static class ObserverData {
		public enum Type {
			SongsAdded, SongsRemoved, SongMoved, ListChanged
		}

		public final Type type;
        public final PlaybackList playbackList;
		public final int position;
		public final int size;
        public final int oldQueueSize;
		public final int oldPosition;

		public ObserverData(Type type, PlaybackList list, int position, int size, int oldQueueSize, int oldPosition) {
			this.type = type;
            this.playbackList = list;
			this.position = position;
			this.size = size;
            this.oldQueueSize = oldQueueSize;
			this.oldPosition = oldPosition;
		}
	}
	
	private LinkedList<Song> mHistory;
	private LinkedList<Song> mQueue;
	private LinkedList<Song> mNext;
	private ArrayList<Song> mRemaining; // shuffle
	
	private LibraryObject mPlayedObject;
	private Sorting mSorting;
	private boolean mSortingReversed;
	private boolean mIsShuffled;

	public PlaybackList() {
		this(null, Sorting.ID, false);
	}

	public PlaybackList(LibraryObject object, Sorting sorting, boolean reversed) {
		mHistory = new LinkedList<>();
		mNext = new LinkedList<>();
		mRemaining = null;
		mPlayedObject = object;
		mSorting = sorting;
		mSortingReversed = reversed;
	}

	public void addToQueueFirst(Song song) {
		if (mQueue == null)
			mQueue = new LinkedList<>();

		mQueue.addFirst(song);

        notifyObservers(new ObserverData(ObserverData.Type.SongsAdded, this, 0, 1, mQueue.size() - 1, -1));
	}

	public void addAllToQueueFirst(Collection<Song> songs) {
		if (mQueue == null)
			mQueue = new LinkedList<>();

		mQueue.addAll(0, songs);

        notifyObservers(new ObserverData(ObserverData.Type.SongsAdded, this, 0, songs.size(), mQueue.size() - songs.size(), -1));
	}

	public void addToQueueLast(Song song, boolean notify) {
		if (mQueue == null)
			mQueue = new LinkedList<>();

		mQueue.addLast(song);

        if (notify) {
            notifyObservers(new ObserverData(ObserverData.Type.SongsAdded, this, mQueue.size() - 1, 1, mQueue.size() - 1, -1));
        }
    }

	public void addAllToQueueLast(Collection<Song> songs) {
		if (mQueue == null)
			mQueue = new LinkedList<>();

		mQueue.addAll(songs);

        notifyObservers(new ObserverData(ObserverData.Type.SongsAdded, this, mQueue.size() - songs.size(), songs.size(), mQueue.size() - songs.size(), -1));
	}

	public void addToHistoryFirst(Song song) {
		mHistory.addFirst(song);
	}
	
	public void addAllToHistoryFirst(Collection<Song> songs) {
		mHistory.addAll(0, songs);
	}

	public void addToHistoryLast(Song song) {
		mHistory.addLast(song);
	}

	public void addAllToHistoryLast(Collection<Song> songs) {
		mHistory.addAll(songs);
	}
	
	public void addToNextSongsFirst(Song song) {
        if (mQueue != null && mQueue.size() > 0) {
            addToQueueFirst(song);
        }
        else {
            mNext.addFirst(song);
            notifyObservers(new ObserverData(ObserverData.Type.SongsAdded, this, 0, 1, 0, -1));
        }
	}

	public void addAllToNextSongsFirst(Collection<Song> songs) {
        if (mQueue != null && mQueue.size() > 0) {
            addAllToQueueFirst(songs);
        } else {
            mNext.addAll(0, songs);
            notifyObservers(new ObserverData(ObserverData.Type.SongsAdded, this, 0, songs.size(), 0, -1));
        }
    }

	public void addToNextSongsLast(Song song) {
		mNext.add(song);
	}
	
	public void addAllToNextSongsLast(Collection<Song> songs) {
		mNext.addAll(songs);
	}
	
	public void setRemaining(Song[] songs) {
        setRemaining(Arrays.asList(songs));
	}
	
	public void setRemaining(Collection<Song> songs) {
		if (mRemaining == null) {
            mRemaining = new ArrayList<>(songs);
        }
        else {
            mRemaining.clear();
            mRemaining.addAll(songs);
        }
	}

    public Song previousSong() {
        if (mHistory != null)
            return mHistory.pollLast();

        return null;
    }

    public boolean hasPreviousSong() {
        return mHistory != null && !mHistory.isEmpty();
    }

    public Song nextSong() {
        Song next = null;

        int oldQueueSize = 0;

        if (mQueue != null) {
            oldQueueSize = mQueue.size();
            next = mQueue.pollFirst();
        }

        if (next == null && mNext != null)
            next = mNext.pollFirst();

        if (next == null && mRemaining != null && !mRemaining.isEmpty()) {
            Random rand = new Random();
            int nextPos = rand.nextInt(mRemaining.size());
            next = mRemaining.remove(nextPos);
        }

        notifyObservers(new ObserverData(ObserverData.Type.SongsRemoved, this, 0, 1, oldQueueSize, -1));

        return next;
    }

    public boolean hasNextSong() {
        return (mNext != null && !mNext.isEmpty()) || (mRemaining != null && !mRemaining.isEmpty());
    }

    public void removeSongs(Collection<Song> songs) {
        if (mQueue != null)
            mQueue.removeAll(songs);

        mNext.removeAll(songs);
        mHistory.removeAll(songs);

        if (mRemaining != null)
            mRemaining.removeAll(songs);
    }

    public void skipSongs(int numSongs) {
        if (numSongs <= 0)
            return;

        int temp = numSongs;
        int oldQueueSize = 0;

        if (mQueue != null && mQueue.size() > 0) {
            oldQueueSize = mQueue.size();
            int skips = Math.min(numSongs, mQueue.size());
            List<Song> subList = mQueue.subList(0, skips);
            addAllToHistoryLast(subList);
            subList.clear();
            numSongs -= skips;
        }

        if (numSongs > 0) {
            List<Song> subList = mNext.subList(0, numSongs);
            addAllToHistoryLast(subList);
            subList.clear();
        }

        notifyObservers(new ObserverData(ObserverData.Type.SongsRemoved, this, 0, temp, oldQueueSize, -1));
    }

    public void repeat() {
        MusicLibrary lib = MusicLibrary.getInstance();

        Sorting sorting = isShuffled() ? Sorting.ID : mSorting;

        Collection<Song> songs = lib.getSongsForObject(mPlayedObject, null, sorting, mSortingReversed);

        if (songs == null)
            return;

        if (isShuffled()) {
            setRemaining(songs);
        } else {
            mNext.addAll(songs);
        }

        notifyObservers(new ObserverData(ObserverData.Type.ListChanged, this, 0, 0, 0, -1));
    }

    public void setIsShuffled(Song song, boolean isShuffled) {
        MusicLibrary lib = MusicLibrary.getInstance();
        mIsShuffled = isShuffled;

        if (mPlayedObject == null || mPlayedObject.getType() != LibraryObject.PLAYLIST)
            updateFromObject(song, false);
        else {
            List<Song> songList = lib.getSongsForPlaylist((Playlist)mPlayedObject, mSorting, mSortingReversed);
            int index = songList.indexOf(song);
            updateFromPlaylist(songList, index, false);
        }

        notifyObservers(new ObserverData(ObserverData.Type.ListChanged, this, 0, 0, 0, -1));
    }

    public void transferQueueTo(PlaybackList list) {
        list.mQueue = mQueue;
    }

	public LinkedList<Song> getHistory() {
		return mHistory;
	}

	public LinkedList<Song> getQueue() {
		return mQueue;
	}

	public LinkedList<Song> getNextSongsList() {
		if (mRemaining != null) {
			Random rand = new Random();
		
			while (!mRemaining.isEmpty()) {
				int nextPos = rand.nextInt(mRemaining.size());
				addToNextSongsLast(mRemaining.remove(nextPos));
			}
		}

		return mNext;
	}

    public boolean isQueueEmpty() {
        return mQueue == null || mQueue.isEmpty();
    }

	public LibraryObject getPlayedObject() {
		return mPlayedObject;
	}

	public Sorting getSorting() {
		return mSorting;
	}

	public boolean isSortingReversed() {
		return mSortingReversed;
	}

	public boolean isShuffled() {
		return mIsShuffled;
	}


    private void updateFromObject(Song song, boolean includeSong) {
        mNext.clear();
        mHistory.clear();

        Sorting sorting = mIsShuffled ? Sorting.ID : mSorting;
        TreeSet<Song> songs = (TreeSet<Song>)MusicLibrary.getInstance().getSongsForObject(
                mPlayedObject, null, sorting, mSortingReversed);

        if (mIsShuffled) {
            if (song != null)
                songs.remove(song);

            setRemaining(songs);
            if (includeSong && song != null)
                addToNextSongsFirst(song);
        }
        else {
            if (song == null)
                mNext.addAll(songs);
            else {
                mHistory.addAll(songs.headSet(song, false));
                mNext.addAll(songs.tailSet(song, false));
                if (includeSong)
                    addToNextSongsFirst(song);
            }
        }
    }

    private void updateFromPlaylist(List<Song> songList, int songPosition, boolean includeSong) {
        mNext.clear();
        mHistory.clear();

        if (mIsShuffled) {
            setRemaining(songList);

            if (songPosition >= 0) {
                Song song = mRemaining.remove(songPosition);

                if (includeSong)
                    addToNextSongsFirst(song);
            }
        }
        else {
            if (songPosition < 0)
                mNext.addAll(songList);
            else {
                mHistory.addAll(songList.subList(0, songPosition));
                mNext.addAll(songList.subList(songPosition + 1, songList.size()));
                if (includeSong)
                    addToNextSongsFirst(songList.get(songPosition));
            }
        }
    }


	private static PlaybackList fromPlaylistInternal(Playlist playlist, List<Song> songList, Sorting sorting, boolean reversed, boolean shuffle, int songPosition) {
		PlaybackList list = new PlaybackList();
		list.mSorting = sorting;
		list.mSortingReversed = reversed;
		list.mPlayedObject = playlist;
		list.mIsShuffled = shuffle;

        list.updateFromPlaylist(songList, songPosition, false);

		return list;
	}

	public static PlaybackList fromObject(int objectType, int objectId, Sorting sorting, boolean reversed, boolean shuffle, int songId) {
		switch (objectType) {
			case LibraryObject.PLAYLIST: {
                MusicLibrary lib = MusicLibrary.getInstance();
                Playlist playlist = lib.getPlaylistById(objectId);
                List<Song> songList = lib.getSongsForPlaylist(playlist, sorting, reversed);
                return fromPlaylistInternal(playlist, songList, sorting, reversed, shuffle, songId);
            }
			default: {
                Song song = MusicLibrary.getInstance().getSongById(songId);

                return fromObject(objectType, objectId, sorting, reversed, shuffle, song);
			}
		}
	}

    public static PlaybackList fromObject(int objectType, int objectId, Sorting sorting, boolean reversed, boolean shuffle, Song song) {
        MusicLibrary lib = MusicLibrary.getInstance();

        switch (objectType) {
            case LibraryObject.PLAYLIST: {
                Playlist playlist = lib.getPlaylistById(objectId);
                List<Song> songList = lib.getSongsForPlaylist(playlist, sorting, reversed);
                int songPosition = songList.indexOf(song);
                return fromPlaylistInternal(playlist, songList, sorting, reversed, shuffle, songPosition);
            }
            default: {
                LibraryObject object = lib.getLibraryObject(objectType, objectId);

                PlaybackList list = new PlaybackList();
                list.mPlayedObject = object;
                list.mIsShuffled = shuffle;
                list.mSorting = sorting;
				list.mSortingReversed = reversed;

                list.updateFromObject(song, false);

                return list;
            }
        }
    }
}
