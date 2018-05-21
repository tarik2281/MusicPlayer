//
// Created by Tarik Karaca on 29.02.16.
//

#ifndef MUSICPLAYER_JNIMEDIATAG_H
#define MUSICPLAYER_JNIMEDIATAG_H

#include "JNIHelper.h"

DECL_JNI_FUNC(jint,     io, MediaTag, nNewInstance);
DECL_JNI_FUNC(void,     io, MediaTag, nReleaseInstance,   jint handle);
DECL_JNI_FUNC(jboolean, io, MediaTag, nOpen,              jint handle, jint fileType, jstring filePath);
DECL_JNI_FUNC(jboolean, io, MediaTag, nOpenFd,            jint handle, jint fileType, jint fd);
DECL_JNI_FUNC(void,     io, MediaTag, nClose,             jint handle);
DECL_JNI_FUNC(jstring,  io, MediaTag, nGetMetadata,       jint handle, jint metaKey);
DECL_JNI_FUNC(jint,     io, MediaTag, nGetProperty,       jint handle, jint propKey);
DECL_JNI_FUNC(jboolean, io, MediaTag, nHasAlbumArt,       jint handle);
DECL_JNI_FUNC(jint,     io, MediaTag, nGetAlbumArtType,   jint handle);
DECL_JNI_FUNC(jint,     io, MediaTag, nGetAlbumArtWidth,  jint handle);
DECL_JNI_FUNC(jint,     io, MediaTag, nGetAlbumArtHeight, jint handle);
DECL_JNI_FUNC(jboolean, io, MediaTag, nExtractAlbumArt,   jint handle, jstring filePath);
DECL_JNI_FUNC(jboolean, io, MediaTag, nExtractAlbumArtFd, jint handle, jint fd);

DECL_JNI_FUNC(void,     io, MediaTag, nSetMetadata,       jint handle, jint metaKey, jstring value);
DECL_JNI_FUNC(void,     io, MediaTag, nSetAlbumArt,       jint handle, jstring filePath, jint imageType);
DECL_JNI_FUNC(void,     io, MediaTag, nSetAlbumArtFd,     jint handle, jint fd, jint imageType);
DECL_JNI_FUNC(void,     io, MediaTag, nRemoveAlbumArt,    jint handle);
DECL_JNI_FUNC(jboolean, io, MediaTag, nSave,              jint handle);

#endif //MUSICPLAYER_JNIMEDIATAG_H
