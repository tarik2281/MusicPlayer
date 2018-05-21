package com.example.musicplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.provider.DocumentFile;

import com.example.musicplayer.ui.dialogs.SDCardAccessDialog;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by 19tarik97 on 04.12.16.
 */

public class StorageManager {

    private static StorageManager sSingleton;

    public static StorageManager getInstance() {
        if (sSingleton == null)
            sSingleton = new StorageManager();

        return sSingleton;
    }

    public static final int RESULT_WRITABLE = 0;
    public static final int RESULT_READONLY = 1;
    public static final int RESULT_REQUEST_ACCESS = 2;

    public static final String KEY_SD_CARD_TREE_URI = "storage_sd_card_tree_uri";
    private static final String KEY_SCANNED_STORAGES = "library_scanned_storages";

    private boolean mInited;
    private Context mContext;

    private Set<String> mStorages;

    private boolean mUnavailablesChecked;
    private Set<String> mUnavailables;

    private StorageManager() {
        mInited = false;
        mContext = null;
    }

    public void initialize(Context context) {
        if (!mInited) {
            mContext = context.getApplicationContext();

            mUnavailablesChecked = false;
            loadStorages();

            mInited = true;
        }// TODO: save scanned storages
    }

    public boolean unavailablesChecked() {
        return mUnavailablesChecked;
    }

    public void updateUnavailables(String[] storages, boolean[] remove) {
        HashSet<String> set = new HashSet<>(mStorages);

        if (storages != null) {
            mUnavailables = new HashSet<>();
            for (int i = 0; i < storages.length; i++) {
                if (!remove[i]) {
                    mUnavailables.add(storages[i]);
                    set.add(storages[i]);
                }
            }
        }

        mUnavailablesChecked = true;
        CacheManager.getInstance().setStringSet(KEY_SCANNED_STORAGES, set);
    }

    public boolean documentsApiSupported() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public int checkSDCardAccess(String filePath, FragmentManager fragmentManager) {
        File file = new File(filePath);

        boolean writable = file.canWrite();

        if (documentsApiSupported() && file.exists() && !writable) {
            DocumentFile documentFile = getSDCardFile(null, filePath, false);
            if (documentFile == null) {
                if (fragmentManager != null) {
                    SDCardAccessDialog.newInstance(Util.getStorageRoot(filePath))
                            .show(fragmentManager, "sd_card_access_dialog");
                    return RESULT_REQUEST_ACCESS;
                }
            }
            else {
                writable = true;
            }
        }

        return writable ? RESULT_WRITABLE : RESULT_READONLY;
    }

    @TargetApi(19)
    public void setSDCardRoot(Uri treeUri) {
        mContext.getContentResolver().takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        CacheManager.getInstance().setString(KEY_SD_CARD_TREE_URI, treeUri.toString());
    }

    public DocumentFile getSDCardRoot() {
        String uriStr = CacheManager.getInstance().getString(KEY_SD_CARD_TREE_URI, null);
        DocumentFile result = null;

        if (uriStr != null) {
            Uri treeUri = Uri.parse(uriStr);

            result = DocumentFile.fromTreeUri(mContext, treeUri);
        }

        return result;
    }

    public DocumentFile getSDCardFile(@Nullable DocumentFile root, String absolutePath, boolean createIfNull) {
        DocumentFile result = null;

        if (root != null || (root = getSDCardRoot()) != null) {
            String[] parts = absolutePath.split("/");
            boolean storage = parts[2].equals(root.getName());
            boolean emulated = parts[2].equals("emulated") && parts[3].equals(root.getName());
            int index = emulated ? 4 : 3;

            if (storage || emulated) {
                if (parts.length == index)
                    return root;

                for (; index < parts.length - 1; index++) {
                    String part = parts[index];

                    if (root != null)
                        root = root.findFile(part);
                }

                if (root != null) {
                    result = root.findFile(parts[parts.length - 1]);
                    if (result == null && createIfNull)
                        result = root.createFile("*/*", parts[parts.length - 1]);
                }
            }
        }

        return result;
    }

    public int openAndDetachFd(Uri uri, String mode) {
        int fileDescriptor = 0;

        ParcelFileDescriptor fd = null;
        try {
            fd = mContext.getContentResolver().openFileDescriptor(uri, mode);
            if (fd != null)
                fileDescriptor = fd.detachFd();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fd != null)
                    fd.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileDescriptor;
    }

    public String[] getUnavailableStorages() {
        if (mUnavailablesChecked) {
            if (mUnavailables == null)
                return null;
            return mUnavailables.toArray(new String[mUnavailables.size()]);
        }
        else {
            Set<String> storages = CacheManager.getInstance().getStringSet(KEY_SCANNED_STORAGES);

            if (storages != null) {
                ArrayList<String> unavailables = new ArrayList<>();

                for (String storage : storages) {
                    File file = new File(storage);

                    File[] list = file.listFiles((FileFilter) null);
                    if (!file.exists() || !file.canRead() || list == null || list.length == 0)
                        unavailables.add(storage);
                }

                if (unavailables.size() > 0)
                    return unavailables.toArray(new String[unavailables.size()]);
            }

            return null;
        }
    }

    public Set<String> getAllStorages() {
        return mStorages;
    }

    private void loadStorages() {
        mStorages = new HashSet<String>();

        if (Build.VERSION.SDK_INT >= 19) {
            File[] externalDirs = mContext.getExternalFilesDirs(null);

            for (File dir : externalDirs) {
                if (dir != null) {
                    String path = dir.getAbsolutePath().split("/Android")[0];
                    mStorages.add(path);
                }
            }
        }

        if (mStorages.isEmpty()) {
            mStorages.add(Environment.getExternalStorageDirectory().getAbsolutePath());

            String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
            String s = "";
            try {
                final Process process = new ProcessBuilder().command("mount")
                        .redirectErrorStream(true).start();
                process.waitFor();
                final InputStream is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    s = s + new String(buffer);
                }
                is.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }

            // parse output
            final String[] lines = s.split("\n");
            for (String line : lines) {
                if (!line.toLowerCase(Locale.US).contains("asec")) {
                    if (line.matches(reg)) {
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            if (part.startsWith("/"))
                                if (!part.toLowerCase(Locale.US).contains("vold"))
                                    mStorages.add(part);
                        }
                    }
                }
            }
        }
    }
}
