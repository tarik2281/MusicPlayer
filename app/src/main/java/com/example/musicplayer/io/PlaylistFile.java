package com.example.musicplayer.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.musicplayer.Util;
import com.example.musicplayer.library.Song;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Tarik on 10.08.2016.
 */
public class PlaylistFile {

    public static class Entry {
        public boolean relativePath;
        public boolean pathHasBackslashes;
        public String filePath;
        public String title;
        public int length;
    }

    public enum Type {
        M3U, PLS, Unknown
    }

    private static final String M3U_EXTENDED_HEADER = "#EXTM3U";
    private static final String M3U_ITEM_PREFIX = "#EXTINF:";
    private static final String PLS_HEADER = "[playlist]";

    private boolean mIsValid;
    private String mName;
    private Type mType;
    private String mDirectoryPath;
    private List<Entry> mEntries;

    public PlaylistFile() {
        mIsValid = false;
        mName = null;
        mType = Type.Unknown;
        mEntries = new ArrayList<>();
    }

    public boolean isValid() {
        return mIsValid;
    }

    public String getName() {
        return mName;
    }

    public Type getType() {
        return mType;
    }

    public String getDirectoryPath() {
        return mDirectoryPath;
    }

    public void entriesFromSongs(Collection<Song> songs) {
        for (Song song : songs) {
            Song.Info info = song.getInfo();
            PlaylistFile.Entry entry = new PlaylistFile.Entry();

            if (!Util.stringIsEmpty(info.getArtist()))
                entry.title = info.getArtist() + " - " + info.getTitle();
            else
                entry.title = info.getTitle();

            entry.filePath = info.getFilePath();

            entry.length = (int)(info.getDuration() / 1000);
            mEntries.add(entry);
        }
    }

    // relative paths
    public void entriesFromSongs(Collection<Song> songs, String basePath) {
        for (Song song : songs) {
            Song.Info info = song.getInfo();
            PlaylistFile.Entry entry = new PlaylistFile.Entry();
            if (!Util.stringIsEmpty(info.getArtist()))
                entry.title = info.getArtist() + " - " + info.getTitle();
            else
                entry.title = info.getTitle();

            entry.filePath = Util.relativize(basePath, info.getFilePath());

            entry.length = (int)(info.getDuration() / 1000);
            mEntries.add(entry);
        }
    }

    @NonNull
    public List<Entry> getEntryList() {
        return mEntries;
    }

    public void setValid(boolean valid) {
        mIsValid = valid;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setType(Type type) {
        mType = type;
    }

    public void setDirectoryPath(String directoryPath) {
        mDirectoryPath = directoryPath;
    }

    public int saveToStream(OutputStream stream) {
        PrintStream out = new PrintStream(new BufferedOutputStream(stream));

        switch (mType) {
            case M3U: {
                out.println(M3U_EXTENDED_HEADER);

                for (Entry entry : mEntries) {
                    out.printf("%s%d,%s\n", M3U_ITEM_PREFIX, entry.length, entry.title);
                    out.println(entry.filePath);
                }

                break;
            }
            case PLS: {
                out.println(PLS_HEADER);

                int index = 0;
                for (Entry entry : mEntries) {
                    index++;

                    out.printf("File%d=%s\n", index, entry.filePath);
                    out.printf("Title%d=%s\n", index, entry.title);
                    out.printf("Length%d=%d\n", index, entry.length);
                }

                out.printf("NumberOfEntries=%d\n", mEntries.size());
                out.println("Version=2");

                break;
            }
        }

        out.flush();

        return 0;
    }

    public static boolean hasPlaylistExtension(String filePath) {
        String upper = filePath.toUpperCase();
        return upper.endsWith(".M3U") || upper.endsWith(".M3U8") || upper.endsWith(".PLS");
    }

    @Nullable
    public static PlaylistFile parseFile(String filePath) {
        PlaylistParser parser = getParser(filePath);
        if (parser == null)
            return null;

        PlaylistFile file = new PlaylistFile();

        int dirIndex = filePath.lastIndexOf('/');
        file.setDirectoryPath(filePath.substring(0, dirIndex));
        parser.parse(file, filePath);

        return file;
    }

    @Nullable
    private static PlaylistParser getParser(String filePath) {
        int index = filePath.lastIndexOf('.');
        String extension = filePath.substring(index + 1).toUpperCase();

        switch (extension) {
            case "M3U":
            case "M3U8":
                return new M3UParser();
            case "PLS":
                return new PLSParser();
        }

        return null;
    }

    public static String makePath(String path, String name, Type type) {
        StringBuilder pathBuilder = new StringBuilder(path);
        if (!path.endsWith("/"))
            pathBuilder.append('/');
        pathBuilder.append(name);

        String lower = name.toUpperCase();

        switch (type) {
            case M3U:
                if (!lower.endsWith(".M3U") && !lower.endsWith(".M3U8"))
                    pathBuilder.append(".m3u");
                break;
            case PLS:
                if (!lower.endsWith(".PLS"))
                    pathBuilder.append(".pls");
                break;
        }

        return pathBuilder.toString();
    }
}
