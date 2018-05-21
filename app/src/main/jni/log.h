//
// Created by Tarik Karaca on 01.03.16.
//

#ifndef MUSICPLAYER_LOG_H
#define MUSICPLAYER_LOG_H

extern "C" {

#include "libavutil/avutil.h"

#include <android/log.h>
#include <SLES/OpenSLES.h>

};

#ifdef _DEBUG

#define LOG(tag, text) ((void)__android_log_write(ANDROID_LOG_INFO, tag, text))
#define LOG_f(tag, ...) ((void)__android_log_print(ANDROID_LOG_INFO, tag, __VA_ARGS__))

#define LOGW(tag, text) ((void)__android_log_write(ANDROID_LOG_WARN, tag, text))
#define LOGW_f(tag, ...) ((void)__android_log_print(ANDROID_LOG_WARN, tag, __VA_ARGS__))

#define LOGE(tag, text) ((void)__android_log_write(ANDROID_LOG_ERROR, tag, text))
#define LOGE_f(tag, ...) ((void)__android_log_print(ANDROID_LOG_ERROR, tag, __VA_ARGS__))

#define assert(cond) do { if (!(cond)) __android_log_assert(#cond, "Assert", "Assertion failure for file %s at line %d \n Condition not met: %s", __FILE__, __LINE__, #cond); } while(0)

/*
 * \return Returns zero if no error occurred
 */
inline int CheckFFmpegErr(const char* tag, int errCode) {
    bool error = errCode < 0;

    if (error && errCode != AVERROR(EAGAIN) && errCode != AVERROR_EOF) {
        char str[128];
        av_strerror(errCode, str, 128);
        LOGE(tag, str);
    }

    return error;
}

/*
 * \return Returns zero if no error occurred
 */
inline int CheckSLResult(const char* tag, SLresult result) {
    bool error = result != SL_RESULT_SUCCESS;

    if (error) {
        const char* str;

        switch (result) {
            case SL_RESULT_PRECONDITIONS_VIOLATED:
                str = "SLES: Precondition violation";
                break;
            case SL_RESULT_PARAMETER_INVALID:
                str = "SLES: Invalid parameter";
                break;
            case SL_RESULT_MEMORY_FAILURE:
                str = "SLES: Memory failure";
                break;
            case SL_RESULT_RESOURCE_ERROR:
                str = "SLES: Lack of resources";
                break;
            case SL_RESULT_IO_ERROR:
                str = "SLES: I/O error";
                break;
            case SL_RESULT_BUFFER_INSUFFICIENT:
                str = "SLES: Insufficient buffer";
                break;
            case SL_RESULT_CONTENT_CORRUPTED:
                str = "SLES: Corrupted content";
                break;
            case SL_RESULT_CONTENT_UNSUPPORTED:
                str = "SLES: Unsupported content";
                break;
            case SL_RESULT_CONTENT_NOT_FOUND:
                str = "SLES: Content not found";
                break;
            case SL_RESULT_FEATURE_UNSUPPORTED:
                str = "SLES: Unsupported feature";
                break;
            case SL_RESULT_INTERNAL_ERROR:
                str = "SLES: Internal error";
                break;
            case SL_RESULT_UNKNOWN_ERROR:
                str = "SLES: Unknown error";
                break;
            case SL_RESULT_OPERATION_ABORTED:
                str = "SLES: Operation aborted";
                break;
            case SL_RESULT_CONTROL_LOST:
                str = "SLES: Control lost";
                break;
            default:
                str = "Error code not found";
                break;
        }

        LOGE(tag, str);
    }

    return error;
}

#else

#define LOG(tag, text)
#define LOG_f(tag, ...)

#define LOGW(tag, text)
#define LOGW_f(tag, ...)

#define LOGE(tag, text)
#define LOGE_f(tag, ...)

#define assert(cond) ((void)0)

/*
 * Returns zero if no error occurred
 */
#define CheckFFmpegErr(tag, errCode) (errCode < 0)

/*
 * Returns zero if no error occurred
 */
#define CheckSLResult(tag, result) (result != SL_RESULT_SUCCESS)

#endif

#endif //MUSICPLAYER_LOG_H
