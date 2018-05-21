package com.example.musicplayer.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Tarik on 07.08.2016.
 */
public abstract class PlaylistParser {

    private PlaylistFile mFile;
    private BufferedReader mReader;

    public void parse(PlaylistFile file, String filePath) {
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            mFile = file;

            int dirIndex = filePath.lastIndexOf('/');
            int extIndex = filePath.lastIndexOf('.');

            if (extIndex < dirIndex)
                mFile.setName(filePath.substring(dirIndex + 1));
            else
                mFile.setName(filePath.substring(dirIndex + 1, extIndex));

            onParse();

            mReader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected PlaylistFile getFile() {
        return mFile;
    }

    protected BufferedReader getReader() {
        return mReader;
    }

    protected abstract void onParse() throws IOException;

    protected void setEntryFilePath(PlaylistFile.Entry entry, String filePath) {
        char firstChar = filePath.charAt(0);

        if (firstChar == '/') {
            entry.relativePath = false;
            entry.pathHasBackslashes = false;
        }
        else if (((firstChar >= 'A' && firstChar <= 'Z') || (firstChar >= 'a' && firstChar <= 'z')) &&
                filePath.charAt(1) == ':') {
            entry.relativePath = false;
            entry.pathHasBackslashes = filePath.charAt(2) == '\\';
        }
        else {
            entry.relativePath = true;
            entry.pathHasBackslashes = filePath.indexOf('\\') != -1;
        }

        entry.filePath = filePath;
    }

    protected PlaylistFile.Entry makeEntry(String filePath, String title, int length) {
        PlaylistFile.Entry entry = new PlaylistFile.Entry();

        setEntryFilePath(entry, filePath);
        entry.title = title;
        entry.length = length;

        return entry;
    }
}
