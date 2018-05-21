/*
 * Copyright ${YEAR} Tarik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.musicplayer.library;

import android.support.annotation.Nullable;

import com.example.musicplayer.Util;

import java.util.Comparator;

public class Album implements LibraryObject {

    static class AlbumComparator implements Comparator<Album> {

		private Sorting sorting;
		private boolean reversed;

		public AlbumComparator(Sorting sorting, boolean reversed) {
			this.sorting = sorting;
			this.reversed = reversed;
		}

		@Override
		public int compare(Album lhs, Album rhs) {
			int res = 0;

			switch (sorting) {
				case ID:
					return Util.longCompare(lhs.getId(), rhs.getId());
				case Title:
					res = titleCompare(lhs, rhs);

					if (res == 0)
						res = artistCompare(lhs, rhs);

					break;
                case TitleExclusive:
					return titleCompare(lhs, rhs);
                case Artist:
					res = artistCompare(lhs, rhs);

					if (res == 0)
						res = titleCompare(lhs, rhs);

					break;
			}

			if (res == 0)
				res = Util.longCompare(lhs.getId(), rhs.getId());

			return res * (reversed ? -1 : 1);
		}

		private int titleCompare(Album lhs, Album rhs) {
			return Util.stringCompare(getTitle(lhs), getTitle(rhs));
		}

		private int artistCompare(Album lhs, Album rhs) {
			return Util.stringCompare(getArtist(lhs), getArtist(rhs));
		}
	}

	public static class Builder {
        private int mId;
        private String mTitle;
        private int mArtistId;
        private String mArtist;
		private boolean mVariousArtists;
		private int mSongCount;

		public Builder() {

		}

		private void setNull() {
			mId = 0;
			mTitle = null;
			mArtist = null;
			mArtistId = 0;
			mVariousArtists = false;
			mSongCount = 0;
        }

		public Builder setId(int id) {
			mId = id;
            return this;
		}

		public Builder setTitle(String title) {
			if (Util.stringIsEmpty(title))
				mTitle = null;
			else
				mTitle = title;
            return this;
		}

		public Builder setArtistId(int id) {
			mArtistId = id;
            return this;
		}

		public Builder setArtistName(String name) {
			if (Util.stringIsEmpty(name))
				mArtist = null;
			else
				mArtist = name;
            return this;
		}

		public Builder setHasVariousArtists(boolean variousArtists) {
			mVariousArtists = variousArtists;
			return this;
		}

		public Builder setSongCount(int songCount) {
			mSongCount = songCount;
			return this;
		}

		public Album build() {
            Album album = new Album();

            album.mId = mId;
            album.mTitle = mTitle;
            album.mArtistId =  mArtistId;
            album.mArtist = mArtist;
			album.mVariousArtists = mVariousArtists;
			album.mSongCount = mSongCount;
			return album;
		}
	}
	
	private int 	mId;
	private String 	mTitle;
	private String 	mArtist;
	private int 	mArtistId;
	private boolean mVariousArtists;
	private int mSongCount;

	private Album() {
        this(0, 0, null, null);
	}

	Album(int id, int artistId, String artist, String title) {
		mId = id;
		mArtist = artist;
		mArtistId = artistId;
		mTitle = title;

        mVariousArtists = false;
        mSongCount = 0;
	}
	
	@Override
	public int getType() {
		return ALBUM;
	}
	
	public int getId() {
		return mId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getArtist() {
		return mArtist;
	}
	
	public int getArtistId() {
		return mArtistId;
	}

	public boolean hasVariousArtists() {
		return mVariousArtists;
	}

    public int getSongCount() {
        return mSongCount;
    }

    void setHasVariousArtists(boolean various) {
        mVariousArtists = various;
    }

    void setSongCount(int count) {
        mSongCount = count;
    }

	@Override
	public String toString() {
		return getTitle(this);
	}

	public static String getTitle(@Nullable Album album) {
		String title = null;

		if (album != null)
			title = album.getTitle();

		return MusicLibrary.getInstance().getAlbumString(title);
	}

	public static String getArtist(@Nullable Album album) {
		String artist = null;

		if (album != null)
			artist = album.getArtist();

		return MusicLibrary.getInstance().getArtistString(artist);
	}
}
