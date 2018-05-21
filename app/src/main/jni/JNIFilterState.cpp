//
// Created by Tarik on 18.07.2016.
//

#include <stddef.h>
#include <memory>
#include "JNIFilterState.h"

#include "CFilterState.h"

DEF_JNI_FUNC(jint, playback, FilterState, nNewInstance) {
    return (jint)new CFilterState();
}

DEF_JNI_FUNC(void, playback, FilterState, nReleaseInstance, jint handle) {
    CFilterState* state = (CFilterState*)handle;

    SAFE_RELEASE(state);
}

DEF_JNI_FUNC(void, playback, FilterState, nSetEqualizer, jint handle, jboolean enabled, jfloatArray gains) {
    CFilterState* state = (CFilterState*)handle;

    state->bEqualizerEnabled = enabled;

    if (gains) {
        jfloat *nGains = env->GetFloatArrayElements(gains, NULL);
        memcpy(state->fBandGains, nGains, sizeof(float) * FILTER_STATE_EQ_BANDS);
        env->ReleaseFloatArrayElements(gains, nGains, 0);
    }
}

DEF_JNI_FUNC(void, playback, FilterState, nSetCompressor, jint handle, jboolean enabled, jdoubleArray args) {
    CFilterState* state = (CFilterState*)handle;

    state->bCompressorEnabled = enabled;

    if (args) {
        jdouble *nArgs = env->GetDoubleArrayElements(args, NULL);
        memcpy(state->dCompressorArgs, nArgs, sizeof(double) * FILTER_STATE_COMP_ARGS);
        env->ReleaseDoubleArrayElements(args, nArgs, 0);
    }
}