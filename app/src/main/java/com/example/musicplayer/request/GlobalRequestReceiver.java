package com.example.musicplayer.request;

import com.example.musicplayer.library.LibraryObject;
import com.example.musicplayer.library.MusicLibrary;
import com.example.musicplayer.library.Playlist;
import com.example.musicplayer.library.Song;
import com.example.musicplayer.playback.PlaybackList;
import com.example.musicplayer.playback.PlaybackState;

/**
 * Created by Tarik on 12.06.2016.
 */
public class GlobalRequestReceiver implements RequestManager.Receiver {

    private static GlobalRequestReceiver sInstance;

    public static GlobalRequestReceiver getInstance() {
        if (sInstance == null)
            sInstance = new GlobalRequestReceiver();

        return sInstance;
    }

    private boolean mRegistered;

    private GlobalRequestReceiver() {

    }

    @Override
    public boolean onReceiveRequest(RequestManager.Request request) {
        int type = request.getType();

        if (type == PlaySongRequest.TYPE) {
            PlaySongRequest songRequest = (PlaySongRequest) request;
            PlaybackState state = PlaybackState.getInstance();

            boolean shuffle = false;

            switch (songRequest.getMode()) {
                case Shuffle:
                    shuffle = true;
                    break;
                case ByOrder:
                    shuffle = false;
                    break;
                case Retain:
                    shuffle = state.isShuffled();
                    break;
            }



            Song song;
            if (songRequest.getObjectType() == LibraryObject.PLAYLIST) {
                song = MusicLibrary.getInstance().getPlaylistById(songRequest.getObjectId())
                        .getSongList().get(songRequest.getSongId());
            }
            else {
                song = MusicLibrary.getInstance().getSongById(songRequest.getSongId());
            }

            if (song == null || !song.isHidden())
                state.setPlaybackList(PlaybackList.fromObject(songRequest.getObjectType(),
                        songRequest.getObjectId(), songRequest.getSorting(), songRequest.isSortingReversed(),
                        shuffle, songRequest.getSongId()), songRequest.getKeepQueue());
            else if (state.getCurrentSong() != null && state.getPlaybackList() != null) {
                state.getPlaybackList().addToHistoryLast(state.getCurrentSong());
            }

            if (song == null)
                state.nextSong(true);
            else
                state.setCurrentSong(song, true);

            state.setPlayingState(true);
        }
        //else if (type == ShowFolderRequest.TYPE) {
        //    FolderActivity.start(mContext, ((ShowFolderRequest)request).getFolderId());
        //    return true;
        //}
        //else if (type == ShowGenreRequest.TYPE) {
        //    GenreActivity.start(mContext, ((ShowGenreRequest)request).getGenreId());
        //    return true;
        //}
        //else if (type == ShowArtistRequest.TYPE) {
        //    ArtistActivity.start(mContext, ((ShowArtistRequest)request).getArtistId());
        //    return true;
        //}
        //else if (type == ShowAlbumRequest.TYPE) {
        //    AlbumActivity.start(mContext, ((ShowAlbumRequest)request).getAlbumId());
        //    return true;
        //}
        //else if (type == ShowPlaylistRequest.TYPE) {
        //    PlaylistActivity.start(mContext, ((ShowPlaylistRequest)request).getPlaylistId());
        //    return true;
        //}

        return false;
    }

    public void register() {
        if (!mRegistered) {
            mRegistered = true;
            RequestManager.getInstance().registerReceiver(this);
        }
    }

    public void unregister() {
        if (mRegistered) {
            mRegistered = false;
            RequestManager.getInstance().unregisterReceiver(this);
        }
    }
}
