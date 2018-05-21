//
// Created by Tarik Karaca on 28.02.16.
//

#include "CMediaTag.h"
#include "CMediaContext.h"
#include "log.h"

#include "taglib/toolkit/tstring.h"
#include "taglib/toolkit/tfilestream.h"

#include "taglib/ape/apefile.h"
#include "taglib/asf/asffile.h"
#include "taglib/flac/flacfile.h"
#include "taglib/it/itfile.h"
#include "taglib/mod/modfile.h"
#include "taglib/mp4/mp4file.h"
#include "taglib/mpc/mpcfile.h"
#include "taglib/mpeg/mpegfile.h"
#include "taglib/ogg/flac/oggflacfile.h"
#include "taglib/ogg/opus/opusfile.h"
#include "taglib/ogg/speex/speexfile.h"
#include "taglib/ogg/vorbis/vorbisfile.h"
#include "taglib/riff/aiff/aifffile.h"
#include "taglib/riff/wav/wavfile.h"
#include "taglib/s3m/s3mfile.h"
#include "taglib/trueaudio/trueaudiofile.h"
#include "taglib/wavpack/wavpackfile.h"
#include "taglib/xm/xmfile.h"

#include "taglib/ape/apetag.h"
#include "taglib/asf/asftag.h"
#include "taglib/mod/modtag.h"
#include "taglib/mp4/mp4tag.h"
#include "taglib/mpeg/id3v1/id3v1tag.h"
#include "taglib/mpeg/id3v2/id3v2tag.h"
#include "taglib/ogg/xiphcomment.h"
#include "taglib/riff/wav/infotag.h"

#include "taglib/mpeg/id3v2/frames/attachedpictureframe.h"
#include "taglib/mp4/mp4coverart.h"
#include "taglib/flac/flacpicture.h"

#define PASTE2(a,b) a##b
#define PASTE(a,b) PASTE2(a,b)
#define TAG(type) PASTE(type, Tag)

#define metaFuncSig(type) TString PASTE(getMetadata, type)(TAG(type)* tag, CMediaTag::MetaKeys key)
#define aArtTypeSig(type) CMediaTag::ImageType PASTE(getAArtType, type)(TAG(type)* tag)
#define aArtDataSig(type) TByteVector PASTE(getAArtData, type)(TAG(type)* tag)
#define sMetaFuncSig(type) int PASTE(setMetadata, type)(TAG(type)* tag, CMediaTag::MetaKeys key, const TString& value)
#define sAArtSig(type) int PASTE(setAArt, type)(TAG(type)* tag, const TByteVector& data, CMediaTag::ImageType imageType)

#define declTagType(type) metaFuncSig(type); \
                        aArtTypeSig(type); \
                        aArtDataSig(type); \
                        sMetaFuncSig(type); \
                        sAArtSig(type);

typedef TagLib::String TString;
typedef TagLib::ByteVector TByteVector;

typedef TagLib::APE::File APEFile;
typedef TagLib::ASF::File ASFFile;
typedef TagLib::FLAC::File FLACFile;
typedef TagLib::IT::File ITFile;
typedef TagLib::Mod::File MODFile;
typedef TagLib::MP4::File MP4File;
typedef TagLib::MPC::File MPCFile;
typedef TagLib::MPEG::File MPEGFile;
typedef TagLib::Ogg::FLAC::File OggFlacFile;
typedef TagLib::Ogg::Opus::File OpusFile;
typedef TagLib::Ogg::Speex::File SpeexFile;
typedef TagLib::Vorbis::File VorbisFile;
typedef TagLib::RIFF::AIFF::File AIFFFile;
typedef TagLib::RIFF::WAV::File WAVFile;
typedef TagLib::S3M::File S3MFile;
typedef TagLib::TrueAudio::File TrueAudioFile;
typedef TagLib::WavPack::File WavPackFile;
typedef TagLib::XM::File XMFile;

typedef TagLib::APE::Tag APETag;
typedef TagLib::ASF::Tag ASFTag;
typedef TagLib::ID3v1::Tag ID3v1Tag;
typedef TagLib::ID3v2::Tag ID3v2Tag;
typedef TagLib::Mod::Tag MODTag;
typedef TagLib::MP4::Tag MP4Tag;
typedef TagLib::RIFF::Info::Tag RIFFTag;
typedef TagLib::Ogg::XiphComment XiphTag;

typedef TagLib::FLAC::Picture FLACPicture;

CMediaTag::ImageType fromMimeType(const TString& mime);
TString toMimeType(CMediaTag::ImageType type);

declTagType(APE);
declTagType(ASF);
declTagType(ID3v1);
declTagType(ID3v2);
declTagType(MOD);
declTagType(MP4);
declTagType(RIFF);
declTagType(Xiph);

