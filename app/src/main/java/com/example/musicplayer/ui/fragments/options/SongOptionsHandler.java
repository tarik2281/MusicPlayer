package com.example.musicplayer.ui.fragments.options;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.playback.PlaybackState;
import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowAlbumRequest;
import com.example.musicplayer.request.ShowArtistRequest;
import com.example.musicplayer.request.ShowFolderRequest;
import com.example.musicplayer.TimeUtil;
import com.example.musicplayer.io.MediaTag;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.ui.dialogs.RemoveFromPlaylistDialog;

import java.util.Locale;

public class SongOptionsHandler extends MenuOptionsHandler<Song> {

	private static final String TAG_REMOVE_DIALOG = "remove_from_playlist";

	private boolean mShowUnhide;
	private boolean mShowArtist;
	private boolean mShowAlbum;
	private boolean mShowFolder;
	private Playlist mPlaylist;

	private int mBitrate = -1;
	private int mSampleRate = -1;
	private int mChannels = -1;
	private MediaTag.ImageType mCoverType = MediaTag.ImageType.Unknown;
	private int mCoverWidth = -1;
	private int mCoverHeight = -1;

	public SongOptionsHandler() {
		super(Song.class);

		mShowUnhide = true;
		mShowArtist = true;
		mShowAlbum = true;
		mShowFolder = true;
	}

	public void setShowUnhide(boolean showUnhide) {
		mShowUnhide = showUnhide;
	}

	public void setShowArtist(boolean showArtist) {
		mShowArtist = showArtist;
	}
	
	public void setShowAlbum(boolean showAlbum) {
		mShowAlbum = showAlbum;
	}

	public void setShowFolder(boolean showFolder) {
		mShowFolder = showFolder;
	}

	public void setPlaylist(Playlist playlist) {
		mPlaylist = playlist;
	}

	@Override
	protected void onCreateMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.options_song, menu);

		menu.findItem(R.id.option_sub_show_artist).setVisible(mShowArtist).setEnabled(mShowArtist);
		menu.findItem(R.id.option_sub_show_album).setVisible(mShowAlbum).setEnabled(mShowAlbum);
		menu.findItem(R.id.option_sub_show_folder).setVisible(mShowFolder).setEnabled(mShowFolder);

		if (mPlaylist != null && mPlaylist.isMutable()) {
			menu.findItem(R.id.option_remove).setVisible(true).setEnabled(true);
			menu.findItem(R.id.option_delete).setVisible(false).setEnabled(false);
		}

		if (!mShowUnhide || getItem() == null || !getItem().isHidden())
			menu.findItem(R.id.option_unhide).setVisible(false).setEnabled(false);
	}

	@Override
	protected boolean onMenuItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.option_unhide:
                MusicLibrary.getInstance().queueUnhideSong(getItem());
				return true;
            case R.id.option_sub_show_artist: {
				if (getItem() != null)
					RequestManager.getInstance().pushRequest(new ShowArtistRequest(getItem().getInfo().getArtistId()));
				return true;
			}
            case R.id.option_sub_show_album: {
				if (getItem() != null)
					RequestManager.getInstance().pushRequest(new ShowAlbumRequest(getItem().getInfo().getAlbumId()));
				return true;
			}
            case R.id.option_sub_show_folder: {
				if (getItem() != null)
                	RequestManager.getInstance().pushRequest(new ShowFolderRequest(getItem().getInfo().getFolderId()));
                return true;
            }
			case R.id.option_remove: {
				if (getItem() != null) {
					RemoveFromPlaylistDialog.newInstance(getItem().getId(), mPlaylist.getId()).show(getChildFragmentManager(), TAG_REMOVE_DIALOG);
				}
				return true;
			}
		}

		return super.onMenuItemSelected(item);
	}

	@Override
	protected boolean getInformationMessage(StringBuilder builder, boolean loaded) {
		String loadingString = getLoadingString();

		Song.Info info = getItem().getInfo();
		addTextInfo(builder, R.string.tags_artist, info.getArtist());
		addTextInfo(builder, R.string.tags_album_artist, info.getAlbumArtist());
		addTextInfo(builder, R.string.tags_album, info.getAlbum());
		addTextInfo(builder, R.string.tags_genre, info.getGenre());
		addNumberInfo(builder, R.string.tags_track_number, info.getTitleNumber(), info.getNumTitles());
		addNumberInfo(builder, R.string.tags_disc_number, info.getDiscNumber(), info.getNumDiscs());
		if (info.getYear() > 0)
			addTextInfo(builder, R.string.tags_year, String.valueOf(info.getYear()));
		addTextInfo(builder, R.string.info_duration, TimeUtil.durationToString(info.getDuration()));
		addTextInfo(builder, R.string.info_file_path, info.getFilePath());

		String bitrate = loaded ? String.format(Locale.getDefault(), "%d kbps", mBitrate) : loadingString;
		addTextInfo(builder, R.string.info_bitrate, bitrate);

		String sampleRate = loaded ? String.format(Locale.getDefault(), "%d Hz", mSampleRate) : loadingString;
		addTextInfo(builder, R.string.info_sample_rate, sampleRate);

		String channelsValue;
		if (!loaded)
			channelsValue = loadingString;
		else {
			switch (mChannels) {
				case 1:
					channelsValue = "mono";
					break;
				case 2:
					channelsValue = "stereo";
					break;
				default:
					channelsValue = String.valueOf(mChannels);
					break;
			}
		}
		addTextInfo(builder, R.string.info_channels, channelsValue);

		String cover;
		if (!loaded)
			cover = loadingString;
		else {
			String coverType;
			switch (mCoverType) {
				case BMP:
					coverType = "bmp";
					break;
				case GIF:
					coverType = "gif";
					break;
				case PNG:
					coverType = "png";
					break;
				case JPEG:
					coverType = "jpeg";
					break;
				default:
					coverType = "n/a";
					break;
			}

			if (mCoverType != MediaTag.ImageType.Unknown)
				cover = String.format(Locale.getDefault(), "%s %dx%d", coverType, mCoverWidth, mCoverHeight);
			else
				cover = coverType;
		}
		addTextInfo(builder, R.string.info_cover, cover);

		mBitrate = -1;
		mSampleRate = -1;
		mChannels = -1;
		mCoverType = MediaTag.ImageType.Unknown;
		mCoverWidth = -1;
		mCoverHeight = -1;

		return true;
	}

	@Override
	protected void loadInformation() {
		MediaTag tag = new MediaTag();

		if (tag.open(getItem().getInfo().getFilePath())) {

			mBitrate = tag.getProperty(MediaTag.Properties.Bitrate);
			mSampleRate = tag.getProperty(MediaTag.Properties.Samplerate);
			mChannels = tag.getProperty(MediaTag.Properties.Channels);

			mCoverType = tag.getAlbumArtType();
			mCoverWidth = tag.getAlbumArtWidth();
			mCoverHeight = tag.getAlbumArtHeight();
			tag.close();
		}

		tag.release();
	}
}
