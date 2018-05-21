package com.example.musicplayer.library;

import com.example.musicplayer.Util;

import java.util.Comparator;

public class Artist implements LibraryObject {

	public static class ArtistComparator implements Comparator<Artist> {

		private Sorting sorting;
		private boolean reversed;

		public ArtistComparator(Sorting sorting, boolean reversed) {
			this.sorting = sorting;
			this.reversed = reversed;
		}

		@Override
		public int compare(Artist lhs, Artist rhs) {
			int res = 0;

			switch (sorting) {
				case ID:
					return Util.longCompare(lhs.getId(), rhs.getId());
				case Name:
					res = nameCompare(lhs, rhs);
					break;
			}

			if (res == 0)
				res = Util.longCompare(lhs.getId(), rhs.getId());

			return res * (reversed ? -1 : 1);
		}

		private int nameCompare(Artist lhs, Artist rhs) {
			return Util.stringCompare(getName(lhs), getName(rhs));
		}
	}

    public static class Builder {
        private int mId;
        private String mName;

        public Builder() {
            mId = 0;
            mName = null;
        }

        public Builder setId(int id) {
            mId = id;
            return this;
        }

        public Builder setName(String name) {
			if (Util.stringIsEmpty(name))
				mName = null;
			else
            	mName = name;
            return this;
        }

        public Artist build() {
            Artist artist = new Artist();

            artist.mId = mId;
            artist.mName = mName;
            return artist;
        }
    }

	private int 	mId;
	private String 	mName;

    private Artist() {
        mId = 0;
        mName = null;
    }

	Artist(int id, String name) {
		mId = id;
		mName = name;
	}
	
	@Override
	public int getType() {
		return ARTIST;
	}
	
	public int getId() {
		return mId;
	}
	
	public String getName() {
		return mName;
	}
	
	@Override
	public String toString() {
		return getName(this);
	}

	public static String getName(Artist artist) {
		String name = null;

		if (artist != null)
			name = artist.getName();

		return MusicLibrary.getInstance().getArtistString(name);
	}
}