#define retFileFunc(func, ...) \
switch (m_fileType) { \
    case FileType::MPEG: { \
        MPEGFile* temp = reinterpret_cast<MPEGFile*>(m_file); \
        if (temp->hasID3v2Tag()) \
            return func##ID3v2(temp->ID3v2Tag(), ##__VA_ARGS__); \
        \
        if (temp->hasAPETag()) \
            return func##APE(temp->APETag(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v1Tag()) \
            return func##ID3v1(temp->ID3v1Tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::MP4: { \
        MP4File* temp = reinterpret_cast<MP4File*>(m_file); \
        MP4Tag* mp4 = temp->tag(); \
        \
        if (!mp4) \
            break; \
        \
        return func##MP4(mp4, ##__VA_ARGS__); \
    } \
    case FileType::Vorbis: { \
        VorbisFile* temp = reinterpret_cast<VorbisFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        return func##Xiph(xiph, ##__VA_ARGS__); \
    } \
    case FileType::OGGFLAC: { \
        OggFlacFile* temp = reinterpret_cast<OggFlacFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        return func##Xiph(xiph, ##__VA_ARGS__); \
    } \
    case FileType::FLAC: { \
        FLACFile* temp = reinterpret_cast<FLACFile*>(m_file); \
        if (temp->hasXiphComment()) \
            return func##Xiph(temp->xiphComment(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v2Tag()) \
            return func##ID3v2(temp->ID3v2Tag(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v1Tag()) \
            return func##ID3v1(temp->ID3v1Tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::MPC: { \
        MPCFile* temp = reinterpret_cast<MPCFile*>(m_file); \
        if (temp->hasAPETag()) \
            return func##APE(temp->APETag(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v1Tag()) \
            return func##ID3v1(temp->ID3v1Tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::WavPack: { \
        WavPackFile* temp = reinterpret_cast<WavPackFile*>(m_file); \
        if (temp->hasAPETag()) \
            return func##APE(temp->APETag(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v1Tag()) \
            return func##ID3v1(temp->ID3v1Tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::SPEEX: { \
        SpeexFile* temp = reinterpret_cast<SpeexFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        return func##Xiph(xiph, ##__VA_ARGS__); \
    } \
    case FileType::OPUS: { \
        OpusFile* temp = reinterpret_cast<OpusFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        return func##Xiph(xiph, ##__VA_ARGS__); \
    } \
    case FileType::TrueAudio: { \
        TrueAudioFile* temp = reinterpret_cast<TrueAudioFile*>(m_file); \
        if (temp->hasID3v2Tag()) \
            return func##ID3v2(temp->ID3v2Tag(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v1Tag()) \
            return func##ID3v1(temp->ID3v1Tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::ASF: { \
        ASFFile* temp = reinterpret_cast<ASFFile*>(m_file); \
        ASFTag* asf = temp->tag(); \
        \
        if (!asf) \
            break; \
        \
        return func##ASF(asf, ##__VA_ARGS__); \
    } \
    case FileType::AIFF: { \
        AIFFFile* temp = reinterpret_cast<AIFFFile*>(m_file); \
        if (temp->hasID3v2Tag()) \
            return func##ID3v2(temp->tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::WAV: { \
        WAVFile* temp = reinterpret_cast<WAVFile*>(m_file); \
        if (temp->hasID3v2Tag()) \
            return func##ID3v2(temp->ID3v2Tag(), ##__VA_ARGS__); \
        \
        if (temp->hasInfoTag()) \
            return func##RIFF(temp->InfoTag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::APE: { \
        APEFile* temp = reinterpret_cast<APEFile*>(m_file); \
        if (temp->hasAPETag()) \
            return func##APE(temp->APETag(), ##__VA_ARGS__); \
        \
        if (temp->hasID3v1Tag()) \
            return func##ID3v1(temp->ID3v1Tag(), ##__VA_ARGS__); \
        \
        break; \
    } \
    case FileType::MOD: { \
        MODFile* temp = reinterpret_cast<MODFile*>(m_file); \
        \
        return func##MOD(temp->tag(), ##__VA_ARGS__); \
    } \
    case FileType::S3M: { \
        S3MFile* temp = reinterpret_cast<S3MFile*>(m_file); \
        \
        return func##MOD(temp->tag(), ##__VA_ARGS__); \
    } \
    case FileType::IT: { \
        ITFile* temp = reinterpret_cast<ITFile*>(m_file); \
        \
        return func##MOD(temp->tag(), ##__VA_ARGS__); \
    } \
    case FileType::XM: { \
        XMFile* temp = reinterpret_cast<XMFile*>(m_file); \
        \
        return func##MOD(temp->tag(), ##__VA_ARGS__); \
    } \
    default: \
        break; \
}

