//
// Created by Tarik on 15.11.2015.
//

#include "CFrame.h"

CFrame::CFrame() : m_stream(NULL),
                   m_basePkt(),
                   m_pkt(),
                   m_frame(NULL) {

    av_init_packet(&m_basePkt);
    m_basePkt.data = NULL;
    m_basePkt.size = 0;

    av_init_packet(&m_pkt);
    m_pkt.data = NULL;
    m_pkt.size = 0;
}

CFrame::~CFrame() {

}

void CFrame::init(CStream * stream) {
    m_stream = stream;

    if (!m_frame)
        m_frame = av_frame_alloc();
}

void CFrame::release() {
    releasePacket();

    if (m_frame)
        av_frame_free(&m_frame);
}

void CFrame::releasePacket() {
    if (m_basePkt.data)
        av_free_packet(&m_basePkt);
}

AVPacket* CFrame::getPacket() {
    return &m_pkt;
}

AVFrame* CFrame::getFrame() const {
    return m_frame;
}

void CFrame::setPacket(AVPacket* packet) {
    av_packet_move_ref(&m_basePkt, packet);

    m_pkt = m_basePkt;
}