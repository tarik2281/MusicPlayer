package com.example.musicplayer.library;

import android.support.annotation.NonNull;

import com.example.musicplayer.io.Metadata;
import com.example.musicplayer.Util;

import java.util.Comparator;

public class Song implements LibraryObject, Comparable<Song> {

	public static class SongComparator implements Comparator<Song> {

		private Sorting sorting;
		private boolean reversed;

		public SongComparator(Sorting sorting, boolean reversed) {
			this.sorting = sorting;
			this.reversed = reversed;
		}

		@Override
		public int compare(Song lhs, Song rhs) {
			int res = 0;

			switch (sorting) {
				case ID:
					return Util.longCompare(lhs.getId(), rhs.getId());
				case Title: {
					res = titleCompare(lhs, rhs);

					if (res == 0) {
						res = artistCompare(lhs, rhs);

						if (res == 0)
							res = albumCompare(lhs, rhs);
					}

					break;
				}
				case Artist: {
					res = artistCompare(lhs, rhs);

					if (res == 0) {
						res = titleCompare(lhs, rhs);

						if (res == 0)
							res = albumCompare(lhs, rhs);
					}

					break;
				}
				case Album: {
					res = albumCompare(lhs, rhs);

					if (res == 0) {
						res = artistCompare(lhs, rhs);

						if (res == 0) {
							res = discNumberCompare(lhs, rhs);

							if (res == 0) {
								res = titleNumberCompare(lhs, rhs);

								if (res == 0)
									res = titleCompare(lhs, rhs);
							}
						}
					}

					break;
				}
				case Number: {
					res = discNumberCompare(lhs, rhs);

					if (res == 0) {
						res = titleNumberCompare(lhs, rhs);

						if (res == 0)
							res = titleCompare(lhs, rhs);
					}

					break;
				}
				case Duration: {
					res = Util.longCompare(lhs.getInfo().getDuration(), rhs.getInfo().getDuration());

					if (res == 0) {
						res = titleCompare(lhs, rhs);

						if (res == 0) {
							res = artistCompare(lhs, rhs);

							if (res == 0)
								res = albumCompare(lhs, rhs);
						}
					}

					break;
				}
				case FileName: {
					res = Util.stringCompare(lhs.getInfo().getFileName(), rhs.getInfo().getFileName());

					if (res == 0) {
						res = artistCompare(lhs, rhs);

						if (res == 0)
							res = albumCompare(lhs, rhs);
					}

					break;
				}
			}

			if (res == 0)
				res = Util.longCompare(lhs.getId(), rhs.getId());

			return res * (reversed ? -1 : 1);
		}

		private int titleCompare(Song lhs, Song rhs) {
			return Util.stringCompare(lhs.getInfo().getTitle(), rhs.getInfo().getTitle());
		}

		private int artistCompare(Song lhs, Song rhs) {
			return Util.stringCompare(getArtist(lhs), getArtist(rhs));
		}

		private int albumCompare(Song lhs, Song rhs) {
			return Util.stringCompare(getAlbum(lhs), getAlbum(rhs));
		}

		private int genreCompare(Song lhs, Song rhs) {
			return Util.stringCompare(getGenre(lhs), getGenre(rhs));
		}

		private int discNumberCompare(Song lhs, Song rhs) {
			int discL = lhs.getInfo().getDiscNumber();
			int discR = rhs.getInfo().getDiscNumber();

			if (discL < 1)
				discL = 1;
			if (discR < 1)
				discR = 1;

			return Util.intCompare(discL, discR);
		}

		private int titleNumberCompare(Song lhs, Song rhs) {
			return Util.intCompare(lhs.getInfo().getTitleNumber(), rhs.getInfo().getTitleNumber());
		}
	}

	public static class Builder {
		private int 	mId;
		private Info mInfo;
        private boolean mIsHidden;

		public Builder() {
			mInfo = new Info();
		}

		public Builder setId(int id) {
			mId = id;
            return this;
		}

		public Builder setTitle(String title) {
			mInfo.mTitle = title;
            return this;
		}

		public Builder setArtistId(int id) {
			mInfo.mArtistId = id;
			return this;
		}

		public Builder setArtist(String name) {
			mInfo.mArtist = Util.stringIsEmpty(name) ? null : name;
            return this;
		}

		public Builder setAlbumArtist(String name) {
			mInfo.mAlbumArtist = Util.stringIsEmpty(name) ? null : name;
			return this;
		}

		public Builder setAlbum(int id, String title) {
			mInfo.mAlbumId = id;
			mInfo.mAlbum = title;
            return this;
		}

		public Builder setGenre(int id, String name) {
			mInfo.mGenre = name;
			mInfo.mGenreId = id; // == 0 ? Genre.makeId(name) : id;
            return this;
		}

		public Builder setDuration(long duration) {
			mInfo.mDuration = duration;
            return this;
		}

