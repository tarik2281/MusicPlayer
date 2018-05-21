//
// Created by Tarik on 15.11.2015.
//

#ifndef MUSICPLAYER_CFRAME_H
#define MUSICPLAYER_CFRAME_H


extern "C" {

#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/frame.h"

};

#define DATA_TYPE_VIDEO 1
#define DATA_TYPE_AUDIO 2

class CStream;

class CFrame {

public:

    CFrame();
    ~CFrame();

    void init(CStream*);
    void release();

    void releasePacket();

    CStream* getStream() const;

    AVPacket* getPacket();
    AVFrame* getFrame() const;

    void setPacket(AVPacket* pkt);

private:

    CStream* m_stream;

    AVPacket m_basePkt;
    AVPacket m_pkt;

    AVFrame* m_frame;

};

inline CStream* CFrame::getStream() const {
    return m_stream;
}

#endif //MUSICPLAYER_CFRAME_H
