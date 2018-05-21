//
// Created by Tarik Karaca on 21.01.16.
//

#include "CStream.h"
#include "CFrame.h"
#include "log.h"

extern "C" {

#include "libavutil/avutil.h"

}

CStream::CStream() : m_codecOpened(false),
                    m_hasFrame(false),
                    m_codec(NULL),
                    m_stream(NULL),
                    m_lastFrame(NULL),
                    m_type(0) {

}

CStream::~CStream() {

}

int CStream::initialize(AVStream * stream) {
    m_stream = stream;

    m_codecOpened = false;
    m_hasFrame = false;
    m_stream->codec->thread_count = 1;

    m_codec = avcodec_find_decoder(m_stream->codec->codec_id);
    if (!m_codec)
        return -1;

    switch (m_codec->type) {
        case AVMEDIA_TYPE_AUDIO:
            m_type = DATA_TYPE_AUDIO;
            break;
        case AVMEDIA_TYPE_VIDEO:
            m_type = DATA_TYPE_VIDEO;
            break;
        default:
            break;
    }

    return 0;
}

void CStream::release() {
    closeCodec();

    if (m_lastFrame) {
        m_lastFrame->release();
        delete m_lastFrame;
        m_lastFrame = NULL;
    }
}

int CStream::openCodec() {
    if (m_codecOpened)
        return 0;

    if (!m_stream || !m_codec || CheckFFmpegErr("CStream::openCodec", avcodec_open2(getContext(), m_codec, NULL)))
        return -1;

    m_codecOpened = true;
    return 0;
}

int CStream::closeCodec() {
    if (!m_codecOpened)
        return 0;

    if (!m_stream || !m_codec)
        return -1;

    avcodec_close(m_stream->codec);
    m_codecOpened = false;

    return 0;
}

CFrame* CStream::getCachedFrame(bool create) {
    if (!m_lastFrame)
        m_lastFrame = create ? new CFrame() : NULL;

    return m_lastFrame;
}

int64_t CStream::fromTimeBase(int64_t time) const {
    return av_rescale_q(time, m_stream->time_base, AV_TIME_BASE_Q) / 1000;
}

int64_t CStream::toTimeBase(int64_t time) const {
    return av_rescale_q(time * 1000, AV_TIME_BASE_Q, m_stream->time_base);
}