		public Builder setTitleNumber(int titleNumber, int numTitles) {
			mInfo.mTitleNumber = titleNumber;
			mInfo.mNumTitles = numTitles;
            return this;
		}

		public Builder setDiscNumber(int discNumber, int numDiscs) {
			mInfo.mDiscNumber = discNumber;
			mInfo.mNumDiscs = numDiscs;
            return this;
		}

		public Builder setYear(int year) {
			mInfo.mYear = year;
			return this;
		}

		public Builder setFolderId(int folderId) {
			mInfo.mFolderId = folderId;
			return this;
		}

		public Builder setFilePath(String filePath) {
			mInfo.mFilePath = filePath;
            return this;
		}

        public Builder setLastModified(long lastModified) {
            mInfo.mLastModified = lastModified;
            return this;
        }

		public Builder setIsHidden(boolean hidden) {
			mIsHidden = hidden;
			return this;
		}

        public Song build() {
            Song song = new Song();
			Info info = new Info();

            song.mId = mId;
            info.mTitle = mInfo.mTitle;
            info.mArtistId = mInfo.mArtistId;
            info.mArtist = mInfo.mArtist;
            info.mAlbumArtist = mInfo.mAlbumArtist;
            info.mAlbumId = mInfo.mAlbumId;
            info.mAlbum = mInfo.mAlbum;
            info.mGenreId = mInfo.mGenreId;
            info.mGenre = mInfo.mGenre;
            info.mDuration = mInfo.mDuration;
            info.mTitleNumber = mInfo.mTitleNumber;
            info.mNumTitles = mInfo.mNumTitles;
            info.mDiscNumber = mInfo.mDiscNumber;
            info.mNumDiscs = mInfo.mNumDiscs;
			info.mYear = mInfo.mYear;
			info.mFolderId = mInfo.mFolderId;
            info.mFilePath = mInfo.mFilePath;
            info.mLastModified = mInfo.mLastModified;
			song.mIsHidden = mIsHidden;

            song.mInfo = info;

            return song;
        }
	}

	public static class Info {
		private String mTitle;
		private int mArtistId;
		private String mArtist;
		private String mAlbumArtist;
		private int mAlbumId;
		private String mAlbum;
		private int mGenreId;
		private String mGenre;
		private long mDuration;
		private int mTitleNumber;
		private int mNumTitles;
		private int mDiscNumber;
		private int mNumDiscs;
		private int mYear;
		private int mFolderId;
		private String mFilePath;
		private long mLastModified;

		private Info() {

		}

		Info(int artistId, int albumId, int genreId, int folderId,
                    Metadata metadata, String filePath, long lastModified) {
			if (metadata.getTitle() == null)
				mTitle = Util.getFileName(filePath);
			else
				mTitle = metadata.getTitle();
			mArtistId = artistId;
			mArtist = metadata.getArtist();
			mAlbumArtist = metadata.getAlbumArtist();
			mAlbumId = albumId;
			mAlbum = metadata.getAlbum();
			mGenreId = genreId;
			mGenre = metadata.getGenre();
			mDuration = metadata.getDuration();
			mTitleNumber = metadata.getTitleNumber();
			mNumTitles = metadata.getNumTitles();
			mDiscNumber = metadata.getDiscNumber();
			mNumDiscs = metadata.getNumDiscs();
			mYear = metadata.getYear();
			mFolderId = folderId;
			mFilePath = filePath;
			mLastModified = lastModified;
		}

		void updateIds(int artistId, int albumId, int genreId) {
            mArtistId = artistId;
            mAlbumId = albumId;
            mGenreId = genreId;
        }

		void setUpdate(int artistId, int albumId, int genreId, Metadata metadata, long lastModified) {
			if (metadata.getTitle() == null)
				mTitle = Util.getFileName(mFilePath);
			else
				mTitle = metadata.getTitle();
			mArtistId = artistId;
			mArtist = metadata.getArtist();
			mAlbumArtist = metadata.getAlbumArtist();
			mAlbumId = albumId;
			mAlbum = metadata.getAlbum();
			mGenreId = genreId;
			mGenre = metadata.getGenre();
			mDuration = metadata.getDuration();
			mTitleNumber = metadata.getTitleNumber();
			mNumTitles = metadata.getNumTitles();
			mDiscNumber = metadata.getDiscNumber();
			mNumDiscs = metadata.getNumDiscs();
			mYear = metadata.getYear();
			mLastModified = lastModified;
		}

