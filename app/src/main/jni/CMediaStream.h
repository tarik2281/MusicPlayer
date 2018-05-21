//
// Created by Tarik Karaca on 24.11.16.
//

#ifndef MUSICPLAYER_CMEDIASTREAM_H
#define MUSICPLAYER_CMEDIASTREAM_H

#include <atomic>
#include <pthread.h>
#include <string>

class CFilterState;
class ICallback;
class IErrorCallback;
class CAudioFilter;
class CMediaContext;
class CDecoder;

#define ERROR_INVALID_FILE -1 // args: const char* filePath

#define BUFFER_QUEUE_COUNT 50

#define _THREAD pthread_t
#define _MUTEX pthread_mutex_t
#define _COND pthread_cond_t

class CMediaStream {

public:

    CMediaStream();
    ~CMediaStream();

    int initialize(uint32_t sampleRate);
    int release();

    void setNextData(const char*, CFilterState*, bool forceClear);

    /**
     * Callback will be called on a separate thread
     */
    void setFinishCallback(ICallback*);

    /**
     * Callback will be called on a separate thread
     */
    void setErrorCallback(IErrorCallback*);

    void startDecodeThread();
    void stopDecodeThread();

    /**
     * Returns the current position in milliseconds
     */
    int64_t getCurrentPosition();

    /**
     * Seek to position in milliseconds
     */
    void seek(int64_t position);

    /**
     * thread-safe
     */
    int fillStreamBuffer(char* streamBuffer, int64_t size);

private:

    struct SAudioBuffer {
        uint8_t * data;
        size_t size;
        size_t capacity;
        int64_t m_position;
    };

    uint32_t m_sampleRate;

    SAudioBuffer m_buffers[BUFFER_QUEUE_COUNT];
    uint8_t m_numBuffers;
    std::atomic<uint8_t> m_bufferWriteIndex;
    std::atomic<uint8_t> m_bufferReadIndex;
    int m_bufferOffset;
    _MUTEX m_bufferMutex;
    _COND m_bufferCond;

    ICallback* m_finishCallback;
    IErrorCallback* m_errorCallback;

    std::string m_nextContextPath;
    CFilterState* m_nextFilterState;
    int64_t m_filterStateTime;
    int64_t m_crossfadeTime;
    bool m_forceClear;
    _MUTEX m_dataMutex;
    _COND m_dataCond;

    CMediaContext* m_context;
    CAudioFilter* m_filter;
    CFilterState* m_filterState;
    CDecoder* m_decoder;
    _THREAD m_decodeThread;
    bool m_decodeThreadRunning;

    int64_t m_seekPosition;
    _MUTEX m_seekMutex;

    std::atomic<int64_t> m_currentPosition;

    std::atomic<bool> m_stopDecodeThread;

    SAudioBuffer* getBuffer(int index);
    void clearBuffers(bool lock = true);

    void decodeThread();

    static void* _decodeThread(void*);

};

#undef _THREAD
#undef _MUTEX
#undef _COND

#endif //MUSICPLAYER_CMEDIASTREAM_H
