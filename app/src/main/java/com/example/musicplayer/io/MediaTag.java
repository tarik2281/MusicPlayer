package com.example.musicplayer.io;

import com.example.musicplayer.MusicApplication;
import com.example.musicplayer.Util;

import java.util.Locale;

/**
 * Created by 19tarik97 on 28.02.16.
 */
public class MediaTag {

    private static native int nNewInstance();
    private static native void nReleaseInstance(int handle);
    private static native boolean nOpen(int handle, int fileType, String filePath);
    private static native boolean nOpenFd(int handle, int fileType, int fd);
    private static native void nClose(int handle);

    private static native String nGetMetadata(int handle, int metaKey);
    private static native int nGetProperty(int handle, int propKey);
    private static native boolean nHasAlbumArt(int handle);
    private static native int nGetAlbumArtType(int handle);
    private static native int nGetAlbumArtWidth(int handle);
    private static native int nGetAlbumArtHeight(int handle);
    private static native boolean nExtractAlbumArt(int handle, String filePath);
    private static native boolean nExtractAlbumArtFd(int handle, int fd);

    private static native void nSetMetadata(int handle, int metaKey, String value);
    private static native void nSetAlbumArt(int handle, String filePath, int type);
    private static native void nSetAlbumArtFd(int handle, int fd, int type);
    private static native void nRemoveAlbumArt(int handle);
    private static native boolean nSave(int handle);

    public enum FileType {
        None, APE, ASF, FLAC, IT, MOD, MP4, MPC, MPEG,
        OGGFLAC, OPUS, SPEEX, Vorbis, AIFF, WAV, S3M,
        TrueAudio, WavPack, XM, AAC, ADTS
    }

    public enum Metadata {
        Title,
        Artist,
        AlbumArtist,
        Album,
        Genre,
        TrackNumber,
        DiscNumber,
        Year
    }

    public enum Properties {
        Duration,
        Bitrate,
        Samplerate,
        Channels
    }

    public enum ImageType {
        JPEG, PNG, BMP, GIF, Unknown
    }

    private int mHandle;

    public MediaTag() {
        if (!MusicApplication.isNativeLoaded()) {
            Util.loge("MediaTag", "MediaTag: Could not create MediaTag instance: Library not loaded");
            mHandle = 0;
        }
        else
            mHandle = nNewInstance();
    }

    public boolean open(String filePath) {
        return open(getFileType(filePath), filePath);
    }

    public boolean open(FileType type, String filePath) {
        return mHandle != 0 && nOpen(mHandle, type.ordinal(), filePath);
    }

    public boolean open(FileType type, int fileDescriptor) {
        return mHandle != 0 && nOpenFd(mHandle, type.ordinal(), fileDescriptor);
    }

    public void close() {
        if (mHandle != 0)
            nClose(mHandle);
    }

    public void release() {
        if (mHandle != 0)
            nReleaseInstance(mHandle);
    }

    public String getMetadata(Metadata key) {
        return mHandle != 0 ? nGetMetadata(mHandle, key.ordinal()) : null;
    }

    public int getProperty(Properties propKey) {
        return mHandle != 0 ? nGetProperty(mHandle, propKey.ordinal()) : 0;
    }

    public boolean hasAlbumArt() {
        return mHandle != 0 && nHasAlbumArt(mHandle);
    }

    public ImageType getAlbumArtType() {
        return mHandle != 0 ? ImageType.values()[nGetAlbumArtType(mHandle)] : ImageType.Unknown;
    }

    public int getAlbumArtWidth() {
        return mHandle != 0 ? nGetAlbumArtWidth(mHandle) : 0;
    }

    public int getAlbumArtHeight() {
        return mHandle != 0 ? nGetAlbumArtHeight(mHandle) : 0;
    }

    /**
     *
     * @param filePath
     * @return true if success
     */
    public boolean extractAlbumArt(String filePath) {
        return mHandle != 0 && nExtractAlbumArt(mHandle, filePath);
    }

    public boolean extractAlbumArt(int fileDescriptor) {
        return mHandle != 0 && nExtractAlbumArtFd(mHandle, fileDescriptor);
    }

    public void setMetadata(Metadata metaKey, String value) {
        if (mHandle != 0)
            nSetMetadata(mHandle, metaKey.ordinal(), value);
    }

    public void setAlbumArt(String filePath, ImageType imageType) {
        if (mHandle != 0)
            nSetAlbumArt(mHandle, filePath, imageType.ordinal());
    }

    public void setAlbumArt(int fileDescriptor, ImageType imageType) {
        if (mHandle != 0)
            nSetAlbumArtFd(mHandle, fileDescriptor, imageType.ordinal());
    }

    public void removeAlbumArt() {
        if (mHandle != 0)
            nRemoveAlbumArt(mHandle);
    }

    public boolean save() {
        return mHandle != 0 &&  nSave(mHandle);
    }

    public int getHandle() {
        return mHandle;
    }

    public static FileType getFileType(String path) {
        int slashIndex = path.lastIndexOf('/');
        int dotIndex = path.lastIndexOf('.');

        if (dotIndex > slashIndex) {
            String extension = path.substring(dotIndex + 1).toUpperCase(Locale.US);
            switch (extension) {
                case "MP3":
                    return FileType.MPEG;
                case "M4A":
                case "M4R":
                case "M4B":
                case "M4P":
                    return FileType.MP4;
                case "AAC":
                    return FileType.AAC;
                case "OGG":
                    return FileType.Vorbis;
                case "OGA":
                    return FileType.OGGFLAC;
                case "FLAC":
                    return FileType.FLAC;
                case "MPC":
                    return FileType.MPC;
                case "WV":
                    return FileType.WavPack;
                case "SPX":
                    return FileType.SPEEX;
                case "OPUS":
                    return FileType.OPUS;
                case "TTA":
                    return FileType.TrueAudio;
                case "WMA":
                case "ASF":
                    return FileType.ASF;
                case "AIF":
                case "AIFF":
                case "AFC":
                case "AIFC":
                    return FileType.AIFF;
                case "WAV":
                    return FileType.WAV;
                case "APE":
                    return FileType.APE;
                case "MOD":
                case "MODULE":
                case "NST":
                case "WOW":
                    return FileType.MOD;
                case "S3M":
                    return FileType.S3M;
                case "IT":
                    return FileType.IT;
                case "XM":
                    return FileType.XM;
            }
        }

        return FileType.None;
    }
}
