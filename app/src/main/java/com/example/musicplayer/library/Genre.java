package com.example.musicplayer.library;

import com.example.musicplayer.Util;

import java.util.Comparator;

public class Genre implements LibraryObject {

	public static class GenreComparator implements Comparator<Genre> {

		private Sorting sorting;
		private boolean reversed;

		public GenreComparator(Sorting sorting, boolean reversed) {
			this.sorting = sorting;
			this.reversed = reversed;
		}

		@Override
		public int compare(Genre lhs, Genre rhs) {
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

		private int nameCompare(Genre lhs, Genre rhs) {
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

        public Genre build() {
            Genre genre = new Genre();

            genre.mId = mId;
            genre.mName = mName;
            return genre;
        }
	}
	
	private int mId;
	private String mName;

    private Genre() {
        mId = 0;
        mName = null;
    }

    Genre(int id, String name) {
		mId = id;
		mName = name;
	}
	
	@Override
	public int getType() {
		return GENRE;
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

	public static String getName(Genre genre) {
		String name = null;

		if (genre != null)
			name = genre.getName();

		return MusicLibrary.getInstance().getGenreString(name);
	}
}
