//
// Created by Tarik Karaca on 03.03.16.
//

//#ifndef MUSICPLAYER_CMEDIAPLAYER2_H
//#define MUSICPLAYER_CMEDIAPLAYER2_H
//
//#include <atomic>
//#include <string>
//#include <pthread.h>
//
//extern "C" {
//
//#include <SLES/OpenSLES.h>
//#include <SLES/OpenSLES_Android.h>
//
//};
//
//
//#define ERROR_INVALID_FILE -1 // args: const char* filePath
//
//
//#define _THREAD pthread_t
//#define _MUTEX pthread_mutex_t
//#define _COND pthread_cond_t
//
//class CFilterState;
//class CAudioFilter;
//class CMediaContext;
//class CDecoder;
//class ICallback;
//class IErrorCallback;
//
//class CMediaPlayer {
//
//public:
//
//    CMediaPlayer();
//    ~CMediaPlayer();
//
//    /*
//     * Initialize this MediaPlayer with framesPerBuffer and sampleRate in Hz
//     */
//    int initialize(uint32_t framesPerBuffer, uint32_t sampleRate);
//    void release();
//
//    void setNextData(const char*, CFilterState*, bool forceClear);
//
//    /**
//     * Callback will be called on a separate thread
//     */
//    void setFinishCallback(ICallback*);
//
//    /**
//     * Callback will be called on a separate thread
//     */
//    void setErrorCallback(IErrorCallback*);
//
//    void play();
//    void pause();
//    void stop();
//
//    void fadeVolume(float volume, float duration);
//
//    /**
//     * Seek to position in milliseconds
//     */
//    void seek(int64_t position);
//
//    /**
//     * Returns the current position in milliseconds
//     */
//    int64_t getCurrentPosition();
//
//    bool fillStreamBuffer(char* buffer, int64_t size);
//
//private:
//
//    struct SAudioBuffer {
//        uint8_t* data;
//        size_t size;
//        size_t capacity;
//    };
//
//    SAudioBuffer m_buffers[BUFFER_QUEUE_COUNT];
//    uint8_t m_numBuffers;
//    std::atomic<int> m_bufferWriteIndex;
//    std::atomic<int> m_bufferReadIndex;
//    int m_bufferOffset;
//    _MUTEX m_bufferMutex;
//    _COND m_bufferCond;
//
//    SAudioBuffer m_streamBuffer;
//    int m_streamBufferOffset;
//    _MUTEX m_streamBufferMutex;
//
//    ICallback* m_finishCallback;
//    IErrorCallback* m_errorCallback;
//
//    std::string m_nextContextPath;
//    CFilterState* m_nextFilterState;
//    int64_t m_filterStateTime;
//    bool m_forceClear;
//    _MUTEX m_dataMutex;
//    _COND m_dataCond;
//
//    CMediaContext* m_context;
//    CAudioFilter* m_filter;
//    int64_t m_crossfadeTime;
//    int64_t m_filterCreationTime;
//    CFilterState* m_filterState;
//    CDecoder* m_decoder;
//    _THREAD m_decodeThread;
//    bool m_decodeThreadRunning;
//
//    SLObjectItf m_engineObject;
//    SLEngineItf m_engine;
//    SLObjectItf m_outputMixObject;
//    SLObjectItf m_playerObject;
//    SLPlayItf   m_player;
//    SLVolumeItf m_volumeControl;
//    SLAndroidSimpleBufferQueueItf m_bufferQueue;
//    _MUTEX m_playerMutex;
//
//    uint32_t m_framesPerBuffer;
//    uint32_t m_sampleRate;
//
//    int64_t m_seekPosition;
//    _MUTEX m_seekMutex;
//
//    float m_currentVolume;
//    float m_fadeVolumeFrom;
//    float m_fadeVolumeTo;
//    float m_fadeScale;
//    int64_t m_fadeStart;
//    _MUTEX m_fadeMutex;
//
//    std::atomic<int64_t> m_currentPosition;
//    bool m_isPlaying;
//
//    std::atomic<bool> m_stop;
//    std::atomic<bool> m_release;
//
//    void clearBuffers(bool lock = true);
//
//    int initializeDevice();
//    int initializePlayer();
//
//    SAudioBuffer* getBuffer(int index);
//
//    void enqueueSilenceBuffer();
//
//    void releaseDevice();
//    void releasePlayer();
//
//    void decodeThread();
//    void audioCallback(SLAndroidSimpleBufferQueueItf);
//
//    static void* _decodeThread(void*);
//    static void  _audioCallback(SLAndroidSimpleBufferQueueItf, void*);
//
//};
//
//#undef _THREAD
//#undef _MUTEX
//#undef _COND
//
//#endif //MUSICPLAYER_CMEDIAPLAYER2_H
//