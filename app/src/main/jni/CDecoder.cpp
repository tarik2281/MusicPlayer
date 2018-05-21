//
// Created by Tarik on 15.11.2015.
//

#include "CDecoder.h"
#include "CFrame.h"
#include "CStream.h"
#include "log.h"
#include "config.h"

#include <stdlib.h>

CDecoder::CDecoder() :  m_swsContext(NULL),
                        m_videoCodec(NULL),
                        m_videoFrame(NULL),
                        m_videoBuffer(NULL),
                        m_videoBufferCapacity(0),
                        m_videoBufferSize(0),
                        m_inHeight(0),
                        m_videoOutWidth(0),
                        m_videoOutHeight(0) {
    
}

CDecoder::~CDecoder() {

}

void CDecoder::release() {
    if (m_videoFrame)
        av_frame_free(&m_videoFrame);

    if (m_swsContext) {
        sws_freeContext(m_swsContext);
        m_swsContext = NULL;
    }

    if (m_videoBuffer)
        av_freep(&m_videoBuffer);
}

void CDecoder::setVideoCodec(AVCodecContext* context) {
    m_videoCodec = context;

    int w = m_videoCodec->width;
    int h = m_videoCodec->height;
    int outW = (m_videoOutWidth > 0) ? m_videoOutWidth : w;
    int outH = (m_videoOutHeight > 0) ? m_videoOutHeight : h;
    m_inHeight = h;

    m_swsContext = sws_getCachedContext(m_swsContext, w, h, m_videoCodec->pix_fmt, outW, outH, DEFAULT_PIX_FMT, SWS_BILINEAR, NULL, NULL, NULL);

    if (m_videoOutWidth <= 0 || m_videoOutHeight <= 0)
        initBuffer();
}

void CDecoder::setOutVideoSize(int width, int height) {
    m_videoOutWidth = width;
    m_videoOutHeight = height;

    initBuffer();
}

int CDecoder::decodeAudioFrame(CFrame* frame, int* gotFrame) const {
    AVPacket* pkt = frame->getPacket();

    int ret = avcodec_decode_audio4(frame->getStream()->getContext(), frame->getFrame(), gotFrame, pkt);

    if (CheckFFmpegErr("CDecoder::decodeAudioFrame", ret))
        return ret;

    ret = FFMIN(ret, pkt->size);

    pkt->data += ret;
    pkt->size -= ret;

    return pkt->size > 0;
}

uint8_t* CDecoder::decodeVideoFrame(CFrame* frame, int* outDataSize) const {
    AVCodecContext* ctxt = frame->getStream()->getContext();
    AVFrame* avFrame = frame->getFrame();
    AVPacket* pkt = frame->getPacket();

    int gotFrame = 0;

    int ret = avcodec_decode_video2(ctxt, avFrame, &gotFrame, pkt);

    if (CheckFFmpegErr("CDecoder::decodeVideoFrame", ret) || !gotFrame) {
        *outDataSize = -1;
        return NULL;
    }

    memset(m_videoBuffer, 0, m_videoBufferSize);
    sws_scale(m_swsContext, (uint8_t const * const *)avFrame->data, avFrame->linesize, 0, m_inHeight, m_videoFrame->data, m_videoFrame->linesize);
    *outDataSize = m_videoBufferSize;

    return m_videoBuffer;
}

void CDecoder::initBuffer() {
    int width = m_videoOutWidth > 0 ? m_videoOutWidth : m_videoCodec->width;
    int height = m_videoOutHeight > 0 ? m_videoOutHeight : m_videoCodec->height;

    m_videoBufferSize = (unsigned int)(width * height * DEFAULT_PIX_FMT_BYTES);

    if (m_videoBufferSize > m_videoBufferCapacity) {
        m_videoBufferCapacity = m_videoBufferSize;

        if (m_videoBuffer)
            av_freep(&m_videoBuffer);
    }

    if (!m_videoBuffer)
        m_videoBuffer = (uint8_t*)av_mallocz(m_videoBufferCapacity);

    if (!m_videoFrame)
        m_videoFrame = av_frame_alloc();

    avpicture_fill((AVPicture*)m_videoFrame, m_videoBuffer, DEFAULT_PIX_FMT, width, height);
}