#define setFileFunc(func, ...) \
switch (m_fileType) { \
    case FileType::MPEG: { \
        MPEGFile* temp = reinterpret_cast<MPEGFile*>(m_file); \
        func##ID3v2(temp->ID3v2Tag(true), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::MP4: { \
        MP4File* temp = reinterpret_cast<MP4File*>(m_file); \
        MP4Tag* mp4 = temp->tag(); \
        \
        if (!mp4) \
            break; \
        \
        func##MP4(mp4, ##__VA_ARGS__); \
        break;\
    } \
    case FileType::Vorbis: { \
        VorbisFile* temp = reinterpret_cast<VorbisFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        func##Xiph(xiph, ##__VA_ARGS__); \
        break;\
    } \
    case FileType::OGGFLAC: { \
        OggFlacFile* temp = reinterpret_cast<OggFlacFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        func##Xiph(xiph, ##__VA_ARGS__);\
        break; \
    } \
    case FileType::FLAC: { \
        FLACFile* temp = reinterpret_cast<FLACFile*>(m_file); \
        func##Xiph(temp->xiphComment(true), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::MPC: { \
        MPCFile* temp = reinterpret_cast<MPCFile*>(m_file); \
        func##APE(temp->APETag(true), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::WavPack: { \
        WavPackFile* temp = reinterpret_cast<WavPackFile*>(m_file); \
        func##APE(temp->APETag(true), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::SPEEX: { \
        SpeexFile* temp = reinterpret_cast<SpeexFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        func##Xiph(xiph, ##__VA_ARGS__); \
        break; \
    } \
    case FileType::OPUS: { \
        OpusFile* temp = reinterpret_cast<OpusFile*>(m_file); \
        XiphTag* xiph = temp->tag(); \
        \
        if (!xiph) \
            break; \
        \
        func##Xiph(xiph, ##__VA_ARGS__); \
        break; \
    } \
    case FileType::TrueAudio: { \
        TrueAudioFile* temp = reinterpret_cast<TrueAudioFile*>(m_file); \
        func##ID3v2(temp->ID3v2Tag(true), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::ASF: { \
        ASFFile* temp = reinterpret_cast<ASFFile*>(m_file); \
        ASFTag* asf = temp->tag(); \
        \
        if (!asf) \
            break; \
        \
        func##ASF(asf, ##__VA_ARGS__); \
        break; \
    } \
    case FileType::AIFF: { \
        AIFFFile* temp = reinterpret_cast<AIFFFile*>(m_file); \
        func##ID3v2(temp->tag(), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::WAV: { \
        WAVFile* temp = reinterpret_cast<WAVFile*>(m_file); \
        func##ID3v2(temp->ID3v2Tag(), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::APE: { \
        APEFile* temp = reinterpret_cast<APEFile*>(m_file); \
        func##APE(temp->APETag(true), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::MOD: { \
        MODFile* temp = reinterpret_cast<MODFile*>(m_file); \
        func##MOD(temp->tag(), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::S3M: { \
        S3MFile* temp = reinterpret_cast<S3MFile*>(m_file); \
        func##MOD(temp->tag(), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::IT: { \
        ITFile* temp = reinterpret_cast<ITFile*>(m_file); \
        func##MOD(temp->tag(), ##__VA_ARGS__); \
        break; \
    } \
    case FileType::XM: { \
        XMFile* temp = reinterpret_cast<XMFile*>(m_file); \
        func##MOD(temp->tag(), ##__VA_ARGS__); \
        break; \
    } \
    default: \
        break; \
}

#define openFile(arg) { \
m_fileType = type; \
\
switch(type) { \
    case MPEG: \
        m_file = new MPEGFile(arg, TagLib::ID3v2::FrameFactory::instance()); \
        break; \
    case MP4: \
        m_file = new MP4File(arg); \
        break; \
    case AAC: \
        m_file = new MP4File(arg); \
        m_fileType = FileType::MP4; \
\
        if (!m_file->isValid()) { \
            delete m_file; \
            m_file = NULL; \
            \
            m_fileType = FileType::ADTS; \
        } \
        \
        break; \
    case Vorbis: \
        m_file = new VorbisFile(arg); \
        break; \
    case OGGFLAC: \
        m_file = new OggFlacFile(arg); \
        \
        if (!m_file->isValid()) { \
            delete m_file; \
            m_fileType = FileType::Vorbis; \
            m_file = new VorbisFile(arg); \
        } \
        \
        break; \
    case FLAC: \
        m_file = new FLACFile(arg, TagLib::ID3v2::FrameFactory::instance()); \
        break; \
    case MPC: \
        m_file = new MPCFile(arg); \
        break; \
    case WavPack: \
        m_file = new WavPackFile(arg); \
        break; \
    case SPEEX: \
        m_file = new SpeexFile(arg); \
        break; \
    case OPUS: \
        m_file = new OpusFile(arg); \
        break; \
    case TrueAudio: \
        m_file = new TrueAudioFile(arg); \
        break; \
    case ASF: \
        m_file = new ASFFile(arg); \
        break; \
    case AIFF: \
        m_file = new AIFFFile(arg); \
        break; \
    case WAV: \
        m_file = new WAVFile(arg); \
        break; \
    case APE: \
        m_file = new APEFile(arg); \
        break; \
    case MOD: \
        m_file = new MODFile(arg); \
        break; \
    case S3M: \
        m_file = new S3MFile(arg); \
        break; \
    case IT: \
        m_file = new ITFile(arg); \
        break; \
    case XM: \
        m_file = new XMFile(arg); \
        break; \
} }

