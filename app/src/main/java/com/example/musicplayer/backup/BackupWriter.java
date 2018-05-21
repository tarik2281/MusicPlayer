package com.example.musicplayer.backup;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

/**
 * Created by 19tar on 04.10.2017.
 */

public class BackupWriter {

    private BackupFile mFile;

    private LinkedList<String> mTags;
    private XmlSerializer mSerializer;

    public BackupWriter(BackupFile file) {
        mFile = file;
        mTags = new LinkedList<>();
    }

    public boolean saveToStream(OutputStream stream) {
        boolean result = false;

        GZIPOutputStream outStream = null;

        try {
            outStream = new GZIPOutputStream(stream);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(stream, "utf-8");
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument("utf-8", null);

            mSerializer = serializer;

            startTag(BackupFile.TAG_ROOT);

            savePreferences();
            saveStats();
            savePresets();
            savePlaylists();

            endTag();

            serializer.endDocument();

            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (outStream != null)
                    outStream.close();
                else
                    stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mSerializer = null;
        }

        return result;
    }

    private void savePreferences() throws IOException {
        startTag(BackupFile.TAG_PREFS);

        for (BackupFile.Preference pref : mFile.getPreferences()) {
            startTag(BackupFile.TAG_ITEM);

            textTag(BackupFile.TAG_TYPE, pref.getType());
            textTag(BackupFile.TAG_KEY, pref.getKey());
            textTag(BackupFile.TAG_VALUE, pref.getStringValue());

            endTag();
        }

        endTag();
    }

    private void saveStats() throws IOException {
        startTag(BackupFile.TAG_STATS);

        for (BackupFile.SongStat stat : mFile.getSongStats()) {
            startTag(BackupFile.TAG_ITEM);

            textTag(BackupFile.TAG_SONG_PATH, stat.getSongPath());
            textTag(BackupFile.TAG_DATE_ADDED, String.valueOf(stat.getDateAdded()));
            textTag(BackupFile.TAG_PRESET_ID, String.valueOf(stat.getPresetId()));
            textTag(BackupFile.TAG_PLAY_COUNT, String.valueOf(stat.getPlayCount()));
            textTag(BackupFile.TAG_LAST_PLAYED, String.valueOf(stat.getLastPlayed()));
            textTag(BackupFile.TAG_IS_HIDDEN, String.valueOf(stat.isHidden()));

            endTag();
        }

        endTag();
    }

    private void savePresets() throws IOException {
        startTag(BackupFile.TAG_PRESETS);

        for (BackupFile.Preset preset : mFile.getPresets()) {
            startTag(BackupFile.TAG_ITEM);

            textTag(BackupFile.TAG_PRESET_ID, String.valueOf(preset.getId()));
            textTag(BackupFile.TAG_NAME, preset.getName());
            textTag(BackupFile.TAG_GAINS, preset.getBandGains());
            textTag(BackupFile.TAG_PREBUILT, String.valueOf(preset.isPrebuilt()));

            endTag();
        }

        endTag();
    }

    private void savePlaylists() throws IOException {
        startTag(BackupFile.TAG_PLAYLISTS);

        for (BackupFile.Playlist playlist : mFile.getPlaylists()) {
            startTag(BackupFile.TAG_ITEM);

            textTag(BackupFile.TAG_NAME, playlist.getName());
            if (playlist.getImportPath() != null)
                textTag(BackupFile.TAG_IMPORT_PATH, playlist.getImportPath());

            savePlaylistSongs(playlist);

            endTag();
        }

        endTag();
    }

    private void savePlaylistSongs(BackupFile.Playlist playlist) throws IOException {
        startTag(BackupFile.TAG_SONGS);

        for (String song : playlist.getSongList()) {
            startTag(BackupFile.TAG_ITEM);

            textTag(BackupFile.TAG_SONG_PATH, song);

            endTag();
        }

        endTag();
    }

    private void startTag(String tag) throws IOException {
        mTags.addLast(tag);
        mSerializer.startTag(null, tag);
    }

    private void endTag() throws IOException {
        mSerializer.endTag(null, mTags.pollLast());
    }

    private void textTag(String tag, String value) throws IOException {
        startTag(tag);
        mSerializer.text(value);
        endTag();
    }
}
