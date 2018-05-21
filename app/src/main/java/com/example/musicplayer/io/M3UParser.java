package com.example.musicplayer.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by Tarik on 07.08.2016.
 */
public class M3UParser extends PlaylistParser {

    private boolean extended;

    @Override
    protected void onParse() throws IOException {
        BufferedReader reader = getReader();
        PlaylistFile file = getFile();

        String line = reader.readLine();

        if (line == null) {
            file.setValid(false);
            return;
        }

        file.setType(PlaylistFile.Type.M3U);

        extended = line.equals("#EXTM3U");

        if (!extended)
            file.getEntryList().add(makeEntry(line, null, -1));

        while ((line = reader.readLine()) != null) {
            if (extended && line.startsWith("#EXTINF:")) {
                int lengthIndex = line.indexOf(':');
                int titleIndex = line.indexOf(',');

                int length = Integer.parseInt(line.substring(lengthIndex + 1, titleIndex));
                String title = line.substring(titleIndex + 1);

                file.getEntryList().add(makeEntry(reader.readLine(), title, length));
            }
            else
                file.getEntryList().add(makeEntry(line, null, -1));
        }

        file.setValid(true);
    }
}