CMediaTag::CMediaTag() : m_fileType(None),
                        m_filePath(),
                        m_stream(NULL),
                        m_file(NULL),
                        m_mediaContext(NULL),
                        m_albumArtContext(NULL) {

}

CMediaTag::~CMediaTag() {
    close();
}

int CMediaTag::open(CMediaTag::FileType type, const char* filePath) {
    openFile(filePath);

    if (m_fileType == FileType::ADTS) {
        m_mediaContext = new CMediaContext();

        if (m_mediaContext->open(filePath) < 0)
            close();
    }

    bool isValid = (m_file && m_file->isValid()) || m_mediaContext;

    if (!isValid)
        close();

    return -!isValid;
}

int CMediaTag::open(CMediaTag::FileType type, int fileDescriptor) {

    m_stream = new TagLib::FileStream(fileDescriptor);

    openFile(m_stream);

    bool isValid = (m_file && m_file->isValid());

    if (!isValid)
        close();

    return -!isValid;
}

void CMediaTag::close() {
    m_filePath = TString();
    m_fileType = FileType::None;

    if (m_albumArtContext) {
        m_albumArtContext->close();
        m_albumArtContext->release();
        delete m_albumArtContext;
        m_albumArtContext = NULL;
    }

    if (m_mediaContext) {
        m_mediaContext->close();
        m_mediaContext->release();
        delete m_mediaContext;
        m_mediaContext = NULL;
    }

    if (m_file) {
        delete m_file;
        m_file = NULL;
    }

    if (m_stream) {
        delete m_stream;
        m_stream = NULL;
    }
}

TagLib::String& CMediaTag::getFilePath() {
    return m_filePath;
}

TagLib::String CMediaTag::getMetadata(MetaKeys key) {

    retFileFunc(getMetadata, key);

    return TString();
}

int64_t CMediaTag::getProperty(PropKeys key) {
    switch (m_fileType) {
        case FileType::None: {
            break;
        }
        case FileType::ADTS: {
            switch (key) {
                case PropKeys::Duration:
                    return m_mediaContext->getDuration();
                case PropKeys::Bitrate:
                    return m_mediaContext->getBitrate() / 1000;
                case PropKeys::Samplerate:
                    return m_mediaContext->getSampleRate();
                case PropKeys::Channels:
                    return m_mediaContext->getChannels();
                default:
                    return 0;
            }

            break;
        }
        default: {
            if (!m_mediaContext) {
                m_mediaContext = new CMediaContext();

                if (m_mediaContext->open(m_filePath.toCString(true)) < 0) {
                    m_mediaContext->close();
                    m_mediaContext->release();
                    m_mediaContext = NULL;
                }
            }

            if (m_mediaContext) {
                switch (key) {
                    case PropKeys::Duration:
                        return m_mediaContext->getDuration();
                    case PropKeys::Bitrate:
                        return m_mediaContext->getBitrate() / 1000;
                    case PropKeys::Samplerate:
                        return m_mediaContext->getSampleRate();
                    case PropKeys::Channels:
                        return m_mediaContext->getChannels();
                }
            }

            TagLib::AudioProperties* props = m_file->audioProperties();

            if (!props)
                return 0;

            switch (key) {
                case PropKeys::Duration:
                    return props->lengthInMilliseconds();
                case PropKeys::Bitrate:
                    return props->bitrate();
                case PropKeys::Samplerate:
                    return props->sampleRate();
                case PropKeys::Channels:
                    return props->channels();
            }

            break;
        }
    }

    return 0;
}

CMediaTag::ImageType CMediaTag::getAlbumArtType() {

    retFileFunc(getAArtType);

    return ImageType::Unknown;
}

TagLib::ByteVector CMediaTag::getAlbumArtData() {

    retFileFunc(getAArtData);

    return TByteVector();
}

