//
// Created by Tarik on 25.12.2015.
//

#include "CAudioFilter.h"
#include "CFrame.h"
#include "CFilterState.h"

#include "log.h"

extern "C" {

#include "libavfilter/buffersrc.h"
#include "libavfilter/buffersink.h"

};

#ifdef _DEBUG
#define ENABLE_LOG_OPTIONS 1
#else
#define ENABLE_LOG_OPTIONS 0
#endif

#define ALLOC_FILTER(ctx, filter, name) \
if (!(ctx = avfilter_graph_alloc_filter(m_filterGraph, filter, name))) { \
LOG_f("CAudioFilter", "Could not alloc filter %d", (filter == NULL)); \
return FILTER_ERROR_ALLOC; \
}

#define INIT_FILTER(func, ctx, str) \
if (CheckFFmpegErr("CAudioFilter::"#func, avfilter_init_str(ctx, str))) \
    return FILTER_ERROR_INIT;

CAudioFilter::CAudioFilter() : m_created(false),
                               m_inSampleFormat(AVSampleFormat::AV_SAMPLE_FMT_NONE),
                               m_outSampleFormat(AVSampleFormat::AV_SAMPLE_FMT_NONE),
                               m_inSampleRate(0),
                               m_outSampleRate(0),
                               m_inChannelLayout(0),
                               m_outChannelLayout(0),
                               m_filterGraph(NULL),
                               m_bufferContext1(NULL),
                               m_bufferContext2(NULL),
                               m_formatContext(NULL),
                               m_buffersinkContext(NULL),
                               m_crossfadeContext(NULL),
                               m_compressorContext1(NULL),
                               m_compressorContext2(NULL),
                               m_bandContexts() {

}

CAudioFilter::~CAudioFilter() {

}

int CAudioFilter::initFormat(AVRational timeBase, AVSampleFormat inSampleFmt, int inSampleRate,
                              int64_t inChannelLayout, AVSampleFormat outSampleFmt,
                              int outSampleRate, int64_t outChannelLayout) {
    if (m_filterGraph == NULL)
        m_filterGraph = avfilter_graph_alloc();

    m_timeBase = timeBase;
    m_inSampleFormat = inSampleFmt;
    m_outSampleFormat = outSampleFmt;
    m_inSampleRate = inSampleRate;
    m_outSampleRate = outSampleRate;
    m_inChannelLayout = inChannelLayout;
    m_outChannelLayout = outChannelLayout;

    char optionsStr[128];
    const AVFilter* format = avfilter_get_by_name("aformat");
    const AVFilter* buffersink = avfilter_get_by_name("abuffersink");

    ALLOC_FILTER(m_formatContext, format, "format");

    sprintf(optionsStr, "sample_fmts=%s:sample_rates=%d:channel_layouts=0x%llx",
            av_get_sample_fmt_name(outSampleFmt), outSampleRate, outChannelLayout);

#if ENABLE_LOG_OPTIONS
    LOG("CAudioFilter::initFormat", optionsStr);
#endif

    INIT_FILTER(initFormat, m_formatContext, optionsStr);


    // buffer sink
    ALLOC_FILTER(m_buffersinkContext, buffersink, "sink");
    INIT_FILTER(initFormat, m_buffersinkContext, NULL);

    return 0;
}

int CAudioFilter::crossfade(CFilterState *state1, CFilterState *state2, float duration) {
#define LINK_FILTERX(last, dest, destPad) { \
    if (CheckFFmpegErr("CAudioFilter::crossfade", avfilter_link(last, 0, dest, destPad))) \
        return FILTER_ERROR_LINK; \
    last = dest; }

    if (createBuffer(&m_bufferContext1))
        return FILTER_ERROR_INIT;

    if (createBuffer(&m_bufferContext2))
        return FILTER_ERROR_INIT;

    AVFilterContext* lastContext1 = m_bufferContext1;
    AVFilterContext* lastContext2 = m_bufferContext2;

#define LINK_COMPRESSOR(num) { \
    if (state##num->bCompressorEnabled) { \
        if (createCompressor(&m_compressorContext##num, state##num)) \
            return FILTER_ERROR_INIT; \
        LINK_FILTERX(lastContext##num, m_compressorContext##num, 0); \
    } }

    LINK_COMPRESSOR(1);
    LINK_COMPRESSOR(2);

#undef LINK_COMPRESSOR

#define LINK_EQUALIZER(num) { \
    if (state##num->bEqualizerEnabled) { \
        int offset = ((num) - 1) * EQBandCount; \
        if (createEqualizer(m_bandContexts + offset, state##num)) \
            return FILTER_ERROR_INIT; \
        \
        for (int i = offset; i < EQBandCount + offset; i++) { \
            LINK_FILTERX(lastContext##num, m_bandContexts[i], 0); \
        } \
    } }

    LINK_EQUALIZER(1);
    LINK_EQUALIZER(2);

#undef LINK_EQUALIZER

    if (createCrossfade(&m_crossfadeContext, duration))
        return FILTER_ERROR_INIT;

    LINK_FILTERX(lastContext1, m_crossfadeContext, 0);
    LINK_FILTERX(lastContext2, m_crossfadeContext, 1);

    LINK_FILTERX(lastContext1, m_formatContext, 0);
    LINK_FILTERX(lastContext1, m_buffersinkContext, 0);

    if (CheckFFmpegErr("CAudioFilter::create", avfilter_graph_config(m_filterGraph, NULL)))
        return FILTER_ERROR_INIT;

    m_created = true;

    return 0;
#undef LINK_FILTERX
}

int CAudioFilter::create(CFilterState* state) {
#define LINK_FILTER(filter) { \
    if (CheckFFmpegErr("CAudioFilter::create", avfilter_link(lastContext, 0, filter, 0))) \
        return FILTER_ERROR_LINK; \
    lastContext = filter; \
}

    if (!m_filterGraph)
        return FILTER_ERROR_INIT;

    if (createBuffer(&m_bufferContext1))
        return FILTER_ERROR_INIT;

    AVFilterContext *lastContext = m_bufferContext1;

    if (state->bCompressorEnabled) {
        if (createCompressor(&m_compressorContext1, state))
            return FILTER_ERROR_INIT;

        LINK_FILTER(m_compressorContext1);
    }

    if (state->bEqualizerEnabled) {
        if (createEqualizer(m_bandContexts, state))
            return FILTER_ERROR_INIT;

        for (int i = 0; i < EQBandCount; i++) {
            LINK_FILTER(m_bandContexts[i]);
        }
    }

    LINK_FILTER(m_formatContext);
    LINK_FILTER(m_buffersinkContext);

    if (CheckFFmpegErr("CAudioFilter::create", avfilter_graph_config(m_filterGraph, NULL)))
        return FILTER_ERROR_INIT;

    m_created = true;

    return 0;
#undef LINK_FILTER
}

void CAudioFilter::release() {
#define FREE_FILTER(filter) { \
    if (filter) \
        avfilter_free(filter); \
    filter = NULL; \
}

    m_created = false;

    for (int i = 0; i < EQBandCount * 2; i++)
        FREE_FILTER(m_bandContexts[i]);

    FREE_FILTER(m_compressorContext1);
    FREE_FILTER(m_compressorContext2);
    FREE_FILTER(m_crossfadeContext);

    FREE_FILTER(m_buffersinkContext);

    FREE_FILTER(m_formatContext);

    FREE_FILTER(m_bufferContext1);
    FREE_FILTER(m_bufferContext2);

    if (m_filterGraph)
        avfilter_graph_free(&m_filterGraph);
    m_filterGraph = NULL;


    LOG("CAudioFilter", "done");

#undef FREE_FILTER
}

size_t CAudioFilter::filterFrame(CFrame* inFrame, uint8_t** buffer, size_t* bufferSize) {
    if (!m_created) {
        LOGE("CAudioFilter::filterFrame", "Filter is not initialized properly.");
        return 0;
    }

    AVFrame* frame = inFrame->getFrame();

    if (m_bufferContext1 && CheckFFmpegErr("CAudioFilter::filterFrame", av_buffersrc_add_frame(m_bufferContext1, frame)))
        return 0;

    if (m_bufferContext2 && CheckFFmpegErr("CAudioFilter::filterFrame", av_buffersrc_add_frame(m_bufferContext2, frame)))
        return 0;

    size_t outBufferSize = 0;
    if (!CheckFFmpegErr("CAudioFilter::filterFrame", av_buffersink_get_frame(m_buffersinkContext, frame))) {
        int planar = av_sample_fmt_is_planar((AVSampleFormat)frame->format);
        int channels = av_get_channel_layout_nb_channels(frame->channel_layout);
        int planes = planar ? channels : 1;
        int bps = av_get_bytes_per_sample((AVSampleFormat)frame->format);
        int plane_size = bps * frame->nb_samples * (planar ? 1 : channels);

        outBufferSize = plane_size * planes;
        av_fast_malloc(buffer, bufferSize, outBufferSize);
        for (int i = 0; i < planes; i++) {
            memcpy((*buffer) + i * plane_size, frame->extended_data[i], plane_size);
        }
    }

    return outBufferSize;
}

int CAudioFilter::createBuffer(AVFilterContext** context) {
    if (m_filterGraph == NULL)
        m_filterGraph = avfilter_graph_alloc();

    char optionsStr[128];
    const AVFilter* filter = avfilter_get_by_name("abuffer");

    sprintf(optionsStr, "time_base=%d/%d:sample_fmt=%s:sample_rate=%d:channel_layout=0x%llx",
            m_timeBase.num, m_timeBase.den,
            av_get_sample_fmt_name(m_inSampleFormat), m_inSampleRate, m_inChannelLayout);

    ALLOC_FILTER(*context, filter, NULL);

    INIT_FILTER(createBuffer, *context, optionsStr);

    return 0;
}

int CAudioFilter::createCrossfade(AVFilterContext** context, float duration) {
    if (m_filterGraph == NULL)
        m_filterGraph = avfilter_graph_alloc();

    char optionsStr[32];
    const AVFilter* filter = avfilter_get_by_name("simple_crossfade");

    sprintf(optionsStr, "d=%f", duration);

    ALLOC_FILTER(*context, filter, NULL);

#if ENABLE_LOG_OPTIONS
    LOG_f("CAudioFilter::createCrossfade", "crossfade: %s", optionsStr);
#endif

    INIT_FILTER(createCrossfade, *context, optionsStr);

    return 0;
}

int CAudioFilter::createCompressor(AVFilterContext** context, CFilterState* state) {
    if (m_filterGraph == NULL)
        m_filterGraph = avfilter_graph_alloc();

    char optionsStr[128];
    const AVFilter* filter = avfilter_get_by_name("acompressor");

    double* args = state->dCompressorArgs;

    snprintf(optionsStr, 128, "%f:%f:%f:%f:%f:%f:%f:%.0f:%.0f:%f", args[0], args[1], args[5], args[2],
             args[3], args[6], args[4], args[7], args[8], args[9]);

    ALLOC_FILTER(*context, filter, NULL);

#if ENABLE_LOG_OPTIONS
    LOG_f("CAudioFilter::createCompressor", "compressor: %s", optionsStr);
#endif

    INIT_FILTER(createCompressor, *context, optionsStr);

    return 0;
}

int CAudioFilter::createEqualizer(AVFilterContext** contexts, CFilterState* state) {
    static int bandFreqs[] = { 31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000 };

    if (m_filterGraph == NULL)
        m_filterGraph = avfilter_graph_alloc();

    char optionsStr[64];
    const AVFilter* equalizer = avfilter_get_by_name("equalizer");
    const AVFilter* bass = avfilter_get_by_name("bass");
    const AVFilter* treble = avfilter_get_by_name("treble");

    float* inBandGains = state->fBandGains;

    for (int i = 0; i < EQBandCount; i++) {
        const AVFilter* filter = equalizer;
        if (i == EQBandBass)
            filter = bass;
        else if (i == EQBandTreble)
            filter = treble;

        ALLOC_FILTER(contexts[i], filter, NULL);

        if (i == EQBandBass || i == EQBandTreble)
            sprintf(optionsStr, "g=%f", inBandGains[i]);
        else
            sprintf(optionsStr, "f=%d:width_type=o:w=1:g=%f", bandFreqs[i], inBandGains[i]);

#if ENABLE_LOG_OPTIONS
        LOG_f("CAudioFilter::createEqualizer", "band %d: %s", i, optionsStr);
#endif

        INIT_FILTER(createEqualizer, contexts[i], optionsStr);
    }

    return 0;
}