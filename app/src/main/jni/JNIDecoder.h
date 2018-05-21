//
// Created by Tarik on 15.11.2015.
//

#ifndef MUSICPLAYER_JNIDECODER_H
#define MUSICPLAYER_JNIDECODER_H

#include "JNIHelper.h"

DECL_JNI_FUNC(void,     io, Decoder, nInit);
DECL_JNI_FUNC(jint,     io, Decoder, nNewInstance);
DECL_JNI_FUNC(void,     io, Decoder, nReleaseInstance, jint);
DECL_JNI_FUNC(void,     io, Decoder, nSetVideoSize,    jint, jint w, jint h);
DECL_JNI_FUNC(jboolean, io, Decoder, nExtractAlbumArt, jint, jint tagHandle, jbyteArray output);

#endif //MUSICPLAYER_JNIDECODER_H