CMediaContext* CMediaTag::getAlbumArtContext() {
    if (!m_albumArtContext) {
        TagLib::ByteVector buffer = getAlbumArtData();

        if (buffer.isEmpty())
            return NULL;

        m_albumArtContext = new CMediaContext();
        if (m_albumArtContext->open((const uint8_t *)buffer.data(), buffer.size()) < 0) {
            m_albumArtContext->close();

            if (m_albumArtContext->open(m_filePath.toCString(true)) < 0) {
                m_albumArtContext->close();
                m_albumArtContext->release();
                delete m_albumArtContext;
                m_albumArtContext = NULL;
            }
        }
    }

    return m_albumArtContext;
}

int CMediaTag::setMetadata(MetaKeys key, TagLib::String& value) {

    setFileFunc(setMetadata, key, value);

    return 0;
}

int CMediaTag::setAlbumArtData(TagLib::ByteVector& data, ImageType type) {

    setFileFunc(setAArt, data, type);

    return 0;
}

int CMediaTag::removeAlbumArt() {

    setFileFunc(setAArt, TByteVector(), ImageType::Unknown);

    return 0;
}

bool CMediaTag::save() {
    switch (m_fileType) {
        case FileType::None:
        case FileType::ADTS:
            break;
        default:
            return m_file->save();
    }

    return false;
}

CMediaTag::ImageType fromMimeType(const TagLib::String& mime) {
    int index = mime.rfind("/");

    if (index != -1) {
        TagLib::String ext = mime.substr(index + 1).upper();
        if (ext == "PNG")
            return CMediaTag::ImageType::PNG;
        else if (ext == "JPEG")
            return CMediaTag::ImageType::JPEG;
        else if (ext == "BMP")
            return CMediaTag::ImageType::BMP;
        else if (ext == "GIF")
            return CMediaTag::ImageType::GIF;
    }

    return CMediaTag::ImageType::Unknown;
}

TagLib::String toMimeType(CMediaTag::ImageType type) {
    TagLib::String result;

    switch (type) {
        case CMediaTag::ImageType::JPEG:
            result = "image/jpeg";
            break;
        case CMediaTag::ImageType::PNG:
            result = "image/png";
            break;
        case CMediaTag::ImageType::BMP:
            result = "image/bmp";
            break;
        case CMediaTag::ImageType::GIF:
            result = "image/gif";
            break;
        case CMediaTag::ImageType::Unknown:
            break;
    }

    return result;
}

//
// APE
//
metaFuncSig(APE) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::AlbumArtist: {
            TagLib::APE::Item item = tag->itemListMap()["ALBUM ARTIST"];
            if (!item.isEmpty())
                return item.toString();

            item = tag->itemListMap()["ALBUMARTIST"];
            if (!item.isEmpty())
                return item.toString();

            break;
        }
        case CMediaTag::MetaKeys::TrackNumber: {
            TagLib::APE::Item item = tag->itemListMap()["TRACK"];
            if (!item.isEmpty())
                return item.toString();

            break;
        }
        case CMediaTag::MetaKeys::DiscNumber: {
            TagLib::APE::Item item = tag->itemListMap()["DISC"];
            if (!item.isEmpty())
                return item.toString();

            break;
        }
        case CMediaTag::MetaKeys::Year: {
            TagLib::APE::Item item = tag->itemListMap()["YEAR"];
            if (!item.isEmpty())
                return item.toString();

            break;
        }
    }

    return TString();
}

aArtTypeSig(APE) {
    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(APE) {
    return TByteVector();
}

sMetaFuncSig(APE) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
            tag->removeItem("ALBUMARTIST");

            tag->addValue("ALBUM ARTIST", value, true);
            break;
        case CMediaTag::MetaKeys::TrackNumber:
            tag->addValue("TRACK", value, true);
            break;
        case CMediaTag::MetaKeys::DiscNumber:
            tag->addValue("DISC", value, true);
            break;
        case CMediaTag::MetaKeys::Year:
            tag->addValue("YEAR", value, true);
            break;
    }

    return 0;
}

sAArtSig(APE) {
    // not supported
    return 0;
}

//
// ASF
//
metaFuncSig(ASF) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::AlbumArtist:
            if (tag->attributeListMap().contains("WM/AlbumArtist"))
                return tag->attributeListMap()["WM/AlbumArtist"][0].toString();

            break;
        case CMediaTag::MetaKeys::TrackNumber:
            if (tag->attributeListMap().contains("WM/TrackNumber"))
                return tag->attributeListMap()["WM/TrackNumber"][0].toString();

            if (tag->attributeListMap().contains("WM/Track"))
                return tag->attributeListMap()["WM/Track"][0].toString();

            break;
        case CMediaTag::MetaKeys::DiscNumber:
            if (tag->attributeListMap().contains("WM/PartOfSet"))
                return tag->attributeListMap()["WM/PartOfSet"][0].toString();

            break;
        case CMediaTag::MetaKeys::Year:
            if (tag->attributeListMap().contains("WM/Year"))
                return tag->attributeListMap()["WM/Year"][0].toString();

            break;
    }

    return TString();
}

