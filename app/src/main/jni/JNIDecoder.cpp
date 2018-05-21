//
// Created by Tarik on 15.11.2015.
//

#include "JNIDecoder.h"

#include "CDecoder.h"
#include "CFrame.h"
#include "CStream.h"
#include "CMediaTag.h"
#include "CMediaContext.h"
#include "log.h"

extern "C" {

#include "libavfilter/avfilter.h"

}

#include "tdebuglistener.h"

#ifdef _DEBUG
#define ENABLE_AV_LOG 1
#else
#define ENABLE_AV_LOG 0
#endif

#define ENABLE_AV_VERBOSE_LOGS 0
#define ENABLE_AV_INFO_LOGS 1
#define ENABLE_AV_WARN_LOGS 1
#define ENABLE_AV_ERROR_LOGS 1

#if ENABLE_AV_LOG
void avLogCallback(void*, int level, const char* fmt, va_list vl) {
    switch (level) {
        case AV_LOG_WARNING:
#if ENABLE_AV_WARN_LOGS
            LOGW_f("AV_LOG", fmt, vl);
#endif
            break;
        case AV_LOG_PANIC:
        case AV_LOG_FATAL:
        case AV_LOG_ERROR:
#if ENABLE_AV_ERROR_LOGS
            LOGE_f("AV_LOG", fmt, vl);
#endif
            break;
        case AV_LOG_INFO:
#if ENABLE_AV_INFO_LOGS
            LOG_f("AV_LOG", fmt, vl);
#endif
            break;
        case AV_LOG_VERBOSE:
#if ENABLE_AV_VERBOSE_LOGS
            LOG_f("AV_LOG", fmt, vl);
#endif
            break;
    }
}
#endif // ENABLE_AV_LOG

class DebugListener : public TagLib::DebugListener {
public:
    virtual void printMessage(const TagLib::String& message) {
        LOG_f("TagLib", "%s", message.toCString(true));
    }
};

DebugListener g_debugListener;

DEF_JNI_FUNC(void, io, Decoder, nInit) {
    av_register_all();
    avfilter_register_all();

#if ENABLE_AV_LOG
    av_log_set_callback(avLogCallback);
#endif

    TagLib::setDebugListener(&g_debugListener);
}

DEF_JNI_FUNC(jint, io, Decoder, nNewInstance) {
    CDecoder* decoder = new CDecoder();

    return (jint)decoder;
}

DEF_JNI_FUNC(void, io, Decoder, nReleaseInstance, jint handle) {
    CDecoder* decoder = (CDecoder*)handle;

    decoder->release();

    delete decoder;
}

DEF_JNI_FUNC(void, io, Decoder, nSetVideoSize, jint handle, jint width, jint height) {
    CDecoder* decoder = (CDecoder*)handle;

    decoder->setOutVideoSize(width, height);
}

DEF_JNI_FUNC(jboolean, io, Decoder, nExtractAlbumArt, jint decHandle, jint tagHandle, jbyteArray output) {
    CDecoder* decoder = (CDecoder*)decHandle;
    CMediaTag* tag = (CMediaTag*)tagHandle;
    CMediaContext* context = tag->getAlbumArtContext();

    if (context) {
        int streamIndex = context->getDefaultVideoIndex();
        if (streamIndex < 0)
            return JNI_FALSE;

        CStream* stream = context->getStream(streamIndex);
        if (stream->getDataType() != DATA_TYPE_VIDEO)
            return JNI_FALSE;

        stream->openCodec();

        CFrame* frame = stream->getCachedFrame();
        uint8_t* buffer = NULL;
        int bufferSize = 0;

        do {
            if (frame && frame->getStream()->getIndex() == streamIndex) {
                decoder->setVideoCodec(frame->getStream()->getContext());
                buffer = decoder->decodeVideoFrame(frame, &bufferSize);
            }

            if (!buffer)
                frame = context->readNextFrame();

        } while (!buffer && !context->eof());

        if (buffer)
            env->SetByteArrayRegion(output, 0, bufferSize, (const jbyte*)buffer);

        return (jboolean)!!buffer;
    }

    return JNI_FALSE;
}
