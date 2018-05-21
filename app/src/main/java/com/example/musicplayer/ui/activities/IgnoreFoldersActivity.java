package com.example.musicplayer.ui.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.library.Folder;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.ui.dialogs.BaseDialogFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by 19tar on 28.09.2017.
 */

public class IgnoreFoldersActivity extends BaseActivity {

    private static final int ANIMATION_DURATION = 250; // in milliseconds

    private static class Entry {
        private Folder mFolder;
        private int mLevel;
        private boolean mChanged;
        private boolean mChecked;
        private boolean mExpanded;
        private boolean mExpandable;
        private Entry[] mEntries;

        public void setEntries(Collection<Folder> folders) {
            mEntries = createEntries(this, folders);
        }

        private static Entry[] createEntries(Entry parent, Collection<Folder> folders) {
            Entry[] entries = new Entry[folders.size()];

            int index = 0;
            for (Folder folder : folders) {
                entries[index++] = createEntry(parent, folder);
            }

            return entries;
        }

        private static Entry createEntry(Entry parent, Folder folder) {
            Entry entry = new Entry();

            entry.mFolder = folder;

            entry.mLevel = 0;
            if (parent != null)
                entry.mLevel = parent.mLevel + 1;

            entry.mChanged = false;
            entry.mChecked = !folder.isIgnored();
            entry.mExpanded = false;
            entry.mExpandable = folder.getFolderCount() > 0;

            return entry;
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private Entry[] mRoots;

        private ArrayList<Entry> mPresentationCache;

        private boolean mInternalUpdate;
        private int mOffset;

        public void setOffset(int offset) {
            mOffset = offset;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
            private View mView;
            private ImageView mArrowView;
            private TextView mTitleView;
            private CheckBox mCheckBox;

            public ViewHolder(View itemView, int viewType) {
                super(itemView);

                mView = itemView;
                mArrowView = (ImageView)mView.findViewById(R.id.arrow);
                mTitleView = (TextView)mView.findViewById(R.id.text_title);
                mCheckBox = (CheckBox)mView.findViewById(R.id.check);

                ((LinearLayout.LayoutParams)mArrowView.getLayoutParams()).setMargins(viewType * mOffset, 0, 0, 0);

                mView.setOnClickListener(this);
                mCheckBox.setOnCheckedChangeListener(this);
            }

            @Override
            public void onClick(View view) {
                Entry entry = getItemAt(getAdapterPosition());

                if (entry.mExpanded && collapse(getAdapterPosition()))
                    mArrowView.animate().setDuration(ANIMATION_DURATION).rotation(0).start();
                else if (!entry.mExpanded && expand(getAdapterPosition()))
                    mArrowView.animate().setDuration(ANIMATION_DURATION).rotation(90).start();
            }

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (!mInternalUpdate) {
                    mChanged = true;
                    Entry entry = getItemAt(getAdapterPosition());
                    setChecked(entry, checked);
                    if (entry.mExpanded)
                        notifyItemRangeChanged(getAdapterPosition() + 1, getItemCount(entry));
                }
            }
        }

        private void setChecked(Entry entry, boolean checked) {
            entry.mChanged = true;
            entry.mChecked = checked;

            if (entry.mExpandable && entry.mEntries == null)
                onExpand(entry);

            if (entry.mEntries != null) {
                for (Entry e : entry.mEntries)
                    setChecked(e, checked);
            }
        }

        private boolean expand(int position) {
            Entry entry = getItemAt(position);

            if (entry.mExpandable && !entry.mExpanded) {
                if (entry.mEntries == null) {
                    onExpand(entry);

                    if (entry.mEntries == null)
                        throw new IllegalStateException("Entries were not set by onExpand");
                }

                entry.mExpanded = true;
                updatePresentation();
                notifyItemRangeInserted(position + 1, getItemCount(entry));
                return true;
            }

            return false;
        }

        private boolean collapse(int position) {
            Entry entry = getItemAt(position);

            if (entry.mExpanded) {
                int itemCount = getItemCount(entry);
                entry.mExpanded = false;
                updatePresentation();
                notifyItemRangeRemoved(position + 1, itemCount);
                return true;
            }

            return false;
        }

        public Adapter() {
            mInternalUpdate = false;
        }