aArtTypeSig(ASF) {
    if (!tag->attributeListMap().contains("WM/Picture"))
        return CMediaTag::ImageType::Unknown;

    return fromMimeType(tag->attributeListMap()["WM/Picture"][0].toPicture().mimeType());
}

aArtDataSig(ASF) {
    if (!tag->attributeListMap().contains("WM/Picture"))
        return TByteVector();

    return tag->attributeListMap()["WM/Picture"][0].toPicture().picture();
}

sMetaFuncSig(ASF) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
            if (value.isEmpty())
                tag->removeItem("WM/AlbumArtist");
            else
                tag->setAttribute("WM/AlbumArtist", value);
            break;
        case CMediaTag::MetaKeys::TrackNumber:
            tag->removeItem("WM/Track");

            if (value.isEmpty())
                tag->removeItem("WM/TrackNumber");
            else
                tag->setAttribute("WM/TrackNumber", value);

            break;
        case CMediaTag::MetaKeys::DiscNumber:
            if (value.isEmpty())
                tag->removeItem("WM/PartOfSet");
            else
                tag->setAttribute("WM/PartOfSet", value);

            break;
        case CMediaTag::MetaKeys::Year:
            if (value.isEmpty())
                tag->removeItem("WM/Year");
            else
                tag->setAttribute("WM/Year", value);
            break;
    }

    return 0;
}

sAArtSig(ASF) {

    if (data.isEmpty()) {
        tag->removeItem("WM/Picture");
        return 0;
    }

    TagLib::ASF::Picture picture;
    picture.setMimeType(toMimeType(imageType));
    picture.setType(TagLib::ASF::Picture::Type::Media);
    picture.setPicture(data);

    tag->setAttribute("WM/Picture", picture);
    return 0;
}

//
// ID3v1
//
metaFuncSig(ID3v1) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::TrackNumber:
            return TString::number(tag->track());
        case CMediaTag::MetaKeys::AlbumArtist:
        case CMediaTag::MetaKeys::DiscNumber:
            break;
        case CMediaTag::MetaKeys::Year:
            return TString::number(tag->year());
    }

    return TString();
}

aArtTypeSig(ID3v1) {
    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(ID3v1) {
    return TByteVector();
}

sMetaFuncSig(ID3v1) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::TrackNumber:
            if (value.isEmpty())
                tag->setTrack(0);
            else
                tag->setTrack((unsigned int)value.toInt());
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
        case CMediaTag::MetaKeys::DiscNumber:
            break;
        case CMediaTag::MetaKeys::Year:
            if (value.isEmpty())
                tag->setYear(0);
            else
                tag->setYear((unsigned int)value.toInt());
            break;
    }

    return 0;
}

sAArtSig(ID3v1) {
    // not supported
    return 0;
}

//
// ID3v2
//
metaFuncSig(ID3v2) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::AlbumArtist: {
            TagLib::ID3v2::FrameList frameList = tag->frameListMap()["TPE2"];
            if (!frameList.isEmpty())
                return frameList.front()->toString();
            break;
        }
        case CMediaTag::MetaKeys::TrackNumber: {
            TagLib::ID3v2::FrameList frameList = tag->frameListMap()["TRCK"];
            if (!frameList.isEmpty())
                return frameList.front()->toString();
            break;
        }
        case CMediaTag::MetaKeys::DiscNumber: {
            TagLib::ID3v2::FrameList frameList = tag->frameListMap()["TPOS"];
            if (!frameList.isEmpty())
                return frameList.front()->toString();
            break;
        }
        case CMediaTag::MetaKeys::Year:
            return TString::number(tag->year());
    }

    return TString();
}

aArtTypeSig(ID3v2) {
    TagLib::ID3v2::FrameList frameList = tag->frameListMap()["APIC"];
    if (!frameList.isEmpty()) {
        TagLib::ID3v2::AttachedPictureFrame* frame = reinterpret_cast<TagLib::ID3v2::AttachedPictureFrame*>(frameList.front());
        TagLib::String mimeType = frame->mimeType();

        return fromMimeType(mimeType);
    }

    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(ID3v2) {
    TagLib::ID3v2::FrameList frameList = tag->frameListMap()["APIC"];
    if (!frameList.isEmpty()) {
        TagLib::ID3v2::AttachedPictureFrame* frame = reinterpret_cast<TagLib::ID3v2::AttachedPictureFrame*>(frameList.front());
        return frame->picture();
    }

    return TByteVector();
}

sMetaFuncSig(ID3v2) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
            tag->setTextFrame("TPE2", value);
            break;
        case CMediaTag::MetaKeys::TrackNumber:
            tag->setTextFrame("TRCK", value);
            break;
        case CMediaTag::MetaKeys::DiscNumber:
            tag->setTextFrame("TPOS", value);
            break;
        case CMediaTag::MetaKeys::Year:
            tag->setTextFrame("TDRC", value);
            break;
    }

    return 0;
}

