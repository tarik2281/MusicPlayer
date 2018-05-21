//
// Created by Tarik Karaca on 24.11.16.
//

#include "CMediaStream.h"
#include "CFilterState.h"
#include "CMediaContext.h"
#include "CAudioFilter.h"
#include "CStream.h"
#include "CFrame.h"
#include "CDecoder.h"
#include "ICallback.h"
#include "IErrorCallback.h"
#include "log.h"
#include "config.h"

#define FILTER_STATE_DELAY 2000
#define FILTER_CROSSFADE_DURATION 2.0f

#define CREATE_THREAD(tid, cb, obj) pthread_create(&tid, NULL, cb, (void*)obj)
#define JOIN(tid) pthread_join(tid, NULL)

#define INIT_MTX(mutex) pthread_mutex_init(&mutex, NULL)
#define LOCK(mutex) pthread_mutex_lock(&mutex)
#define UNLOCK(mutex) pthread_mutex_unlock(&mutex)

#define INIT_COND(cond) pthread_cond_init(&cond, NULL)
#define WAIT(cond, mutex) pthread_cond_wait(&cond, &mutex)
#define NOTIFY(cond) pthread_cond_signal(&cond)
#define NOTIFY_ALL(cond) pthread_cond_broadcast(&cond)

CMediaStream::CMediaStream() : m_sampleRate(0),
                               m_buffers(),
                               m_numBuffers(0),
                               m_bufferWriteIndex(0),
                               m_bufferReadIndex(0),
                               m_bufferOffset(0),
                               m_finishCallback(NULL),
                               m_errorCallback(NULL),
                               m_nextFilterState(NULL),
                               m_filterStateTime(0),
                               m_crossfadeTime(0),
                               m_forceClear(false),
                               m_context(NULL),
                               m_filter(NULL),
                               m_filterState(NULL),
                               m_decoder(NULL),
                               m_decodeThreadRunning(false),
                               m_seekPosition(0),
                               m_currentPosition(0),
                               m_stopDecodeThread(false) {

}

CMediaStream::~CMediaStream() {

}

int CMediaStream::initialize(uint32_t sampleRate) {
    INIT_MTX(m_bufferMutex);
    INIT_COND(m_bufferCond);

    INIT_MTX(m_dataMutex);
    INIT_COND(m_dataCond);

    INIT_MTX(m_seekMutex);

    m_decoder = new CDecoder();
    m_sampleRate = sampleRate;

    return 0;
}

int CMediaStream::release() {
    stopDecodeThread();

    if (m_filterState)
        SAFE_RELEASE(m_filterState);

    if (m_filter) {
        m_filter->release();
        delete m_filter;
        m_filter = NULL;
    }

    if (m_context) {
        m_context->close();
        m_context->release();
        delete m_context;
        m_context = NULL;
    }

    for (int i = 0; i < BUFFER_QUEUE_COUNT; i++) {
        SAudioBuffer* buffer = getBuffer(i);

        if (buffer->data) av_freep(&buffer->data);
    }
    memset(m_buffers, 0, sizeof(SAudioBuffer) * BUFFER_QUEUE_COUNT);

    setFinishCallback(NULL);
    setErrorCallback(NULL);

    if (m_decoder)
        delete m_decoder;
    m_decoder = NULL;
}

void CMediaStream::setNextData(const char * path, CFilterState * state, bool forceClear) {
    LOCK(m_dataMutex);

    if (path)
        m_nextContextPath = path;

    state->retain();

    if (m_nextFilterState)
        SAFE_RELEASE(m_nextFilterState);

    m_nextFilterState = state;
    m_filterStateTime = getCurrentPosition();

    m_forceClear = forceClear;

    NOTIFY_ALL(m_dataCond);
    UNLOCK(m_dataMutex);
}

void CMediaStream::setFinishCallback(ICallback * cb) {
    if (cb)
        cb->retain();

    if (m_finishCallback)
        m_finishCallback->release();

    m_finishCallback = cb;
}

void CMediaStream::setErrorCallback(IErrorCallback * cb) {
    if (cb)
        cb->retain();

    if (m_errorCallback)
        m_errorCallback->release();

    m_errorCallback = cb;
}

