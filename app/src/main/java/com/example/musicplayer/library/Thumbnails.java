package com.example.musicplayer.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.musicplayer.R;
import com.example.musicplayer.Util;
import com.example.musicplayer.io.Decoder;
import com.example.musicplayer.io.MediaTag;
import com.example.musicplayer.ui.AlbumArtCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by 19tar on 01.10.2017.
 */

public class Thumbnails {

    private static final String FOLDER = "albumarts";

    private static Thumbnails sSingleton;

    private boolean mInitialized;
    private File mFolder;
    private int mSize;

    private Thumbnails() {

    }

    public void initialize(Context context) {
        if (!mInitialized) {
            mFolder = new File(context.getFilesDir(), FOLDER);
            mFolder.mkdirs();

            mSize = context.getResources().getDimensionPixelSize(R.dimen.album_cover_size);

            mInitialized = true;
        }
    }

    public File getFile(int albumId) {
        return new File(mFolder, String.valueOf(albumId));
    }

    File delete(int albumId) {
        File file = getFile(albumId);

        if (file.exists())
            file.delete();

        return file;
    }

    void deleteAll() {
        Util.deleteDirectory(mFolder);
        mFolder.mkdir();
    }

    public Decoder getDecoder() {
        Decoder decoder = new Decoder();
        decoder.setVideoSize(mSize, mSize);
        return decoder;
    }

    public Bitmap decodeBitmap(int albumId, Decoder decoder, MediaTag cache) {
        Bitmap res = null;

        boolean releaseTag = false;
        if (cache == null) {
            cache = new MediaTag();
            releaseTag = true;
        }

        Collection<Song> songs = MusicLibrary.getInstance().getSongsForObject(LibraryObject.ALBUM,
                albumId, null, Sorting.ID, false);

        for (Song song : songs) {
            if (res != null)
                break;

            if (cache.open(song.getInfo().getFilePath())) {
                res = decoder.readAlbumArt(cache);

                cache.close();
            }
        }

        if (releaseTag)
            cache.release();

        return res;
    }

    public Bitmap loadBitmap(int albumId) {
        File file = getFile(albumId);
        if (file.exists())
            return BitmapFactory.decodeFile(file.getAbsolutePath());

        return null;
    }

    void save(int albumId, Decoder decoder, MediaTag cache) {
        boolean releaseTag = false;
        if (cache == null) {
            cache = new MediaTag();
            releaseTag = true;
        }

        File file = delete(albumId);
        Bitmap bitmap = decodeBitmap(albumId, decoder, cache);

        if (bitmap != null) {
            FileOutputStream os = null;

            try {
                os = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    if (os != null)
                        os.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        AlbumArtCache.getInstance().invalidateAlbumArt(albumId);

        if (releaseTag)
            cache.release();
    }

    void saveAll(Collection<Integer> albums, Decoder decoder) {
        MediaTag tag = new MediaTag();

        for (int id : albums)
            save(id, decoder, tag);

        tag.release();
    }

    public static Thumbnails getInstance() {
        if (sSingleton == null)
            sSingleton = new Thumbnails();

        return sSingleton;
    }
}
