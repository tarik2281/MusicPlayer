//
// Created by Tarik on 15.11.2015.
//

#ifndef MUSICPLAYER_JNIMEDIAPLAYER_H
#define MUSICPLAYER_JNIMEDIAPLAYER_H

#include "JNIHelper.h"

DECL_JNI_FUNC(jint,     playback, MediaPlayer, nNewInstance,        jint sampleRate);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nReleaseInstance,    jint);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nSetNextData,        jint, jstring path, jint stateHandle, jboolean forceClear);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nSetFinishCallback,  jint, jobject cb);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nSetErrorCallback,   jint, jobject cb);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nStartDecodeThread,  jint);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nStopDecodeThread,   jint);
DECL_JNI_FUNC(void,     playback, MediaPlayer, nSeek,               jint, jlong offset);
DECL_JNI_FUNC(jlong,    playback, MediaPlayer, nGetCurrentPosition, jint);
DECL_JNI_FUNC(jboolean, playback, MediaPlayer, nFillStreamArray,    jint, jbyteArray array);
DECL_JNI_FUNC(jboolean, playback, MediaPlayer, nFillStreamBuffer,   jint, jobject buffer);

#endif //MUSICPLAYER_JNIMEDIAPLAYER_H
