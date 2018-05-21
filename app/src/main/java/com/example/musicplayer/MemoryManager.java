package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 19tar on 17.09.2017.
 */

public class MemoryManager {

    public interface Trimmable {
        void onTrimMemory();
    }

    private static MemoryManager sSingleton;

    private List<Trimmable> mTrimmables;

    private MemoryManager() {
        mTrimmables = new ArrayList<>();
    }

    public static MemoryManager getInstance() {
        if (sSingleton == null)
            sSingleton = new MemoryManager();

        return sSingleton;
    }

    public void registerForTrim(Trimmable t) {
        if (t == null)
            throw new NullPointerException("Cannot register a null value for trimming");

        mTrimmables.add(t);
    }

    public void unregisterFromTrim(Trimmable t) {
        mTrimmables.remove(t);
    }

    public void trimMemory() {
        for (Trimmable t : mTrimmables)
            t.onTrimMemory();
    }
}
