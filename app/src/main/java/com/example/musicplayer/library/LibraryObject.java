package com.example.musicplayer.library;

public interface LibraryObject {
	int ARTIST = 0;
	int ALBUM = 1;
	int SONG = 2;
	int GENRE = 3;
	int FOLDER = 4;
	int PLAYLIST = 5;
	int PRESET = 6;
	int UNKNOWN = -1;
	int NONE = -2;

	int getType();
	int getId();
}
