//
// Created by Tarik on 18.07.2016.
//

#ifndef MUSICPLAYER_JNIFILTERSTATE_H
#define MUSICPLAYER_JNIFILTERSTATE_H

#include "JNIHelper.h"

DECL_JNI_FUNC(jint, playback, FilterState, nNewInstance);
DECL_JNI_FUNC(void, playback, FilterState, nReleaseInstance, jint handle);
DECL_JNI_FUNC(void, playback, FilterState, nSetEqualizer,    jint handle, jboolean, jfloatArray);
DECL_JNI_FUNC(void, playback, FilterState, nSetCompressor,   jint handle, jboolean, jdoubleArray);

#endif //MUSICPLAYER_JNIFILTERSTATE_H
