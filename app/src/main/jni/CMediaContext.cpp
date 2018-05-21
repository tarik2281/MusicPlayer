//
// Created by Tarik Karaca on 01.03.16.
//

#include "CMediaContext.h"
#include "CFrame.h"
#include "CStream.h"
#include "log.h"

extern "C" {

#include "libavutil/mem.h"

}

#define BITS_PER_BYTE 8.0
#define READ_BUFFER_SIZE 4096
#define POSITION_NONE -1

CMediaContext::CMediaContext() : m_bufferStream(),
                                 m_avioContext(NULL),
                                 m_streams(NULL),
                                 m_numStreams(0),
                                 m_cacheFrames(false),
                                 m_audioIsMpeg(false),
                                 m_lastFrame(NULL),
                                 m_eofReached(false),
                                 m_inputType(None),
                                 m_startPos(POSITION_NONE),
                                 m_currentPosition(POSITION_NONE),
                                 m_audioBitrate(0),
                                 m_defAudioIndex(INDEX_NONE),
                                 m_defVideoIndex(INDEX_NONE),
                                 m_formatContext(NULL),
                                 m_isOpen(false),
                                 m_filePath() {

}

CMediaContext::~CMediaContext() {

}

int CMediaContext::open(const uint8_t *buffer, size_t size) {
    if (initIOContext() < 0)
        return -1;

    m_bufferStream.setSource(buffer, size);

    m_formatContext = avformat_alloc_context();
    if (!m_formatContext)
        return -1;

    m_formatContext->pb = m_avioContext;

    if (CheckFFmpegErr("CMediaContext::open", avformat_open_input(&m_formatContext, NULL, NULL, NULL)))
        return -1;

    m_inputType = InputType::BufferStream;

    if (readStreams() < 0) {
        avformat_close_input(&m_formatContext);
        return -1;
    }

    m_isOpen = true;
    
    return 0;
}

int CMediaContext::open(const char *filePath) {
    m_filePath = filePath;

    m_bufferStream.setNullSource();

    m_formatContext = NULL;

    if (CheckFFmpegErr("CMediaContext::open", avformat_open_input(&m_formatContext, filePath, NULL, NULL)))
        return -1;

    m_inputType = InputType::File;

    if (readStreams() < 0) {
        avformat_close_input(&m_formatContext);
        return -1;
    }

    m_isOpen = true;

    return 0;
}

void CMediaContext::close() {
    m_isOpen = false;

    std::string().swap(m_filePath);

    closeStreams();

    if (m_formatContext)
        avformat_close_input(&m_formatContext);

    m_defAudioIndex = INDEX_NONE;
    m_defVideoIndex = INDEX_NONE;
    m_startPos = POSITION_NONE;
    m_currentPosition = POSITION_NONE;
}

void CMediaContext::release() {
    close();

    releaseStreams();

    releaseIOContext();

    if (m_lastFrame) {
        m_lastFrame->release();
        delete m_lastFrame;
        m_lastFrame = NULL;
    }
}

bool CMediaContext::isOpen() {
    return m_isOpen;
}

const char* CMediaContext::getFilePath() const {
    return m_filePath.c_str();
}

int CMediaContext::seekBytes(int64_t offset) {
    if (!m_audioIsMpeg)
        return -1;

    readStartPos();

    CStream* stream = getStream(getDefaultAudioIndex());

    double bitrate = (double)stream->getBitrate();
    double pos = (double)offset / 1000.0 * bitrate / BITS_PER_BYTE;

    int res = av_seek_frame(m_formatContext, getDefaultAudioIndex(), m_startPos + (int64_t)pos, AVSEEK_FLAG_BYTE);

    if (CheckFFmpegErr("CMediaContext::seekBytes", res))
        return res;

    if (res >= 0)
        m_eofReached = false;

    avcodec_flush_buffers(stream->getContext());

    return res;
}

int CMediaContext::seekTime(int64_t offset) {
    CStream* stream = getStream(getDefaultAudioIndex());
    int64_t time = stream->toTimeBase(offset);

    int res = av_seek_frame(m_formatContext, getDefaultAudioIndex(), time, 0);

    if (CheckFFmpegErr("CMediaContext::seekTime", res))
        return res;

    if (res >= 0)
        m_eofReached = false;

    avcodec_flush_buffers(stream->getContext());

    return res;
}

int64_t CMediaContext::getCurrentPosition() {
    if (!m_audioIsMpeg)
        return m_currentPosition;

    if (m_audioBitrate == 0)
        m_audioBitrate = getStream(getDefaultAudioIndex())->getBitrate();

    double bitrate = (double)m_audioBitrate;
    double diff = (double)(m_currentPosition - m_startPos);

    return (int64_t)(diff / bitrate * BITS_PER_BYTE * 1000.0);
}