void CMediaStream::startDecodeThread() {
    if (!m_decodeThreadRunning) {
        m_stopDecodeThread = false;
        CREATE_THREAD(m_decodeThread, CMediaStream::_decodeThread, this);
    }
}

void CMediaStream::stopDecodeThread() {
    if (m_decodeThreadRunning) {
        m_stopDecodeThread = true;
        NOTIFY_ALL(m_dataCond);
        NOTIFY_ALL(m_bufferCond);
        JOIN(m_decodeThread);
    }
}

int64_t CMediaStream::getCurrentPosition() {
    return m_currentPosition;
}

void CMediaStream::seek(int64_t position) {
    LOCK(m_seekMutex);

    m_seekPosition = position;

    UNLOCK(m_seekMutex);
}

int CMediaStream::fillStreamBuffer(char *streamBuffer, int64_t size) {
    int64_t len = size;
    int64_t len1 = 0;
    int64_t offset = 0;

    LOCK(m_bufferMutex);

    while (!m_stopDecodeThread && m_numBuffers <= 0)
        WAIT(m_bufferCond, m_bufferMutex);

    UNLOCK(m_bufferMutex);

    if (m_stopDecodeThread)
        return false;

    SAudioBuffer* buffer = getBuffer(m_bufferReadIndex);
    m_currentPosition = buffer->m_position;

    while (len > 0) {
        if (m_bufferOffset >= buffer->size) {
            m_bufferReadIndex = ((m_bufferReadIndex + 1) % BUFFER_QUEUE_COUNT);

            LOCK(m_bufferMutex);

            if (m_numBuffers > 0)
                m_numBuffers--;

            NOTIFY_ALL(m_bufferCond);

            while (!m_stopDecodeThread && m_numBuffers <= 0)
                WAIT(m_bufferCond, m_bufferMutex);

            UNLOCK(m_bufferMutex);

            if (m_stopDecodeThread)
                return false;

            buffer = getBuffer(m_bufferReadIndex);

            m_bufferOffset = 0;
            m_currentPosition = buffer->m_position;
        }

        len1 = buffer->size - m_bufferOffset;

        if (len1 > len)
            len1 = len;

        memcpy(streamBuffer + offset, buffer->data + m_bufferOffset, (size_t)len1);

        len -= len1;
        offset += len1;
        m_bufferOffset += len1;
    }

    return true;
}

CMediaStream::SAudioBuffer* CMediaStream::getBuffer(int index) {
    return &m_buffers[index];
}

void CMediaStream::clearBuffers(bool lock) {
    if (lock)
        LOCK(m_bufferMutex);

    m_numBuffers = 0;
    m_bufferWriteIndex = 0;
    m_bufferReadIndex = 0;

    if (lock)
        UNLOCK(m_bufferMutex);
}

