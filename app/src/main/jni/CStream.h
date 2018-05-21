//
// Created by Tarik Karaca on 21.01.16.
//

#ifndef MUSICPLAYER_CSTREAM_H
#define MUSICPLAYER_CSTREAM_H

extern "C" {

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"

};

class CFrame;

class CStream {

public:

    CStream();
    ~CStream();

    int initialize(AVStream*);
    void release();

    int openCodec();
    int closeCodec();

    CFrame* getCachedFrame(bool create = false);

    const char* getCodecName() const;
    bool isCodecMpeg() const;
    AVCodecContext* getContext() const;
    int getIndex() const;
    int getDataType() const;

    int64_t getDuration() const;
    int getSampleRate() const;
    int64_t getBitrate() const;
    int getChannels() const;

    int getWidth() const;
    int getHeight() const;

    /*
     * Convert time from stream time base to milliseconds
     */
    int64_t fromTimeBase(int64_t time) const;

    /*
     * Convert time from milliseconds to stream time base
     */
    int64_t toTimeBase(int64_t time) const;

public:

    bool m_codecOpened;
    bool m_hasFrame;
    AVCodec* m_codec;
    AVStream* m_stream;
    CFrame* m_lastFrame;
    int m_type;

};

inline const char* CStream::getCodecName() const {
    return getContext()->codec->name;
}

inline bool CStream::isCodecMpeg() const {
    return m_codec->id == AV_CODEC_ID_MP3;
}

inline AVCodecContext* CStream::getContext() const {
    return m_stream->codec;
}

inline int CStream::getIndex() const {
    return m_stream->index;
}

inline int CStream::getDataType() const {
    return m_type;
}

inline int64_t CStream::getDuration() const {
    return fromTimeBase(m_stream->duration);
}

inline int CStream::getSampleRate() const {
    return getContext()->sample_rate;
}

inline int64_t CStream::getBitrate() const {
    return getContext()->bit_rate;
}

inline int CStream::getChannels() const {
    return getContext()->channels;
}

inline int CStream::getWidth() const {
    return getContext()->width;
}

inline int CStream::getHeight() const {
    return getContext()->height;
}

#endif //MUSICPLAYER_CSTREAM_H
