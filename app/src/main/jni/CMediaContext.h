//
// Created by Tarik Karaca on 01.03.16.
//

#ifndef MUSICPLAYER_CMEDIACONTEXT_H
#define MUSICPLAYER_CMEDIACONTEXT_H

#include "CBufferStream.h"

#include <string>

extern "C" {

#include "libavformat/avformat.h"

};

#define INDEX_NONE -1

class CFrame;
class CStream;

class CMediaContext {

public:

    CMediaContext();
    ~CMediaContext();

    int open(const uint8_t* buffer, size_t size);
    int open(const char* filePath);
    void close();
    void release();

    bool isOpen();
    const char* getFilePath() const;

    /*!
     * Seek the context to the given position in milliseconds.
     *
     * \see seekTime(int64_t)
     */
    int seekBytes(int64_t offset);

    /*!
     * \see seekBytes(int64_t)
     */
    int seekTime(int64_t offset);

    /*!
     * Returns the current position in the context in milliseconds.
     */
    int64_t getCurrentPosition();

    CFrame* readNextFrame();
    bool eof() const;

    // returns null if index out of range
    CStream* getStream(int index) const;
    int getNumStreams() const;

    /*!
     * Returns the duration of the context in milliseconds.
     */
    int64_t getDuration() const;

    /*!
     * Returns the sample rate in Hz.
     */
    int getSampleRate() const;

    /*!
     * Returns the bitrate in bits per second.
     */
    int64_t getBitrate() const;

    int getChannels() const;

    int getDefaultAudioIndex() const;
    int getDefaultVideoIndex() const;

private:

    enum InputType {
        None, BufferStream, File
    };

    CBufferStream m_bufferStream;
    AVIOContext* m_avioContext;

    CStream* m_streams;
    int m_numStreams;

    bool m_audioIsMpeg;
    bool m_cacheFrames;
    CFrame* m_lastFrame;
    bool m_eofReached;
    InputType m_inputType;
    int64_t m_startPos;
    int64_t m_currentPosition;
    int64_t m_audioBitrate;
    int m_defAudioIndex;
    int m_defVideoIndex;
    AVFormatContext* m_formatContext;
    bool m_isOpen;
    std::string m_filePath;

    int initIOContext();
    int readStreams();

    void readStartPos();

    void closeStreams();

    void releaseIOContext();
    void releaseStreams();

    static int readPacket(void* opaque, uint8_t* buf, int buf_size);
    static int64_t seek(void* opaque, int64_t offset, int whence);

};

#endif //MUSICPLAYER_CMEDIACONTEXT_H
