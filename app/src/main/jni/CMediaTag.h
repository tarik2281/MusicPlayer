//
// Created by Tarik Karaca on 28.02.16.
//

#ifndef MUSICPLAYER_CMEDIATAG_H
#define MUSICPLAYER_CMEDIATAG_H

#include <toolkit/tfilestream.h>
#include "taglib/toolkit/tfile.h"
#include "taglib/toolkit/tstring.h"

class CMediaContext;

class CMediaTag {

public:

    enum FileType {
        None, APE, ASF, FLAC, IT, MOD, MP4, MPC, MPEG,
        OGGFLAC, OPUS, SPEEX, Vorbis, AIFF, WAV, S3M,
        TrueAudio, WavPack, XM, AAC, ADTS
    };

    enum MetaKeys {
        Title,
        Artist,
        AlbumArtist,
        Album,
        Genre,
        TrackNumber,
        DiscNumber,
        Year
    };

    enum PropKeys {
        Duration,
        Bitrate,
        Samplerate,
        Channels
    };

    enum ImageType {
        JPEG, PNG, BMP, GIF, Unknown
    };

    CMediaTag();
    ~CMediaTag();

    int open(FileType type, const char* filePath);
    int open(FileType type, int fileDescriptor);
    void close();

    TagLib::String& getFilePath();
    TagLib::String getMetadata(MetaKeys key);
    int64_t getProperty(PropKeys key);
    ImageType getAlbumArtType();
    TagLib::ByteVector getAlbumArtData();
    CMediaContext* getAlbumArtContext();

    int setMetadata(MetaKeys key, TagLib::String& value);
    int setAlbumArtData(TagLib::ByteVector& data, ImageType type);
    int removeAlbumArt();

    // Returns true if succeeds
    bool save();

private:

    FileType m_fileType;

    TagLib::FileStream* m_stream;
    TagLib::String m_filePath;
    TagLib::File* m_file;

    CMediaContext* m_mediaContext;
    CMediaContext* m_albumArtContext;

};

#endif //MUSICPLAYER_CMEDIATAG_H