sAArtSig(ID3v2) {
    tag->removeFrames("APIC");

    if (data.isEmpty())
        return 0;

    TagLib::ID3v2::AttachedPictureFrame* frame = new TagLib::ID3v2::AttachedPictureFrame();

    frame->setMimeType(toMimeType(imageType));
    frame->setType(TagLib::ID3v2::AttachedPictureFrame::Type::Media);
    frame->setPicture(data);

    tag->addFrame(frame);
    return 0;
}

//
// MOD
//
metaFuncSig(MOD) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
        case CMediaTag::MetaKeys::Album:
        case CMediaTag::MetaKeys::Genre:
        case CMediaTag::MetaKeys::AlbumArtist:
        case CMediaTag::MetaKeys::TrackNumber:
        case CMediaTag::MetaKeys::DiscNumber:
        case CMediaTag::MetaKeys::Year:
            break;
    }

    return TString();
}

aArtTypeSig(MOD) {
    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(MOD) {
    return TByteVector();
}

sMetaFuncSig(MOD) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
        case CMediaTag::MetaKeys::Album:
        case CMediaTag::MetaKeys::Genre:
        case CMediaTag::MetaKeys::AlbumArtist:
        case CMediaTag::MetaKeys::TrackNumber:
        case CMediaTag::MetaKeys::DiscNumber:
        case CMediaTag::MetaKeys::Year:
            break;
    }

    return 0;
}

sAArtSig(MOD) {
    // not supported

    return 0;
}

//
// MP4
//
metaFuncSig(MP4) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::AlbumArtist: {
            if (tag->contains("aART"))
                return tag->item("aART").toStringList().toString(", ");

            break;
        }
        case CMediaTag::MetaKeys::TrackNumber: {
            if (tag->contains("trkn")) {
                TagLib::MP4::Item::IntPair pair = tag->item("trkn").toIntPair();

                TagLib::String ret = TagLib::String::number(pair.first);
                ret += "/";
                ret += TagLib::String::number(pair.second);
                return ret;
            }

            break;
        }
        case CMediaTag::MetaKeys::DiscNumber: {
            if (tag->contains("disk")) {
                TagLib::MP4::Item::IntPair pair = tag->item("disk").toIntPair();

                TagLib::String ret = TagLib::String::number(pair.first);
                ret += "/";
                ret += TagLib::String::number(pair.second);
                return ret;
            }

            break;
        }
        case CMediaTag::MetaKeys::Year:
            return TString::number(tag->year());
    }

    return TString();
}

aArtTypeSig(MP4) {
    if (tag->contains("covr")) {
        TagLib::MP4::CoverArtList list = tag->item("covr").toCoverArtList();

        if (!list.isEmpty()) {
            switch (list.front().format()) {
                case TagLib::MP4::CoverArt::Format::JPEG:
                    return CMediaTag::ImageType::JPEG;
                case TagLib::MP4::CoverArt::Format::PNG:
                    return CMediaTag::ImageType::PNG;
                case TagLib::MP4::CoverArt::Format::BMP:
                    return CMediaTag::ImageType::BMP;
                case TagLib::MP4::CoverArt::Format::GIF:
                    return CMediaTag::ImageType::GIF;
                default:
                    break;
            }
        }
    }

    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(MP4) {
    if (tag->contains("covr")) {
        TagLib::MP4::CoverArtList list = tag->item("covr").toCoverArtList();

        if (!list.isEmpty())
            return list.front().data();
    }

    return TByteVector();
}

sMetaFuncSig(MP4) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
            tag->setItem("aART", TagLib::StringList(value));
            break;
        case CMediaTag::MetaKeys::TrackNumber: {
            TagLib::StringList list = value.split("/");
            tag->setItem("trkn", TagLib::MP4::Item(list.front().toInt(), list.size() > 1 ? list.back().toInt() : 0));
            break;
        }
        case CMediaTag::MetaKeys::DiscNumber: {
            TagLib::StringList list = value.split("/");
            tag->setItem("disk", TagLib::MP4::Item(list.front().toInt(), list.size() > 1 ? list.back().toInt() : 0));
            break;
        }
        case CMediaTag::MetaKeys::Year:
            if (value.isEmpty())
                tag->removeItem("\251day");
            else
                tag->setItem("\251day", TagLib::StringList(value));
            break;
    }

    return 0;
}

