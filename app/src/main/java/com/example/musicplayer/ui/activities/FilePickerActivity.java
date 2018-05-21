package com.example.musicplayer.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.R;
import com.example.musicplayer.StorageManager;
import com.example.musicplayer.Util;
import com.example.musicplayer.backup.BackupFile;
import com.example.musicplayer.io.PlaylistFile;
import com.example.musicplayer.ui.adapters.FileListAdapter;
import com.example.musicplayer.ui.adapters.ItemAdapter;
import com.example.musicplayer.ui.dialogs.FileExistsDialog;
import com.example.musicplayer.ui.dialogs.NewFolderDialog;
import com.example.musicplayer.ui.dialogs.OnDialogDismissListener;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * Created by Tarik on 15.08.2016.
 */
public class FilePickerActivity extends BaseActivity implements AdapterView.OnItemClickListener,
        View.OnClickListener, OnDialogDismissListener {

    private static class Storage {
        public String title;
        public File file;
    }

    private static class StorageListAdapter extends ItemAdapter<Storage> {

        public StorageListAdapter(Context context, Storage[] items) {
            super(context, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null)
                v = getLayoutInflater().inflate(R.layout.entry_storage, parent, false);

            TextView titleView = (TextView)v.findViewById(R.id.title);
            TextView pathView = (TextView)v.findViewById(R.id.path);

            Storage item = getItem(position);

            titleView.setText(item.title);
            pathView.setText(item.file.getAbsolutePath());

            return v;
        }
    }

    public static final String KEY_FILEPATH = "file_path";

    public static final int MODE_IMPORT = 0;
    public static final int MODE_EXPORT = 1;

    public static final int TYPE_PLAYLIST = 0;
    public static final int TYPE_BACKUP = 1;

    private static final String KEY_MODE = "mode";
    private static final String KEY_FILE_TYPE = "file_type";

    private static final String TAG_DIALOG = "new_folder_dialog";

    private File mCurrentDirectory;
    private int mLevel;

    private ArrayList<Storage> mStorages;
    private int mMode;
    private int mFileType;

    // export
    private File mSourceFile;

    private FileListAdapter mFileAdapter;
    private StorageListAdapter mStorageAdapter;

    private ListView mListView;
    private View mExportButton;
    private MenuItem mNewFolderItem;

    private Comparator<File> mFileComparator = new Comparator<File>() {

        @Override
        public int compare(File lhs, File rhs) {
            int res = Util.boolCompare(rhs.isDirectory(), lhs.isDirectory());

            if (res == 0)
                res = Util.stringCompare(lhs.getName(), rhs.getName());

            return res;
        }
    };

    private FileFilter mFileFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory() || mMode == MODE_EXPORT)
                return true;

            String fileName = pathname.getName();

            switch (mFileType) {
                case TYPE_PLAYLIST:
                    return PlaylistFile.hasPlaylistExtension(fileName);
                case TYPE_BACKUP:
                    return BackupFile.hasBackupExtension(fileName);
            }

            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMode = extras.getInt(KEY_MODE);

            String source = extras.getString(KEY_FILEPATH);
            if (source != null) {
                mSourceFile = new File(source);
            }

            mFileType = extras.getInt(KEY_FILE_TYPE, -1);

            switch (mMode) {
                case MODE_IMPORT:
                    switch (mFileType) {
                        case TYPE_PLAYLIST:
                            setTitle(R.string.title_import_playlist);
                            break;
                        case TYPE_BACKUP:
                            setTitle(R.string.title_select_backup_file);
                            break;
                    }
                    break;
                case MODE_EXPORT:
                    setTitle(R.string.title_select_export_dir);
                    break;
            }
        }

        setResult(RESULT_CANCELED);

        showBackButton(true);

        Set<String> storagePaths = StorageManager.getInstance().getAllStorages();

        mStorages = new ArrayList<>(storagePaths.size());

        for (String storagePath : storagePaths) {
            Storage storage = new Storage();
            if (Environment.isExternalStorageEmulated() && storagePath.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                storage.title = getString(R.string.file_picker_internal_storage);
            }
            else {
                storage.title = getString(R.string.file_picker_sd_card);
            }
            storage.file = new File(storagePath);
            mStorages.add(storage);
        }

        mFileAdapter = new FileListAdapter(this, null);
        mStorageAdapter = new StorageListAdapter(this, mStorages.toArray(new Storage[mStorages.size()]));

        setContentView(R.layout.activity_file_picker);

        if (mMode == MODE_EXPORT) {
            mExportButton = findViewById(R.id.button_export);
            if (mExportButton != null)
                mExportButton.setOnClickListener(this);
        }

        mListView = (ListView)findViewById(R.id.list);
        if (mListView != null)
            mListView.setOnItemClickListener(this);

        String filePath = null;
        if (savedInstanceState != null)
            filePath = savedInstanceState.getString(KEY_FILEPATH);

        if (filePath != null)
            setCurrentDirectory(new File(filePath));
        else
            showStorages();
    }

    @Override
    public void onBackPressed() {
        if (backAvailable()) {
            goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mLevel == 0) {
            Storage storage = mStorages.get(position);
            if (mMode == MODE_EXPORT && StorageManager.getInstance().checkSDCardAccess(storage.file.getAbsolutePath(),
                    getSupportFragmentManager()) == StorageManager.RESULT_REQUEST_ACCESS)
                return;

            mLevel++;
            setCurrentDirectory(storage.file);
            mListView.setAdapter(mFileAdapter);
            return;
        }

        if (backAvailable() && position == 0) {
            goBack();
            return;
        }

        File file = mFileAdapter.getItem(position);

        if (file.isDirectory()) {
            setCurrentDirectory(file);
            mLevel++;
        }
        else if (mMode == MODE_IMPORT) {
            Intent intent = new Intent("playlist_file_path");
            intent.putExtra(KEY_FILEPATH, file.getAbsolutePath());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_file_picker, menu);

        mNewFolderItem = menu.findItem(R.id.option_new_folder);

        updateNewFolderItem();

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentDirectory != null) {
            outState.putString(KEY_FILEPATH, mCurrentDirectory.getAbsolutePath());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_new_folder: {
                NewFolderDialog.newInstance(mCurrentDirectory.getAbsolutePath()).show(getSupportFragmentManager(), TAG_DIALOG);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_export: {
                if (mSourceFile == null) {
                    if (StorageManager.getInstance().checkSDCardAccess(mCurrentDirectory.getAbsolutePath(),
                            null) == StorageManager.RESULT_READONLY) {
                        Toast.makeText(this, R.string.file_picker_error_read_only, Toast.LENGTH_SHORT).show();
                        break;
                    }

                    Intent intent = new Intent("playlist_file_path");
                    intent.putExtra(KEY_FILEPATH, mCurrentDirectory.getAbsolutePath());
                    setResult(RESULT_OK, intent);
                    finish();
                }
                else {
                    File exportFile = new File(mCurrentDirectory, mSourceFile.getName());

                    if (StorageManager.getInstance().checkSDCardAccess(mCurrentDirectory.getAbsolutePath(),
                            null) == StorageManager.RESULT_READONLY) {
                        Toast.makeText(this, R.string.file_picker_error_read_only, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    else if (exportFile.exists()) {
                        FileExistsDialog.newInstance().show(getSupportFragmentManager(), "file_exists_dialog");
                        break;
                    }
                    else {
                        saveFile(exportFile);
                    }
                }

                break;
            }
        }
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog, String tag) {
        if (dialog instanceof NewFolderDialog && mCurrentDirectory != null) {
            setCurrentDirectory(mCurrentDirectory);
        }
        else if (dialog instanceof FileExistsDialog) {
            if (((FileExistsDialog)dialog).confirmed()) {
                saveFile(new File(mCurrentDirectory, mSourceFile.getName()));
            }
        }
    }

    private void saveFile(File exportFile) {
        OutputStream stream = null;

        try {
            if (!exportFile.canWrite()) {
                DocumentFile file = StorageManager.getInstance().getSDCardFile(null, exportFile.getAbsolutePath(), true);
                stream = getContentResolver().openOutputStream(file.getUri());
            }
            else
                stream = new FileOutputStream(exportFile);

            Util.copyFileToStream(mSourceFile, stream);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        setResult(RESULT_OK);
        finish();
    }

    private void showStorages() {
        mCurrentDirectory = null;
        mListView.setAdapter(mStorageAdapter);
        setSubtitle(getString(R.string.file_picker_storages));
        mLevel = 0;
        updateNewFolderItem();

        if (mExportButton != null)
            mExportButton.setVisibility(View.GONE);
    }

    private void setCurrentDirectory(File directory) {
        mCurrentDirectory = directory;
        setSubtitle(mCurrentDirectory.getAbsolutePath());

        File[] files = directory.listFiles(mFileFilter);
        if (files != null)
            Arrays.sort(files, mFileComparator);

        mFileAdapter.setShowUpItem(mCurrentDirectory.getParentFile() != null);
        mFileAdapter.setItems(files);
        mFileAdapter.notifyDataSetChanged();
        updateNewFolderItem();

        if (mMode == MODE_EXPORT && mExportButton != null)
            mExportButton.setVisibility(View.VISIBLE);
    }

    private void goBack() {
        if (mLevel > 1) {
            mLevel--;
            setCurrentDirectory(mCurrentDirectory.getParentFile());
        }
        else if (mLevel == 1) {
            showStorages();
        }
    }

    private boolean backAvailable() {
        return mLevel > 0;
    }

    private void updateNewFolderItem() {
        if (mNewFolderItem != null) {
            boolean visible = mLevel != 0 && mMode == MODE_EXPORT;

            mNewFolderItem.setEnabled(visible).setVisible(visible);
        }
    }


    public static Intent importIntent(Context context, int importType) {
        Intent intent = new Intent(context, FilePickerActivity.class);
        intent.putExtra(KEY_MODE, MODE_IMPORT);
        intent.putExtra(KEY_FILE_TYPE, importType);
        return intent;
    }

    public static Intent exportIntent(Context context) {
        Intent intent = new Intent(context, FilePickerActivity.class);
        intent.putExtra(KEY_MODE, MODE_EXPORT);
        return intent;
    }

    public static Intent exportIntent(Context context, String filePath, int fileType) {
        Intent intent = new Intent(context, FilePickerActivity.class);
        intent.putExtra(KEY_MODE, MODE_EXPORT);
        intent.putExtra(KEY_FILEPATH, filePath);
        intent.putExtra(KEY_FILE_TYPE, fileType);
        return intent;
    }
}
