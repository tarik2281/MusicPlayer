package com.example.musicplayer.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by Tarik on 16.08.2016.
 */
public class PLSParser extends PlaylistParser {

    private static final String HEADER = "[playlist]";

    @Override
    protected void onParse() throws IOException {
        PlaylistFile file = getFile();
        BufferedReader reader = getReader();

        String line = reader.readLine();

        if (line == null || !line.equals(HEADER)) {
            file.setValid(false);
            return;
        }

        while ((line = reader.readLine()) != null) {
            int separatorIndex = line.indexOf('=');
            if (separatorIndex == -1)
                continue;

            String key = line.substring(0, separatorIndex);
            String value = line.substring(separatorIndex + 1);

            if (key.startsWith("File")) {
                int index = Integer.parseInt(line.substring(4, separatorIndex)) - 1;
                setEntryFilePath(getEntryForIndex(index), value);
            }
            else if (key.startsWith("Title")) {
                int index = Integer.parseInt(line.substring(5, separatorIndex)) - 1;
                getEntryForIndex(index).title = value;
            }
            else if (key.startsWith("Length")) {
                int index = Integer.parseInt(line.substring(6, separatorIndex)) - 1;
                getEntryForIndex(index).length = Integer.parseInt(value);
            }
        }
    }

    private PlaylistFile.Entry getEntryForIndex(int index) {
        List<PlaylistFile.Entry> entries = getFile().getEntryList();

        while (index >= entries.size())
            entries.add(new PlaylistFile.Entry());

        return entries.get(index);
    }
}