void CMediaStream::decodeThread() {
    m_decodeThreadRunning = true;

    std::string tempPath;
    CFilterState* tempState = NULL;
    bool forceClear = false;

    bool createFilter = false;
    bool nextFilter = false;
    bool crossfadeEnded = false;

    CFrame* frame = NULL;
    int gotFrame = 0;
    int framesRemaining = false;

    while (!m_stopDecodeThread) {

        LOCK(m_dataMutex);

        // wait while no file and no filter state are set
        while (!m_stopDecodeThread && (((!m_context || (m_context && !m_context->isOpen())) && m_nextContextPath.empty()) ||
                           (!m_filterState && !m_nextFilterState)))
            WAIT(m_dataCond, m_dataMutex);

        if (!m_nextContextPath.empty())
            tempPath.swap(m_nextContextPath);

        if (m_nextFilterState) {
            tempState = m_nextFilterState;
            m_nextFilterState = NULL;
        }

        forceClear = m_forceClear;
        m_forceClear = false;

        UNLOCK(m_dataMutex);

        if (m_stopDecodeThread)
            break;

#define CALL_ERROR() do { \
            LOGE("CMediaPlayer::decodeThread", "Attempted to play invalid file."); \
            if (m_errorCallback) \
                m_errorCallback->call(ERROR_INVALID_FILE, m_context->getFilePath()); \
            m_context->close(); } while (0)

        if (!tempPath.empty()) {
            if (m_context)
                m_context->close();
            else
                m_context = new CMediaContext();

            if (m_context->open(tempPath.c_str()) < 0)
                CALL_ERROR();
            else if (m_context->getStream(m_context->getDefaultAudioIndex())->openCodec() < 0)
                CALL_ERROR();
            else
                createFilter = true;

            std::string().swap(tempPath);

            if (!m_context->isOpen())
                continue;
        }
        else {
            nextFilter = tempState && getCurrentPosition() >= m_filterStateTime + FILTER_STATE_DELAY;
            crossfadeEnded = m_crossfadeTime > 0 && getCurrentPosition() >= m_crossfadeTime + FILTER_STATE_DELAY;
        }

        if (createFilter || nextFilter) {
            if (!m_filter)
                m_filter = new CAudioFilter();

            m_filter->release();

            CStream* stream = m_context->getStream(m_context->getDefaultAudioIndex());
            AVCodecContext* context = stream->getContext();
            if (m_filter->initFormat(stream->m_stream->time_base, context->sample_fmt,
                                     context->sample_rate, context->channel_layout,
                                     DEFAULT_SAMPLE_FMT, m_sampleRate, DEFAULT_CHANNEL_LAYOUT) < 0)
                CALL_ERROR();

            if (createFilter) {
                m_filter->create(tempState);

                if (m_filterState)
                    SAFE_RELEASE(m_filterState);

                m_filterState = tempState;
                tempState = NULL;
            }
            else if (nextFilter) {
                m_crossfadeTime = getCurrentPosition();

                m_filter->crossfade(m_filterState, tempState, FILTER_CROSSFADE_DURATION);

                if (m_filterState)
                SAFE_RELEASE(m_filterState);

                m_filterState = tempState;
                tempState = NULL;
            }
            /*else if (crossfadeEnded) {
                m_crossfadeTime = 0;

                m_filter->create(m_filterState);
            }*/

            createFilter = false;
            nextFilter = false;
            crossfadeEnded = false;

            if (!m_context->isOpen())
                continue;
        }

#undef CALL_ERROR

        LOCK(m_bufferMutex);

        if (forceClear)
            clearBuffers(false);

        while (!m_stopDecodeThread && m_numBuffers >= BUFFER_QUEUE_COUNT)
            WAIT(m_bufferCond, m_bufferMutex);

        UNLOCK(m_bufferMutex);

        if (m_stopDecodeThread)
            break;

        do {

            LOCK(m_seekMutex);

            if (m_seekPosition > -1) {
                if (m_context->seekBytes(m_seekPosition) < 0)
                    m_context->seekTime(m_seekPosition);

                m_seekPosition = -1;

                UNLOCK(m_seekMutex);

                clearBuffers();
                framesRemaining = false;
            }
            else
                UNLOCK(m_seekMutex);

            if (m_context->eof())
                break;

            if (!framesRemaining)
                frame = m_context->readNextFrame();

            if (frame && frame->getStream()->getDataType() == DATA_TYPE_AUDIO) {
                CStream* stream = frame->getStream();
                framesRemaining = m_decoder->decodeAudioFrame(frame, &gotFrame);

                if (framesRemaining < 0) {
                    framesRemaining = 0;
                    continue;
                }

                if (gotFrame) {
                    SAudioBuffer* buffer = getBuffer(m_bufferWriteIndex);

                    buffer->size = m_filter->filterFrame(frame, &buffer->data, &buffer->capacity);
                    buffer->m_position = m_context->getCurrentPosition();

                    m_bufferWriteIndex = ((m_bufferWriteIndex + 1) % BUFFER_QUEUE_COUNT);

                    LOCK(m_bufferMutex);

                    m_numBuffers++;
                    NOTIFY_ALL(m_bufferCond);

                    UNLOCK(m_bufferMutex);
                }
            }
            else {
                gotFrame = 0;

                if (m_context->eof() && m_finishCallback)
                    m_finishCallback->call();
            }

        } while (!gotFrame && !m_stopDecodeThread);
    }

    if (m_context) {
        m_context->close();
    }

    m_decodeThreadRunning = false;
}

void* CMediaStream::_decodeThread(void * ctx) {
    CMediaStream* stream = (CMediaStream*)ctx;

    stream->decodeThread();

    return NULL;
}