package com.example.musicplayer.library;

import com.example.musicplayer.Util;

import java.util.Comparator;

/**
 * Created by Tarik on 06.08.2016.
 */
public class Folder implements LibraryObject {

    public static class FolderComparator implements Comparator<Folder> {

        private Sorting sorting;
        private boolean reversed;

        public FolderComparator(Sorting sorting, boolean reversed) {
            this.sorting = sorting;
            this.reversed = reversed;
        }

        @Override
        public int compare(Folder lhs, Folder rhs) {
            int res = 0;

            switch (sorting) {
                case ID:
                    return Util.longCompare(lhs.getId(), rhs.getId());
                case Name:
                    res = nameCompare(lhs, rhs);

                    if (res == 0)
                        res = pathCompare(lhs, rhs);

                    break;
                case Path:
                    res = pathCompare(lhs, rhs);

                    if (res == 0)
                        res = nameCompare(lhs, rhs);

                    break;
            }

            if (res == 0)
                res = Util.longCompare(lhs.getId(), rhs.getId());

            return res * (reversed ? -1 : 1);
        }

        private int nameCompare(Folder lhs, Folder rhs) {
            return Util.stringCompare(lhs.getName(), rhs.getName());
        }

        private int pathCompare(Folder lhs, Folder rhs) {
            return Util.stringCompare(lhs.getPath(), rhs.getPath());
        }
    }

    public static class Builder {
        private int mId;
        private String mName;
        private String mPath;
        private int mParentId;
        private IgnoreType mIgnored;
        private int mSongCount;
        private int mFolderCount;

        public Builder() {
            reset();
        }

        public Builder setId(int id) {
            mId = id;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setPath(int parentId, String path) {
            mParentId = parentId;
            mPath = path;
            return this;
        }

        public Builder setIgnoreType(IgnoreType ignored) {
            mIgnored = ignored;
            return this;
        }

        public Builder setSongCount(int songCount) {
            mSongCount = songCount;
            return this;
        }

        public Builder setFolderCount(int folderCount) {
            mFolderCount = folderCount;
            return this;
        }

        public Folder build() {
            Folder folder = new Folder();

            folder.mId = mId;
            folder.mName = mName;
            folder.mPath = mPath;
            folder.mParentId = mParentId;
            folder.mIgnored = mIgnored;
            folder.mSongCount = mSongCount;
            return folder;
        }

        private void reset() {
            mId = 0;
            mName = null;
            mPath = null;
            mParentId = 0;
            mIgnored = IgnoreType.NotIgnore;
        }
    }

    public enum IgnoreType {
        Ignore, UserIgnore, UserNotIgnore, NotIgnore
    }

    private int mId;
    private String mName;
    private String mPath;
    private int mParentId;
    private IgnoreType mIgnored;
    private int mSongCount;
    private int mFolderCount;

    private Folder() {
        this(0, 0, null, null, IgnoreType.NotIgnore);
    }

    Folder(int id, int parentId, String path, String name, IgnoreType ignored) {
        mId = id;
        mParentId = parentId;
        mPath = path;
        mName = name;
        mIgnored = ignored;
        mSongCount = 0;
        mFolderCount = 0;
    }

    @Override
    public int getType() {
        return LibraryObject.FOLDER;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return mName;
    }

    public String getPath() {
        return mPath;
    }

    public int getParentId() {
        return mParentId;
    }

    public String getFullPath() {
        return makeFullPath(getPath(), getName());
    }

    public boolean isIgnored() {
        return mIgnored == IgnoreType.Ignore || mIgnored == IgnoreType.UserIgnore;
    }

    public int getSongCount() {
        return mSongCount;
    }

    public int getFolderCount() {
        return mFolderCount;
    }

    IgnoreType getIgnoreType() {
        return mIgnored;
    }

    void setIgnored(IgnoreType ignored) {
        mIgnored = ignored;
    }

    void setSongCount(int songCount) {
        mSongCount = songCount;
    }

    void setFolderCount(int folderCount) {
        mFolderCount = folderCount;
    }

    public static String getPathFromFullPath(String fullPath) {
        int index = fullPath.lastIndexOf('/');
        return fullPath.substring(0, index);
    }

    public static String getNameFromFullPath(String fullPath) {
        int index = fullPath.lastIndexOf('/');
        return fullPath.substring(index + 1);
    }

    private static String makeFullPath(String path, String name) {
        return path + '/' + name;
    }
}
