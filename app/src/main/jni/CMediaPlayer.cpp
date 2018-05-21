//
// Created by Tarik Karaca on 03.03.16.
//

//#include "CMediaPlayer.h"
//
//#include "CFilterState.h"
//#include "CAudioFilter.h"
//#include "CDecoder.h"
//#include "CFrame.h"
//#include "CMediaContext.h"
//#include "CStream.h"
//#include "ICallback.h"
//#include "IErrorCallback.h"
//#include "config.h"
//#include "log.h"
//
//extern "C" {
//
//#include "libavutil/mem.h"
//
//#include <SLES/OpenSLES_AndroidConfiguration.h>
//
//}
//
//#define CREATE_THREAD(tid, cb, obj) pthread_create(&tid, NULL, cb, (void*)obj)
//#define JOIN(tid) pthread_join(tid, NULL)
//
//#define INIT_MTX(mutex) pthread_mutex_init(&mutex, NULL)
//#define LOCK(mutex) pthread_mutex_lock(&mutex)
//#define UNLOCK(mutex) pthread_mutex_unlock(&mutex)
//
//#define INIT_COND(cond) pthread_cond_init(&cond, NULL)
//#define WAIT(cond, mutex) pthread_cond_wait(&cond, &mutex)
//#define NOTIFY(cond) pthread_cond_signal(&cond)
//#define NOTIFY_ALL(cond) pthread_cond_broadcast(&cond)
//
//#define ALLOC_FILTER() new CAudioFilter()
//#define FREE_FILTER(filter) { filter->release(); delete filter; filter = NULL; }
//
//CMediaPlayer::CMediaPlayer() : m_buffers(),
//                               m_numBuffers(0),
//                               m_bufferWriteIndex(0),
//                               m_bufferReadIndex(0),
//                               m_bufferOffset(0),
//                               m_bufferMutex(),
//                               m_bufferCond(),
//                               m_streamBuffer(),
//                               m_streamBufferOffset(0),
//                               m_finishCallback(NULL),
//                               m_errorCallback(NULL),
//                               m_context(NULL),
//                               m_nextContextPath(),
//                               m_forceClear(false),
//                               m_filter(NULL),
//                               m_filterStateTime(0),
//                               m_crossfadeTime(0),
//                               m_filterCreationTime(0),
//                               m_filterState(NULL),
//                               m_nextFilterState(NULL),
//                               m_dataMutex(),
//                               m_dataCond(),
//                               m_decoder(NULL),
//                               m_decodeThread(),
//                               m_decodeThreadRunning(false),
//                               m_engineObject(NULL),
//                               m_engine(NULL),
//                               m_outputMixObject(NULL),
//                               m_playerObject(NULL),
//                               m_player(NULL),
//                               m_volumeControl(NULL),
//                               m_bufferQueue(NULL),
//                               m_framesPerBuffer(0),
//                               m_sampleRate(0),
//                               m_seekPosition(-1),
//                               m_seekMutex(),
//                               m_currentVolume(1.0f),
//                               m_fadeVolumeFrom(0.0f),
//                               m_fadeVolumeTo(0.0f),
//                               m_fadeScale(0.0f),
//                               m_fadeStart(-1),
//                               m_fadeMutex(),
//                               m_currentPosition(),
//                               m_isPlaying(false),
//                               m_stop(false),
//                               m_release(false) {
//
//}
//
//CMediaPlayer::~CMediaPlayer() {
//
//}
//
//int CMediaPlayer::initialize(uint32_t framesPerBuffer, uint32_t sampleRate) {
//    LOG_f("CMediaPlayer::initialize", "Initializing MediaPlayer with framesPerBuffer = %d, sampleRate = %d", framesPerBuffer, sampleRate);
//    m_framesPerBuffer = framesPerBuffer;
//    m_sampleRate = sampleRate;
//
//    INIT_MTX(m_bufferMutex);
//    INIT_COND(m_bufferCond);
//
//    INIT_MTX(m_streamBufferMutex);
//
//    INIT_MTX(m_dataMutex);
//    INIT_COND(m_dataCond);
//
//    INIT_MTX(m_playerMutex);
//
//    INIT_MTX(m_seekMutex);
//
//    INIT_MTX(m_fadeMutex);
//
//    m_decoder = new CDecoder();
//
//    if (initializeDevice() < 0)
//        return -1;
//
//    if (initializePlayer() < 0)
//        return -1;
//
//    return 0;
//}
//
//void CMediaPlayer::release() {
//    stop();
//
//    if (m_filterState)
//        SAFE_RELEASE(m_filterState);
//
//    if (m_filter)
//        FREE_FILTER(m_filter);
//
//    releasePlayer();
//    releaseDevice();
//
//    if (m_context) {
//        m_context->close();
//        m_context->release();
//        delete m_context;
//        m_context = NULL;
//    }
//
//    for (int i = 0; i < BUFFER_QUEUE_COUNT; i++) {
//        SAudioBuffer* buffer = getBuffer(i);
//
//        if (buffer->data)
//            av_freep(&buffer->data);
//    }
//    memset(m_buffers, 0, sizeof(SAudioBuffer) * BUFFER_QUEUE_COUNT);
//
//    if (m_streamBuffer.data)
//        free(m_streamBuffer.data);
//    memset(&m_streamBuffer, 0, sizeof(SAudioBuffer));
//
//    if (m_finishCallback)
//        m_finishCallback->release();
//    m_finishCallback = NULL;
//
//    if (m_errorCallback)
//        m_errorCallback->release();
//    m_errorCallback = NULL;
//
//    if (m_decoder)
//        delete m_decoder;
//    m_decoder = NULL;
//}
//
//void CMediaPlayer::setNextData(const char * path, CFilterState * state, bool forceClear) {
//    LOCK(m_dataMutex);
//
//    if (path)
//        m_nextContextPath = path;
//
//    state->retain();
//
//    if (m_nextFilterState)
//        SAFE_RELEASE(m_nextFilterState);
//
//    m_nextFilterState = state;
//    m_filterStateTime = getCurrentPosition();
//
//    m_forceClear = forceClear;
//
//    NOTIFY_ALL(m_dataCond);
//    UNLOCK(m_dataMutex);
//}
//
//void CMediaPlayer::setFinishCallback(ICallback* callback) {
//    if (callback)
//        callback->retain();
//
//    if (m_finishCallback)
//        m_finishCallback->release();
//
//    m_finishCallback = callback;
//}
//
//void CMediaPlayer::setErrorCallback(IErrorCallback * callback) {
//    if (callback)
//        callback->retain();
//
//    if (m_errorCallback)
//        m_errorCallback->release();
//
//    m_errorCallback = callback;
//}
//
//void CMediaPlayer::play() {
//    if (!m_decodeThreadRunning) {
//        m_stop = false;
//        CREATE_THREAD(m_decodeThread, CMediaPlayer::_decodeThread, this);
//    }
//
//    /*if (m_playerObject) {
//        if (CheckSLResult("CMediaPlayer::play", (*m_player)->SetPlayState(m_player, SL_PLAYSTATE_PLAYING)))
//            return;
//
//        m_isPlaying = true;
//    }*/
//}
//
//void CMediaPlayer::pause() {
//    /*if (m_playerObject) {
//        if (CheckSLResult("CMediaPlayer::pause", (*m_player)->SetPlayState(m_player, SL_PLAYSTATE_PAUSED)))
//            return;
//
//        m_isPlaying = false;
//    }*/
//}
//
//void CMediaPlayer::stop() {
//    pause();
//
//    if (m_decodeThreadRunning) {
//        m_stop = true;
//        NOTIFY_ALL(m_dataCond);
//        NOTIFY_ALL(m_bufferCond);
//        JOIN(m_decodeThread);
//    }
//}
//
//void CMediaPlayer::fadeVolume(float volume, float duration) {
//    LOCK(m_fadeMutex);
//    m_fadeVolumeFrom = m_currentVolume;
//    m_fadeVolumeTo = volume;
//    m_fadeScale = (m_fadeVolumeTo - m_fadeVolumeFrom) / 1000.0f / duration;
//    m_fadeStart = getCurrentPosition();
//    UNLOCK(m_fadeMutex);
//}
//
//void CMediaPlayer::seek(int64_t position) {
//    LOCK(m_seekMutex);
//
//    m_seekPosition = position;
//
//    UNLOCK(m_seekMutex);
//}
//
//int64_t CMediaPlayer::getCurrentPosition() {
//    return m_currentPosition;
//}
//
//void CMediaPlayer::clearBuffers(bool lock) {
//    if (lock)
//        LOCK(m_bufferMutex);
//
//    m_numBuffers = 0;
//    m_bufferWriteIndex = 0;
//    m_bufferReadIndex = 0;
//
//    if (lock)
//        UNLOCK(m_bufferMutex);
//}
//
//int CMediaPlayer::initializeDevice() {
//    /*SLEngineOption threadSafety;
//    threadSafety.feature = SL_ENGINEOPTION_THREADSAFE;
//    threadSafety.data = SL_BOOLEAN_TRUE;
//
//    if (CheckSLResult("CMediaPlayer::initializeDevice", slCreateEngine(&m_engineObject, 1, &threadSafety, 0, NULL, NULL)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializeDevice", (*m_engineObject)->Realize(m_engineObject, SL_BOOLEAN_FALSE)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializeDevice", (*m_engineObject)->GetInterface(m_engineObject, SL_IID_ENGINE, &m_engine)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializeDevice", (*m_engine)->CreateOutputMix(m_engine, &m_outputMixObject, 0, NULL, NULL)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializeDevice", (*m_outputMixObject)->Realize(m_outputMixObject, SL_BOOLEAN_FALSE)))
//        return -1;
//*/
//    return 0;
//}
//
//int CMediaPlayer::initializePlayer() {
//    /*if (m_playerObject)
//        return 0;
//
//    SLuint32 sampleRate = m_sampleRate * 1000;
//
//    SLDataLocator_AndroidSimpleBufferQueue loc_buf = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
//    SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, DEFAULT_NUM_CHANNELS, sampleRate,
//                                    SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
//                                    DEFAULT_CHANNEL_LAYOUT, SL_BYTEORDER_LITTLEENDIAN };
//
//    SLDataSource audioSrc = { &loc_buf, &format_pcm };
//
//    SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX, m_outputMixObject };
//    SLDataSink audioSink = { &loc_outmix, NULL };
//
//    const SLInterfaceID ids[] = { SL_IID_BUFFERQUEUE, SL_IID_VOLUME };
//    const SLboolean req[] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
//    if (CheckSLResult("CMediaPlayer::initializePlayer", (*m_engine)->CreateAudioPlayer(m_engine, &m_playerObject, &audioSrc, &audioSink, 2, ids, req)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializePlayer", (*m_playerObject)->Realize(m_playerObject, SL_BOOLEAN_FALSE)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializePlayer", (*m_playerObject)->GetInterface(m_playerObject, SL_IID_PLAY, &m_player)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializePlayer", (*m_playerObject)->GetInterface(m_playerObject, SL_IID_VOLUME, &m_volumeControl)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializePlayer", (*m_playerObject)->GetInterface(m_playerObject, SL_IID_BUFFERQUEUE, &m_bufferQueue)))
//        return -1;
//
//    if (CheckSLResult("CMediaPlayer::initializePlayer", (*m_bufferQueue)->RegisterCallback(m_bufferQueue, CMediaPlayer::_audioCallback, this)))
//        return -1;
//
//    m_streamBuffer.size = m_framesPerBuffer * DEFAULT_NUM_CHANNELS * av_get_bytes_per_sample(DEFAULT_SAMPLE_FMT);
//    m_streamBuffer.data = (uint8_t*)malloc(m_streamBuffer.size);
//
//    enqueueSilenceBuffer();*/
//
//    return 0;
//}
//
//CMediaPlayer::SAudioBuffer* CMediaPlayer::getBuffer(int index) {
//    return &m_buffers[index];
//}
//
//void CMediaPlayer::enqueueSilenceBuffer() {
//    if (!m_streamBuffer.data || !m_bufferQueue)
//        return;
//
//    memset(m_streamBuffer.data, 0, m_streamBuffer.size);
//    CheckSLResult("CMediaPlayer::enqueueSilenceBuffer", (*m_bufferQueue)->Enqueue(m_bufferQueue, m_streamBuffer.data, m_streamBuffer.size));
//}
//
//void CMediaPlayer::releaseDevice() {
//    if (m_outputMixObject)
//        (*m_outputMixObject)->Destroy(m_outputMixObject);
//
//    if (m_engineObject)
//        (*m_engineObject)->Destroy(m_engineObject);
//
//    m_engineObject = NULL;
//    m_engine = NULL;
//    m_outputMixObject = NULL;
//}
//
//void CMediaPlayer::releasePlayer() {
//    m_release = true;
//    NOTIFY_ALL(m_bufferCond);
//
//    if (m_playerObject)
//        (*m_playerObject)->Destroy(m_playerObject);
//
//    if (m_streamBuffer.data)
//        free(m_streamBuffer.data);
//    m_streamBuffer.data = NULL;
//    m_streamBuffer.size = 0;
//    m_streamBuffer.capacity = 0;
//
//    m_playerObject = NULL;
//    m_player = NULL;
//    m_bufferQueue = NULL;
//}
//
//void CMediaPlayer::decodeThread() {
//    m_decodeThreadRunning = true;
//
//    std::string tempPath;
//    CFilterState* tempState = NULL;
//    bool forceClear = false;
//    CFrame* frame = NULL;
//    int gotFrame = 0;
//    int framesRemaining = false;
//    bool createFilter = false;
//    bool nextFilterTime = false;
//    bool crossfadeEnded = false;
//
//    while (!m_stop) {
//        LOCK(m_dataMutex);
//
//        // wait while no file and no filter state is set
//        while (!m_stop && (((!m_context || (m_context && !m_context->isOpen())) && m_nextContextPath.empty()) ||
//                    (!m_filterState && !m_nextFilterState)))
//            WAIT(m_dataCond, m_dataMutex);
//
//        if (!m_nextContextPath.empty())
//            tempPath.swap(m_nextContextPath);
//
//        if (m_nextFilterState) {
//            tempState = m_nextFilterState;
//            m_nextFilterState = NULL;
//        }
//
//        forceClear = m_forceClear;
//        m_forceClear = false;
//
//        UNLOCK(m_dataMutex);
//
//        if (m_stop)
//            break;
//
//        if (!tempPath.empty()) {
//#define CALL_ERROR() do { \
//            LOGE("CMediaPlayer::decodeThread", "Attempted to play invalid file."); \
//            if (m_errorCallback) \
//                m_errorCallback->call(ERROR_INVALID_FILE, m_context->getFilePath()); \
//            m_context->close(); } while (0) \
//
//            if (m_context)
//                m_context->close();
//            else
//                m_context = new CMediaContext();
//
//            if (m_context->open(tempPath.c_str()) < 0)
//                CALL_ERROR();
//            else if (m_context->getStream(m_context->getDefaultAudioIndex())->openCodec() < 0)
//                CALL_ERROR();
//            else
//                createFilter = true;
//
//            std::string().swap(tempPath);
//
//            if (!m_context->isOpen())
//                continue;
//        }
//        else {
//            nextFilterTime = tempState && getCurrentPosition() >= m_filterStateTime + 2000;
//            crossfadeEnded = m_crossfadeTime > 0 && getCurrentPosition() >= m_crossfadeTime + 2000;
//        }
//
//        if (createFilter || nextFilterTime || crossfadeEnded) {
//            if (!m_filter)
//                m_filter = ALLOC_FILTER();
//
//            m_filter->release();
//
//            int res = 0;
//
//            if (frame != NULL) {
//                m_filterCreationTime = frame->getPacket()->pts;
//            }
//            else {
//                m_filterCreationTime = 0;
//            }
//
//            AVCodecContext* context = m_context->getStream(m_context->getDefaultAudioIndex())->getContext();
//            if ((res = m_filter->initFormat(m_context->getStream(m_context->getDefaultAudioIndex())->m_stream->time_base,
//                                            context->sample_fmt, context->sample_rate, context->channel_layout,
//                                 DEFAULT_SAMPLE_FMT, m_sampleRate, DEFAULT_CHANNEL_LAYOUT)) < 0)
//                CALL_ERROR();
//
//            if (createFilter) {
//                m_filter->create(tempState);
//
//                if (m_filterState)
//                    SAFE_RELEASE(m_filterState);
//
//                m_filterState = tempState;
//                tempState = NULL;
//            }
//            else if (nextFilterTime) {
//                m_crossfadeTime = getCurrentPosition();
//
//                m_filter->crossfade(m_filterState, tempState, 2.0f);
//
//                if (m_filterState)
//                    SAFE_RELEASE(m_filterState);
//
//                m_filterState = tempState;
//                tempState = NULL;
//            }
//            else if (crossfadeEnded) {
//                m_crossfadeTime = 0;
//
//                m_filter->create(m_filterState);
//            }
//
//            createFilter = false;
//            nextFilterTime = false;
//            crossfadeEnded = false;
//
//            if (!m_context->isOpen())
//                continue;
//#undef CALL_ERROR
//        }
//
//        LOCK(m_fadeMutex);
//
//        if (m_fadeStart >= 0) {
//            m_currentVolume = m_fadeVolumeFrom + ((getCurrentPosition() - m_fadeStart) * m_fadeScale);
//
//            int64_t duration = getCurrentPosition() - m_fadeStart;
//
//            bool fadeIn = m_fadeVolumeTo > m_fadeVolumeFrom;
//            if ((fadeIn && m_currentVolume >= m_fadeVolumeTo) || (!fadeIn && m_currentVolume <= m_fadeVolumeTo)) {
//                m_currentVolume = m_fadeVolumeTo;
//                m_fadeStart = -1;
//            }
//
//            SLmillibel result = m_currentVolume == 0.0f ? SL_MILLIBEL_MIN : (SLmillibel)(20.0f * log10f(m_currentVolume) * 100.0f);
//            LOG_f("CMediaPlayer::decodeThread", "result = %d mB, duration = %lld ms", result, duration);
//            (*m_volumeControl)->SetVolumeLevel(m_volumeControl, result);
//        }
//
//        UNLOCK(m_fadeMutex);
//
//        LOCK(m_bufferMutex);
//
//        if (forceClear)
//            clearBuffers(false);
//
//        while (!m_stop && m_numBuffers >= BUFFER_QUEUE_COUNT)
//            WAIT(m_bufferCond, m_bufferMutex);
//
//        UNLOCK(m_bufferMutex);
//
//        if (m_stop)
//            break;
//
//        gotFrame = 0;
//
//        do {
//            LOCK(m_seekMutex);
//
//            if (m_seekPosition > -1) {
//                if (m_context->seekBytes(m_seekPosition) < 0)
//                    m_context->seekTime(m_seekPosition);
//
//                m_seekPosition = -1;
//
//                UNLOCK(m_seekMutex);
//
//                clearBuffers();
//                framesRemaining = false;
//            }
//            else
//                UNLOCK(m_seekMutex);
//
//            if (m_context->eof())
//                break;
//
//            if (!framesRemaining)
//                frame = m_context->readNextFrame();
//
//            m_currentPosition = m_context->getCurrentPosition();
//
//            if (frame && frame->getStream()->getDataType() == DATA_TYPE_AUDIO) {
//                CStream* stream = frame->getStream();
//                framesRemaining = m_decoder->decodeAudioFrame(frame, &gotFrame);
//
//                //frame->getFrame()->pts = stream->toTimeBase(getCurrentPosition() - m_filterCreationTime);
//
//                frame->getFrame()->pts = frame->getPacket()->pts - m_filterCreationTime;
//                //frame->getFrame()->pts = frame->getPacket()->pts;
//
//                if (framesRemaining < 0) {
//                    framesRemaining = 0;
//                    continue;
//                }
//
//                if (gotFrame) {
//                    SAudioBuffer* buffer = getBuffer(m_bufferWriteIndex);
//
//                    buffer->size = m_filter->filterFrame(frame, &buffer->data, &buffer->capacity);
//
//                    m_bufferWriteIndex = ((m_bufferWriteIndex + 1) % BUFFER_QUEUE_COUNT);
//
//                    LOCK(m_bufferMutex);
//
//                    m_numBuffers++;
//                    NOTIFY_ALL(m_bufferCond);
//
//                    UNLOCK(m_bufferMutex);
//                }
//            }
//            else {
//                gotFrame = 0;
//
//                if (m_context->eof()) {
//                    if (m_finishCallback)
//                        m_finishCallback->call();
//                }
//            }
//
//        } while (!gotFrame && !m_stop);
//    }
//
//    if (m_context) {
//        m_context->close();
//        m_context->release();
//        delete m_context;
//        m_context = NULL;
//    }
//
//    m_decodeThreadRunning = false;
//}
//
//void CMediaPlayer::audioCallback(SLAndroidSimpleBufferQueueItf bq) {
//    int len = m_streamBuffer.size;
//    int len1 = 0;
//
//    m_streamBufferOffset = 0;
//
//    LOCK(m_bufferMutex);
//
//    while (!m_release && m_numBuffers <= 0)
//        WAIT(m_bufferCond, m_bufferMutex);
//
//    UNLOCK(m_bufferMutex);
//
//    if (m_release)
//        return;
//
//    SAudioBuffer* buffer = getBuffer(m_bufferReadIndex);
//
//    while (len > 0) {
//        if (m_bufferOffset >= buffer->size) {
//            m_bufferReadIndex = ((m_bufferReadIndex + 1) % BUFFER_QUEUE_COUNT);
//
//            LOCK(m_bufferMutex);
//
//            if (m_numBuffers > 0)
//                m_numBuffers--;
//
//            NOTIFY_ALL(m_bufferCond);
//
//            while (!m_release && m_numBuffers <= 0)
//                WAIT(m_bufferCond, m_bufferMutex);
//
//            UNLOCK(m_bufferMutex);
//
//            if (m_release)
//                return;
//
//            buffer = getBuffer(m_bufferReadIndex);
//
//            m_bufferOffset = 0;
//        }
//
//        len1 = buffer->size - m_bufferOffset;
//
//        if (len1 > len)
//            len1 = len;
//
//        memcpy(m_streamBuffer.data + m_streamBufferOffset, buffer->data + m_bufferOffset, len1);
//
//        len -= len1;
//        m_streamBufferOffset += len1;
//        m_bufferOffset += len1;
//    }
//
//    if (CheckSLResult("CMediaPlayer::audioCallback", (*bq)->Enqueue(bq, m_streamBuffer.data, m_streamBuffer.size)));
//}
//
//bool CMediaPlayer::fillStreamBuffer(char *buf, int64_t size) {
//    int64_t len = size;
//    int64_t len1 = 0;
//
//    m_streamBufferOffset = 0;
//
//    LOCK(m_bufferMutex);
//
//    while (!m_release && m_numBuffers <= 0)
//        WAIT(m_bufferCond, m_bufferMutex);
//
//    UNLOCK(m_bufferMutex);
//
//    if (m_release)
//        return false;
//
//    SAudioBuffer* buffer = getBuffer(m_bufferReadIndex);
//
//    while (len > 0) {
//        if (m_bufferOffset >= buffer->size) {
//            m_bufferReadIndex = ((m_bufferReadIndex + 1) % BUFFER_QUEUE_COUNT);
//
//            LOCK(m_bufferMutex);
//
//            if (m_numBuffers > 0)
//                m_numBuffers--;
//
//            NOTIFY_ALL(m_bufferCond);
//
//            while (!m_release && m_numBuffers <= 0)
//                WAIT(m_bufferCond, m_bufferMutex);
//
//            UNLOCK(m_bufferMutex);
//
//            if (m_release)
//                return false;
//
//            buffer = getBuffer(m_bufferReadIndex);
//
//            m_bufferOffset = 0;
//        }
//
//        len1 = buffer->size - m_bufferOffset;
//
//        if (len1 > len)
//            len1 = len;
//
//        memcpy(buf + m_streamBufferOffset, buffer->data + m_bufferOffset, (size_t)len1);
//        //memcpy(m_streamBuffer.data + m_streamBufferOffset, buffer->data + m_bufferOffset, len1);
//
//        len -= len1;
//        m_streamBufferOffset += len1;
//        m_bufferOffset += len1;
//    }
//
//    return true;
//}
//
//void* CMediaPlayer::_decodeThread(void* context) {
//    CMediaPlayer* player = (CMediaPlayer*)context;
//
//    player->decodeThread();
//
//    return NULL;
//}
//
//void CMediaPlayer::_audioCallback(SLAndroidSimpleBufferQueueItf bq, void* context) {
//    CMediaPlayer* player = (CMediaPlayer*)context;
//
//    player->audioCallback(bq);
//}