sAArtSig(MP4) {
    if (data.isEmpty()) {
        tag->removeItem("covr");
        return 0;
    }

    TagLib::MP4::CoverArt::Format format = TagLib::MP4::CoverArt::Format::Unknown;
    switch (imageType) {
        case CMediaTag::ImageType::JPEG:
            format = TagLib::MP4::CoverArt::Format::JPEG;
            break;
        case CMediaTag::ImageType::PNG:
            format = TagLib::MP4::CoverArt::Format::PNG;
            break;
        case CMediaTag::ImageType::BMP:
            format = TagLib::MP4::CoverArt::Format::BMP;
            break;
        case CMediaTag::ImageType::GIF:
            format = TagLib::MP4::CoverArt::Format::GIF;
            break;
        default:
            break;
    }

    TagLib::MP4::CoverArt coverArt(format, data);
    TagLib::MP4::CoverArtList list;
    list.append(coverArt);

    tag->setItem("covr", TagLib::MP4::Item(list));

    return 0;
}

//
// RIFF
//
metaFuncSig(RIFF) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::TrackNumber:
            return tag->fieldText("IPRT");
        case CMediaTag::MetaKeys::AlbumArtist:
        case CMediaTag::MetaKeys::DiscNumber:
            break;
        case CMediaTag::MetaKeys::Year:
            return tag->fieldText("ICRD").substr(0, 4);
    }

    return TString();
}

aArtTypeSig(RIFF) {
    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(RIFF) {
    return TByteVector();
}

sMetaFuncSig(RIFF) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::TrackNumber:
            tag->setFieldText("IPRT", value);
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
        case CMediaTag::MetaKeys::DiscNumber:
            break;
        case CMediaTag::MetaKeys::Year:
            tag->setFieldText("ICRD", value);
            break;
    }

    return 0;
}

sAArtSig(RIFF) {
    // not supported

    return 0;
}

//
// Xiph
//
metaFuncSig(Xiph) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            return tag->title();
        case CMediaTag::MetaKeys::Artist:
            return tag->artist();
        case CMediaTag::MetaKeys::Album:
            return tag->album();
        case CMediaTag::MetaKeys::Genre:
            return tag->genre();
        case CMediaTag::MetaKeys::AlbumArtist: {
            TagLib::StringList list = tag->fieldListMap()["ALBUMARTIST"];
            if (!list.isEmpty())
                return list.front();

            break;
        }
        case CMediaTag::MetaKeys::TrackNumber: {
            TagLib::StringList list = tag->fieldListMap()["TRACKNUMBER"];
            if (!list.isEmpty())
                return list.front();

            break;
        }
        case CMediaTag::MetaKeys::DiscNumber: {
            TagLib::StringList list = tag->fieldListMap()["DISCNUMBER"];
            if (!list.isEmpty())
                return list.front();

            break;
        }
        case CMediaTag::MetaKeys::Year:
            return TString::number(tag->year());
    }

    return TString();
}

aArtTypeSig(Xiph) {
    TagLib::List<FLACPicture*> list = tag->pictureList();
    if (!list.isEmpty())
        return fromMimeType(list.front()->mimeType());

    return CMediaTag::ImageType::Unknown;
}

aArtDataSig(Xiph) {
    TagLib::List<FLACPicture*> list = tag->pictureList();
    if (!list.isEmpty())
        return list.front()->data();

    return TByteVector();
}

sMetaFuncSig(Xiph) {
    switch (key) {
        case CMediaTag::MetaKeys::Title:
            tag->setTitle(value);
            break;
        case CMediaTag::MetaKeys::Artist:
            tag->setArtist(value);
            break;
        case CMediaTag::MetaKeys::Album:
            tag->setAlbum(value);
            break;
        case CMediaTag::MetaKeys::Genre:
            tag->setGenre(value);
            break;
        case CMediaTag::MetaKeys::AlbumArtist:
            tag->addField("ALBUMARTIST", value);
            break;
        case CMediaTag::MetaKeys::TrackNumber:
            tag->removeFields("TRACKNUM");
            tag->addField("TRACKNUMBER", value);
            break;
        case CMediaTag::MetaKeys::DiscNumber:
            tag->addField("DISCNUMBER", value);
            break;
        case CMediaTag::MetaKeys::Year:
            if (value.isEmpty())
                tag->setYear(0);
            else
                tag->setYear((unsigned int)value.toInt());
            break;
    }

    return 0;
}

sAArtSig(Xiph) {
    tag->removeAllPictures();

    if (data.isEmpty())
        return 0;

    FLACPicture* picture = new FLACPicture();

    picture->setMimeType(toMimeType(imageType));
    picture->setType(FLACPicture::Type::Media);
    picture->setData(data);

    tag->addPicture(picture);

    return 0;
}