CFrame* CMediaContext::readNextFrame() {
    if (eof())
        return NULL;

    AVPacket pkt;
    pkt.data = NULL;
    pkt.size = 0;

    if (CheckFFmpegErr("CMediaContext::readNextFrame", av_read_frame(m_formatContext, &pkt))) {
        m_eofReached = true;
        return NULL;
    }

    int streamIndex = pkt.stream_index;

    CStream* stream = getStream(streamIndex);
    CFrame* frame = NULL;

    if (m_cacheFrames)
        frame = stream->getCachedFrame();
    else {
        if (!m_lastFrame)
            m_lastFrame = new CFrame();

        frame = m_lastFrame;
    }

    frame->releasePacket();

    if (m_audioIsMpeg) {
        m_currentPosition = pkt.pos;

        if (m_startPos == POSITION_NONE && stream->getDataType() == DATA_TYPE_AUDIO)
            m_startPos = m_currentPosition;
    }
    else
        m_currentPosition = stream->fromTimeBase(pkt.pts);

    frame->setPacket(&pkt);

    frame->init(stream);

    return frame;
}

bool CMediaContext::eof() const {
    return m_eofReached;
}

CStream* CMediaContext::getStream(int index) const {
    if (!m_streams || index < 0 || index > m_numStreams)
        return NULL;

    return &m_streams[index];
}

int CMediaContext::getNumStreams() const {
    return m_numStreams;
}

int64_t CMediaContext::getDuration() const {
    CStream* stream = getStream(getDefaultAudioIndex());

    return stream ? stream->getDuration() : -1;
}

int CMediaContext::getSampleRate() const {
    CStream* stream = getStream(getDefaultAudioIndex());

    return stream ? stream->getSampleRate() : -1;
}

int64_t CMediaContext::getBitrate() const {
    CStream* stream = getStream(getDefaultAudioIndex());

    return stream ? stream->getBitrate() : -1;
}

int CMediaContext::getChannels() const {
    CStream* stream = getStream(getDefaultAudioIndex());

    return stream ? stream->getChannels() : -1;
}

int CMediaContext::getDefaultAudioIndex() const {
    return m_defAudioIndex;
}

int CMediaContext::getDefaultVideoIndex() const {
    return m_defVideoIndex;
}

int CMediaContext::initIOContext() {
    if (m_avioContext)
        return 0;

    uint8_t* readBuffer = (uint8_t*)av_mallocz(READ_BUFFER_SIZE);

    m_avioContext = avio_alloc_context(readBuffer, READ_BUFFER_SIZE, 0, &m_bufferStream, CMediaContext::readPacket, NULL, CMediaContext::seek);

    return -(m_avioContext == NULL);
}

int CMediaContext::readStreams() {
    if (CheckFFmpegErr("CMediaContext::readStreams", avformat_find_stream_info(m_formatContext, NULL)))
        return -1;

    if (m_formatContext->nb_streams > getNumStreams()) {
        releaseStreams();

        m_numStreams = m_formatContext->nb_streams;
        m_streams = new CStream[getNumStreams()];
    }

    m_audioIsMpeg = false;
    m_numStreams = m_formatContext->nb_streams;
    m_defAudioIndex = INDEX_NONE;
    m_defVideoIndex = INDEX_NONE;
    m_startPos = POSITION_NONE;
    m_currentPosition = POSITION_NONE;
    m_audioBitrate = 0;
    m_eofReached = false;

    for (int i = 0; i < getNumStreams(); i++) {
        CStream* stream = getStream(i);
        AVStream* avStream = m_formatContext->streams[i];

        if (stream->initialize(avStream) < 0)
            continue;

        switch (stream->getDataType()) {
            case DATA_TYPE_AUDIO:
                if (m_defAudioIndex == INDEX_NONE) {
                    m_defAudioIndex = i;
                    m_audioIsMpeg = stream->isCodecMpeg();
                }

                break;
            case DATA_TYPE_VIDEO:
                if (m_defVideoIndex == INDEX_NONE)
                    m_defVideoIndex = i;

                break;
            default:
                break;
        }
    }

    return 0;
}

void CMediaContext::readStartPos() {
    if (m_startPos != POSITION_NONE)
        return;

    while (readNextFrame()) {
        if (m_startPos != POSITION_NONE)
            break;
    }
}

void CMediaContext::closeStreams() {
    if (!m_streams)
        return;

    for (int i = 0; i < getNumStreams(); i++) {
        CStream* stream = getStream(i);

        stream->closeCodec();
    }
}

void CMediaContext::releaseIOContext() {
    if (m_avioContext) {
        av_freep(&m_avioContext->buffer);
        av_freep(&m_avioContext);
    }
}

void CMediaContext::releaseStreams() {
    if (!m_streams)
        return;

    for (int i = 0; i < getNumStreams(); i++) {
        CStream* stream = getStream(i);

        stream->release();
    }

    delete [] m_streams;
    m_streams = NULL;
    m_numStreams = 0;
}

int CMediaContext::readPacket(void *opaque, uint8_t *buf, int buf_size) {
    CBufferStream* stream = (CBufferStream*)opaque;

    return stream->read(buf, (size_t)buf_size);
}

// Only called when seeking to an absolute position
int64_t CMediaContext::seek(void *opaque, int64_t offset, int whence) {
    CBufferStream* stream = (CBufferStream*)opaque;

    return stream->seek(offset);
}