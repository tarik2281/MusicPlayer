package com.example.musicplayer.backup;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;


/**
 * Created by 19tar on 04.10.2017.
 */

public class BackupReader {

    private BackupFile mFile;
    private XmlPullParser mParser;

    public boolean readFromStream(BackupFile file, InputStream stream) {
        boolean result = false;

        mFile = file;

        GZIPInputStream inStream = null;

        try {
            inStream = new GZIPInputStream(stream);

            mParser = Xml.newPullParser();
            mParser.setInput(inStream, "utf-8");

            while (readUntilExitTag(BackupFile.TAG_ROOT)) {
                switch (mParser.getEventType()) {
                    case XmlPullParser.START_TAG: {
                        switch (mParser.getName()) {
                            case BackupFile.TAG_PREFS:
                                readPreferences();
                                break;
                            case BackupFile.TAG_STATS:
                                readSongStats();
                                break;
                            case BackupFile.TAG_PRESETS:
                                readPresets();
                                break;
                            case BackupFile.TAG_PLAYLISTS:
                                readPlaylists();
                                break;
                        }
                        break;
                    }
                }
            }

            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null)
                    inStream.close();
                else
                    stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private void readPreferences() throws IOException, XmlPullParserException {
        String type = null;
        String key = null;
        String value = null;

        while (readUntilExitTag(BackupFile.TAG_PREFS)) {
            switch (mParser.getEventType()) {
                case XmlPullParser.START_TAG: {
                    switch (mParser.getName()) {
                        case BackupFile.TAG_ITEM:
                            type = null;
                            key = null;
                            value = null;
                            break;
                        case BackupFile.TAG_TYPE:
                            type = mParser.nextText();
                            break;
                        case BackupFile.TAG_KEY:
                            key = mParser.nextText();
                            break;
                        case BackupFile.TAG_VALUE:
                            value = mParser.nextText();
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    if (mParser.getName().equals(BackupFile.TAG_ITEM))
                        mFile.addPreference(type, key, value);
                    break;
                }
            }
        }
    }

    private void readSongStats() throws IOException, XmlPullParserException {
        String songPath = null;
        long dateAdded = 0;
        int presetId = 0;
        int playCount = 0;
        long lastPlayed = 0;
        boolean hidden = false;

        while (readUntilExitTag(BackupFile.TAG_STATS)) {
            switch (mParser.getEventType()) {
                case XmlPullParser.START_TAG: {
                    switch (mParser.getName()) {
                        case BackupFile.TAG_ITEM:
                            songPath = null;
                            dateAdded = 0;
                            presetId = 0;
                            playCount = 0;
                            lastPlayed = 0;
                            break;
                        case BackupFile.TAG_SONG_PATH:
                            songPath = mParser.nextText();
                            break;
                        case BackupFile.TAG_DATE_ADDED:
                            dateAdded = Long.parseLong(mParser.nextText());
                            break;
                        case BackupFile.TAG_PRESET_ID:
                            presetId = Integer.parseInt(mParser.nextText());
                            break;
                        case BackupFile.TAG_PLAY_COUNT:
                            playCount = Integer.parseInt(mParser.nextText());
                            break;
                        case BackupFile.TAG_LAST_PLAYED:
                            lastPlayed = Long.parseLong(mParser.nextText());
                            break;
                        case BackupFile.TAG_IS_HIDDEN:
                            hidden = Boolean.parseBoolean(mParser.nextText());
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    if (mParser.getName().equals(BackupFile.TAG_ITEM))
                        mFile.addSongStat(songPath, dateAdded, presetId, playCount, lastPlayed, hidden);
                    break;
                }
            }
        }
    }

    private void readPresets() throws IOException, XmlPullParserException {
        int presetId = 0;
        String name = null;
        String bandGains = null;
        boolean prebuilt = false;

        while (readUntilExitTag(BackupFile.TAG_PRESETS)) {
            switch (mParser.getEventType()) {
                case XmlPullParser.START_TAG: {
                    switch (mParser.getName()) {
                        case BackupFile.TAG_ITEM:
                            presetId = 0;
                            name = null;
                            bandGains = null;
                            prebuilt = false;
                            break;
                        case BackupFile.TAG_PRESET_ID:
                            presetId = Integer.parseInt(mParser.nextText());
                            break;
                        case BackupFile.TAG_NAME:
                            name = mParser.nextText();
                            break;
                        case BackupFile.TAG_GAINS:
                            bandGains = mParser.nextText();
                            break;
                        case BackupFile.TAG_PREBUILT:
                            prebuilt = Boolean.parseBoolean(mParser.nextText());
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                    if (mParser.getName().equals(BackupFile.TAG_ITEM))
                        mFile.addPreset(presetId, name, bandGains, prebuilt);
                    break;
            }
        }
    }

    private void readPlaylists() throws IOException, XmlPullParserException {
        String name = null;
        String importPath = null;
        ArrayList<String> songs = null;

        while (readUntilExitTag(BackupFile.TAG_PLAYLISTS)) {
            switch (mParser.getEventType()) {
                case XmlPullParser.START_TAG: {
                    switch (mParser.getName()) {
                        case BackupFile.TAG_ITEM:
                            name = null;
                            importPath = null;
                            songs = null;
                            break;
                        case BackupFile.TAG_NAME:
                            name = mParser.nextText();
                            break;
                        case BackupFile.TAG_IMPORT_PATH:
                            importPath = mParser.nextText();
                            break;
                        case BackupFile.TAG_SONGS:
                            songs = readPlaylistSongs();
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                    if (mParser.getName().equals(BackupFile.TAG_ITEM))
                        mFile.addPlaylist(name, importPath, songs);

                    break;
            }
        }
    }

    private ArrayList<String> readPlaylistSongs() throws IOException, XmlPullParserException {
        ArrayList<String> songs = new ArrayList<>();

        while (readUntilExitTag(BackupFile.TAG_SONGS)) {
            switch (mParser.getEventType()) {
                case XmlPullParser.START_TAG: {
                    switch (mParser.getName()) {
                        case BackupFile.TAG_ITEM:
                            break;
                        case BackupFile.TAG_SONG_PATH:
                            songs.add(mParser.nextText());
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                    break;
            }
        }

        return songs;
    }

    private boolean readUntilExitTag(String tag) throws IOException, XmlPullParserException {
        return mParser.nextTag() != XmlPullParser.END_TAG || !mParser.getName().equals(tag);
    }
}