		void setEdit(int artistId, int albumId, int genreId, Metadata metadata) {
			if (metadata.getTitle() != null) {
				if (Util.stringIsEmpty(metadata.getTitle()))
					mTitle = Util.getFileName(mFilePath);
				else
					mTitle = metadata.getTitle();
			}

			if (artistId != -1)
				mArtistId = artistId;

			if (albumId != -1)
				mAlbumId = albumId;

			if (genreId != -1)
				mGenreId = genreId;

			if (metadata.getArtist() != null) {
				if (Util.stringIsEmpty(metadata.getArtist()))
					mArtist = null;
				else
					mArtist = metadata.getArtist();
			}

			if (metadata.getAlbumArtist() != null) {
				if (Util.stringIsEmpty(metadata.getAlbumArtist()))
					mAlbumArtist = null;
				else
					mAlbumArtist = metadata.getAlbumArtist();
			}

			if (metadata.getAlbum() != null) {
				if (Util.stringIsEmpty(metadata.getAlbum()))
					mAlbum = null;
				else
					mAlbum = metadata.getAlbum();
			}

			if (metadata.getGenre() != null) {
				if (Util.stringIsEmpty(metadata.getGenre()))
					mGenre = null;
				else
					mGenre = metadata.getGenre();
			}

			if (metadata.getTitleNumber() != 0)
				mTitleNumber = metadata.getTitleNumber();

			if (metadata.getNumTitles() != 0)
				mNumTitles = metadata.getNumTitles();

			if (metadata.getDiscNumber() != 0)
				mDiscNumber = metadata.getDiscNumber();

			if (metadata.getNumDiscs() != 0)
				mNumDiscs = metadata.getNumDiscs();

			if (metadata.getYear() != 0)
				mYear = metadata.getYear();
		}

		void setLastModified(long time) {
			mLastModified = time;
		}

		public String getTitle() {
			return mTitle;
		}

		public int getArtistId() {
			return mArtistId;
		}

		public String getArtist() {
			return mArtist;
		}

		public String getAlbumArtist() {
			return mAlbumArtist;
		}

		public int getAlbumId() {
			return mAlbumId;
		}

		public String getAlbum() {
			return mAlbum;
		}

		public int getGenreId() {
			return mGenreId;
		}

		public String getGenre() {
			return mGenre;
		}

		public long getDuration() {
			return mDuration;
		}

		public int getTitleNumber() {
			return mTitleNumber;
		}

		public int getNumTitles() {
			return mNumTitles;
		}

		public int getDiscNumber() {
			return mDiscNumber;
		}

		public int getNumDiscs() {
			return mNumDiscs;
		}

		public int getYear() {
			return mYear;
		}

		public int getFolderId() {
			return mFolderId;
		}

		public String getFilePath() {
			return mFilePath;
		}

		public long getLastModified() {
			return mLastModified;
		}

		public String getFileName() {
			return Util.getFileName(mFilePath);
		}

		public String getDefaultArtist() {
            return mArtist == null ? mAlbumArtist : mArtist;
        }

        public String getRelevantArtist() {
			return mAlbumArtist != null ? mAlbumArtist : mArtist;
		}
	}

	private int 	mId;
	private Info mInfo;
	private boolean mIsHidden;
	private int mPresetId;

    private Song() {
        this(0, null);
    }

    Song(int id, Info info) {
		mId = id;
		mInfo = info;
        mIsHidden = false;
        mPresetId = 0;
	}

	public Info getInfo() {
		return mInfo;
	}

	@Override
	public int getType() {
		return SONG;
	}
	
	public int getId() {
		return mId;
	}

	public boolean isHidden() {
		return mIsHidden;
	}

	public int getPresetId() {
		return mPresetId;
	}

	void setPresetId(int id) {
        mPresetId = id;
    }

	void setHidden(boolean hidden) {
        mIsHidden = hidden;
    }

    @Override
    public int compareTo(@NonNull Song another) {
        return Util.longCompare(getId(), another.getId());
    }

    @Override
	public String toString() {
		return mInfo.mTitle;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Song && ((Song) o).getId() == getId();
	}

	public static String getTitle(Song song) {
		return MusicLibrary.getInstance().getUseFileName() ? song.getInfo().getFileName() : song.getInfo().getTitle();
	}

	public static String getArtist(Song song) {
		String name = null;

		if (song != null)
			name = song.getInfo().getDefaultArtist();

		return MusicLibrary.getInstance().getArtistString(name);
	}

    public static String getArtistText(Song song) {
        String artist = null;
        String album = null;

        if (song != null) {
            artist = song.getInfo().getDefaultArtist();
            album = song.getInfo().getAlbum();
        }

		String text = MusicLibrary.getInstance().getArtistString(artist);
        if (album != null)
            text += " - " + album;

        return text;
    }

	public static String getAlbum(Song song) {
		String album = null;

		if (song != null)
			album = song.getInfo().getAlbum();

		return MusicLibrary.getInstance().getAlbumString(album);
	}

	public static String getGenre(Song song) {
		String genre = null;

		if (song != null)
			genre = song.getInfo().getGenre();

		return MusicLibrary.getInstance().getGenreString(genre);
	}
}