        public void setRoots(Collection<Folder> folders) {
            mRoots = Entry.createEntries(null, folders);
            updatePresentation();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = getLayoutInflater();
            return new ViewHolder(inflater.inflate(R.layout.entry_ignore_folder, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Entry entry = getItemAt(position);

            mInternalUpdate = true;

            holder.mArrowView.setVisibility(entry.mExpandable ? View.VISIBLE : View.INVISIBLE);

            if (entry.mExpandable) {
                if (entry.mExpanded)
                    holder.mArrowView.animate().setDuration(0).rotation(90).start();
                else
                    holder.mArrowView.animate().setDuration(0).rotation(0).start();
            }

            holder.mTitleView.setText(entry.mFolder.getName());
            holder.mCheckBox.setChecked(entry.mChecked);

            mInternalUpdate = false;
        }

        @Override
        public int getItemCount() {
            return mPresentationCache.size();
            /*int count = mRoots.length;

            for (Entry entry : mRoots) {
                count += getItemCount(entry);
            }

            return count;*/
        }

        private int getItemCount(Entry entry) {
            int count = 0;

            if (entry.mExpanded) {
                count += entry.mEntries.length;

                for (Entry e : entry.mEntries)
                    count += getItemCount(e);
            }

            return count;
        }

        @Override
        public int getItemViewType(int position) {
            return getItemAt(position).mLevel;
        }

        private Entry getItemAt(int position) {
            return mPresentationCache.get(position);
        }

        private void updatePresentation() {
            if (mPresentationCache == null)
                mPresentationCache = new ArrayList<>(mRoots.length);

            mPresentationCache.clear();

            for (Entry entry : mRoots)
                addEntriesToPresentation(entry);
        }

        private void addEntriesToPresentation(Entry entry) {
            mPresentationCache.add(entry);

            if (entry.mExpanded) {
                for (Entry e : entry.mEntries)
                    addEntriesToPresentation(e);
            }
        }

        public Parcelable saveState() {
            SavedState state = new SavedState();
            state.mEntries = mRoots;
            return state;
        }

        public void restoreState(Parcelable parcelable) {
            SavedState state = (SavedState)parcelable;

            mRoots = state.mEntries;
            updatePresentation();
        }
    }

    private static class SavedState implements Parcelable {

        private Entry[] mEntries;

        public SavedState() {

        }

        protected SavedState(Parcel in) {
            int count = in.readInt();

            mEntries = new Entry[count];
            for (int i = 0; i < count; i++)
                mEntries[i] = readEntry(in);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mEntries.length);

            for (Entry entry : mEntries)
                writeEntry(dest, entry);
        }

        private void writeEntry(Parcel dest, Entry e) {
            dest.writeInt(e.mFolder.getId());
            dest.writeInt(e.mLevel);
            writeBoolean(dest, e.mChanged);
            writeBoolean(dest, e.mChecked);
            writeBoolean(dest, e.mExpanded);
            if (e.mEntries != null) {
                dest.writeInt(e.mEntries.length);
                for (Entry entry : e.mEntries)
                    writeEntry(dest, entry);
            } else
                dest.writeInt(0);
        }

        private Entry readEntry(Parcel in) {
            Entry entry = new Entry();
            entry.mFolder = MusicLibrary.getInstance().getFolderById(in.readInt());
            entry.mLevel = in.readInt();
            entry.mChanged = readBoolean(in);
            entry.mChecked = readBoolean(in);
            entry.mExpanded = readBoolean(in);
            entry.mExpandable = entry.mFolder.getFolderCount() > 0;
            int count = in.readInt();
            if (count > 0) {
                entry.mEntries = new Entry[count];

                for (int i = 0; i < count; i++)
                    entry.mEntries[i] = readEntry(in);
            }
            return entry;
        }

        private void writeBoolean(Parcel dest, boolean val) {
            dest.writeInt(val ? 1 : 0);
        }

        private boolean readBoolean(Parcel in) {
            return in.readInt() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private static final String KEY_CHANGED = "changed";

    private RecyclerView mRecylerView;
    private Adapter mAdapter;
    private boolean mChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRecylerView = new RecyclerView(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecylerView.setLayoutManager(layoutManager);

        mAdapter = new Adapter();
        if (savedInstanceState != null) {
            mAdapter.restoreState(savedInstanceState.getParcelable(KEY_ADAPTER));
            mChanged = savedInstanceState.getBoolean(KEY_CHANGED);
        }
        else
            mAdapter.setRoots(MusicLibrary.getInstance().getRootFolders());
        mRecylerView.setAdapter(mAdapter);

        mAdapter.mOffset = getResources().getDimensionPixelSize(R.dimen.ignore_folders_offset);

        setContentView(mRecylerView);
    }

    private static final String KEY_ADAPTER = "adapter_entry_structure";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_ADAPTER, mAdapter.saveState());
        outState.putBoolean(KEY_CHANGED, mChanged);
    }

    private void onExpand(Entry entry) {
        // sub entries for the given entry must be set here
        entry.setEntries(MusicLibrary.getInstance().getSubfoldersAll(entry.mFolder.getId()));
    }

    @Override
    public void onBackPressed() {
        if (mChanged) {
            ApplyDialog.newInstance().show(getSupportFragmentManager(), "apply_dialog");
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && mChanged) {
            ApplyDialog.newInstance().show(getSupportFragmentManager(), "apply_dialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class ApplyDialog extends BaseDialogFragment implements DialogInterface.OnClickListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            builder.setTitle("Scanned Directories");
            builder.setMessage("Apply changes?");

            builder.setPositiveButton(R.string.dialog_button_yes, this);
            builder.setNegativeButton(R.string.dialog_button_no, this);

            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
                case DialogInterface.BUTTON_POSITIVE: {
                    IgnoreFoldersActivity activity = (IgnoreFoldersActivity)getActivity();

                    ArrayList<Folder> folders = new ArrayList<>();
                    ArrayList<Boolean> ignored = new ArrayList<>();

                    for (Entry e : activity.mAdapter.mRoots) {
                        addFolder(e, folders, ignored);
                    }

                    boolean[] ignoredArray = new boolean[ignored.size()];

                    for (int i = 0; i < ignored.size(); i++)
                        ignoredArray[i] = ignored.get(i);

                    MusicLibrary.getInstance().setFoldersIgnored(folders.toArray(new Folder[folders.size()]),
                            ignoredArray);

                    break;
                }
            }

            getActivity().finish();
        }

        private void addFolder(Entry e, List<Folder> folders, List<Boolean> ignored) {
            if (e.mChanged) {
                folders.add(e.mFolder);
                ignored.add(!e.mChecked);
            }

            if (e.mEntries != null) {
                for (Entry entry : e.mEntries)
                    addFolder(entry, folders, ignored);
            }
        }

        public static ApplyDialog newInstance() {
            return new ApplyDialog();
        }
    }
}
