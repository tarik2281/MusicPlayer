package com.example.musicplayer.io;

import com.example.musicplayer.Util;

public class Metadata {
    public static class Builder {
        private Metadata mMetadata;
        private boolean mIsEditing;

        public Builder() {
            this(false);
        }

        public Builder(boolean editing) {
            this(null, editing);
        }

        public Builder(Metadata metadata, boolean editing) {
            mMetadata = metadata == null ? new Metadata() : metadata;
            mIsEditing = editing;
        }

        public Builder setTitle(String title) {
            mMetadata.mTitle = Util.stringIsEmpty(title) && !mIsEditing ? null : title;

            return this;
        }

        public Builder setArtist(String artist) {
            mMetadata.mArtist = Util.stringIsEmpty(artist) && !mIsEditing ? null : artist;

            return this;
        }

        public Builder setAlbumArtist(String albumArtist) {
            mMetadata.mAlbumArtist = Util.stringIsEmpty(albumArtist) && !mIsEditing ? null : albumArtist;

            return this;
        }

        public Builder setAlbum(String album) {
            mMetadata.mAlbum = Util.stringIsEmpty(album) && !mIsEditing ? null : album;

            return this;
        }

        public Builder setGenre(String genre) {
            mMetadata.mGenre = Util.stringIsEmpty(genre) && !mIsEditing ? null : genre;

            return this;
        }

        public Builder setTitleNumbers(String titleNumbers) {
            int[] numbers = Util.splitNumbersString(titleNumbers);
            mMetadata.mTitleNumber = numbers[0];
            mMetadata.mNumTitles = numbers[1];

            return this;
        }

        public Builder setTitleNumber(int titleNumber) {
            mMetadata.mTitleNumber = titleNumber;

            return this;
        }

        public Builder setNumTitles(int numTitles) {
            mMetadata.mNumTitles = numTitles;

            return this;
        }

        public Builder setDiscNumbers(String discNumbers) {
            int[] numbers = Util.splitNumbersString(discNumbers);
            mMetadata.mDiscNumber = numbers[0];
            mMetadata.mNumDiscs = numbers[1];

            return this;
        }

        public Builder setDiscNumber(int discNumber) {
            mMetadata.mDiscNumber = discNumber;

            return this;
        }

        public Builder setNumDiscs(int numDiscs) {
            mMetadata.mNumDiscs = numDiscs;

            return this;
        }

        public Builder setYear(String year) {
            try {
                mMetadata.mYear = Integer.parseInt(year);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                mMetadata.mYear = -1;
            }

            return this;
        }

        public Builder setYear(int year) {
            mMetadata.mYear = year;
            return this;
        }

        public Builder setDuration(long duration) {
            mMetadata.mDuration = duration;

            return this;
        }

        public Builder fromMediaTag(MediaTag tag) {
            setTitle(tag.getMetadata(MediaTag.Metadata.Title));
            setArtist(tag.getMetadata(MediaTag.Metadata.Artist));
            setAlbumArtist(tag.getMetadata(MediaTag.Metadata.AlbumArtist));
            setAlbum(tag.getMetadata(MediaTag.Metadata.Album));
            setGenre(tag.getMetadata(MediaTag.Metadata.Genre));
            setTitleNumbers(tag.getMetadata(MediaTag.Metadata.TrackNumber));
            setDiscNumbers(tag.getMetadata(MediaTag.Metadata.DiscNumber));
            setYear(tag.getMetadata(MediaTag.Metadata.Year));
            setDuration(tag.getProperty(MediaTag.Properties.Duration));
            return this;
        }

        public Metadata build() {
            return mMetadata;
        }
    }


    // Editing: null if not changed, empty string to delete
	private String mTitle;
    private String mArtist;
    private String mAlbumArtist;
    private String mAlbum;
    private String mGenre;
    // Editing: 0 if not changed, -1 to delete
    private int mTitleNumber;
    private int mNumTitles;
    private int mDiscNumber;
    private int mNumDiscs;
    private int mYear;
    private long mDuration;

    private Metadata() {
		
	}

	public String getTitle() { return mTitle; }
    public String getArtist() { return mArtist; }
    public String getAlbumArtist() { return mAlbumArtist; }
    public String getAlbum() { return mAlbum; }
    public String getGenre() { return mGenre; }
    public int getTitleNumber() { return mTitleNumber; }
    public int getNumTitles() { return mNumTitles; }
    public int getDiscNumber() { return mDiscNumber; }
    public int getNumDiscs() { return mNumDiscs; }
    public int getYear() { return mYear; }
    public long getDuration() { return mDuration; }

    public String getRelevantArtist() {
        return mAlbumArtist != null ? mAlbumArtist : mArtist;
    }
}
