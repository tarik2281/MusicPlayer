//
// Created by Tarik on 25.12.2015.
//

#ifndef MUSICPLAYER_CAUDIOFILTER_H
#define MUSICPLAYER_CAUDIOFILTER_H

extern "C" {

#include "libavutil/samplefmt.h"
#include "libavutil/frame.h"
#include "libavfilter/avfilter.h"

};

#define FILTER_ERROR_ALLOC -1
#define FILTER_ERROR_INIT -2
#define FILTER_ERROR_LINK -3

class CFrame;
class CFilterState;

enum EQBand {
    EQBand1,
    EQBand2,
    EQBand3,
    EQBand4,
    EQBand5,
    EQBand6,
    EQBand7,
    EQBand8,
    EQBand9,
    EQBand10,
    EQBandBass,
    EQBandTreble,
    EQBandCount,
    EQBandNone
};

class CAudioFilter {

public:

    CAudioFilter();
    ~CAudioFilter();


    int initFormat(AVRational timeBase, AVSampleFormat inSampleFmt, int inSampleRate, int64_t inChannelLayout,
                    AVSampleFormat outSampleFmt, int outSampleRate, int64_t outChannelLayout);

    int crossfade(CFilterState* state1, CFilterState* state2, float duration);
    int create(CFilterState* state);
    void release();

    size_t filterFrame(CFrame* inFrame, uint8_t** buffer, size_t* bufferSize);

private:

    int createBuffer(AVFilterContext**);
    int createCrossfade(AVFilterContext**, float duration);
    int createCompressor(AVFilterContext**, CFilterState*);
    int createEqualizer(AVFilterContext**, CFilterState*);

    bool m_created;

    AVRational m_timeBase;
    AVSampleFormat m_inSampleFormat;
    AVSampleFormat m_outSampleFormat;
    int m_inSampleRate;
    int m_outSampleRate;
    int64_t m_inChannelLayout;
    int64_t m_outChannelLayout;

    AVFilterGraph* m_filterGraph;

    AVFilterContext* m_bufferContext1;
    AVFilterContext* m_bufferContext2;
    AVFilterContext* m_formatContext;
    AVFilterContext* m_buffersinkContext;

    AVFilterContext* m_crossfadeContext;
    AVFilterContext* m_compressorContext1;
    AVFilterContext* m_compressorContext2;
    AVFilterContext* m_bandContexts[EQBandCount * 2];

};


#endif //MUSICPLAYER_CAUDIOFILTER_H
