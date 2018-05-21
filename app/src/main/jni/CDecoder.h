//
// Created by Tarik on 15.11.2015.
//

#ifndef MUSICPLAYER_CDECODER_H
#define MUSICPLAYER_CDECODER_H

extern "C" {

#include "libavcodec/avcodec.h"
#include "libavutil/frame.h"
#include "libswscale/swscale.h"

};

class CFrame;

class CDecoder {
public:

    CDecoder();
    ~CDecoder();

    void release();

    void setVideoCodec(AVCodecContext*);
    void setOutVideoSize(int width, int height);

    int decodeAudioFrame(CFrame*, int*) const;

    uint8_t* decodeVideoFrame(CFrame*, int* outDataSize) const;

private:

    SwsContext* m_swsContext;

    AVCodecContext* m_videoCodec;

    AVFrame* m_videoFrame;
    uint8_t* m_videoBuffer;
    unsigned int m_videoBufferCapacity;
    unsigned int m_videoBufferSize;

    int m_inHeight;
    int m_videoOutWidth;
    int m_videoOutHeight;

    void initBuffer();

};

#endif //MUSICPLAYER_CDECODER_H
