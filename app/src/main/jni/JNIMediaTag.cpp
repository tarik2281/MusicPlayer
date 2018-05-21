//
// Created by Tarik Karaca on 29.02.16.
//

#include "JNIMediaTag.h"

#include "CMediaTag.h"
#include "CMediaContext.h"
#include "log.h"

#include "taglib/toolkit/tstring.h"
#include "CStream.h"

#include <stdio.h>
#include <memory.h>

DEF_JNI_FUNC(jint, io, MediaTag, nNewInstance) {
    return (jint)new CMediaTag();
}

DEF_JNI_FUNC(void, io, MediaTag, nReleaseInstance, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    delete tag;
}

DEF_JNI_FUNC(jboolean, io, MediaTag, nOpen, jint handle, jint fileType, jstring filePath) {
    CMediaTag* tag = (CMediaTag*)handle;

    jboolean result = JNI_FALSE;

    if (filePath) {
        const char *nFilePath = env->GetStringUTFChars(filePath, NULL);

        if (tag->open((CMediaTag::FileType)fileType, nFilePath) >= 0)
            result = JNI_TRUE;

        env->ReleaseStringUTFChars(filePath, nFilePath);
    }

    return result;
}

DEF_JNI_FUNC(jboolean, io, MediaTag, nOpenFd, jint handle, jint fileType, jint fd) {
    CMediaTag* tag = (CMediaTag*)handle;

    jboolean result = JNI_FALSE;

    if (tag->open((CMediaTag::FileType)fileType, fd) >= 0)
        result = JNI_TRUE;

    return result;
}

DEF_JNI_FUNC(void, io, MediaTag, nClose, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    tag->close();
}

DEF_JNI_FUNC(jstring, io, MediaTag, nGetMetadata, jint handle, jint metaKey) {
    CMediaTag* tag = (CMediaTag*)handle;

    TagLib::String value = tag->getMetadata((CMediaTag::MetaKeys)metaKey);

    return env->NewStringUTF(value.toCString(true));
}

DEF_JNI_FUNC(jint, io, MediaTag, nGetProperty, jint handle, jint propKey) {
    CMediaTag* tag = (CMediaTag*)handle;

    return tag->getProperty((CMediaTag::PropKeys)propKey);
}

DEF_JNI_FUNC(jboolean, io, MediaTag, nHasAlbumArt, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    return (jboolean)!tag->getAlbumArtData().isEmpty();
}

DEF_JNI_FUNC(jint, io, MediaTag, nGetAlbumArtType, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    return (jint)tag->getAlbumArtType();
}

DEF_JNI_FUNC(jint, io, MediaTag, nGetAlbumArtWidth, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    CMediaContext* context = tag->getAlbumArtContext();
    if (!context)
        return 0;

    CStream* stream = context->getStream(context->getDefaultVideoIndex());

    return stream ? stream->getWidth() : 0;
}

DEF_JNI_FUNC(jint, io, MediaTag, nGetAlbumArtHeight, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    CMediaContext* context = tag->getAlbumArtContext();
    if (!context)
        return 0;

    CStream* stream = context->getStream(context->getDefaultVideoIndex());

    return stream ? stream->getHeight() : 0;
}

DEF_JNI_FUNC(jboolean, io, MediaTag, nExtractAlbumArt, jint handle, jstring filePath) {
    CMediaTag* tag = (CMediaTag*)handle;

    TagLib::ByteVector data = tag->getAlbumArtData();

    jboolean result = JNI_FALSE;

    const char* nFilePath = NULL;

    if (filePath != NULL && !data.isNull()) {
        nFilePath = env->GetStringUTFChars(filePath, NULL);

        FILE *file = fopen(nFilePath, "wb");
        if (file) {
            fwrite(data.data(), 1, data.size(), file);

            if (!ferror(file))
                result = JNI_TRUE;

            fclose(file);
        }

        env->ReleaseStringUTFChars(filePath, nFilePath);
    }

    return result;
}

DEF_JNI_FUNC(jboolean, io, MediaTag, nExtractAlbumArtFd, jint handle, jint fd) {
    CMediaTag* tag = (CMediaTag*)handle;

    TagLib::ByteVector data = tag->getAlbumArtData();

    jboolean result = JNI_FALSE;

    if (!data.isNull()) {
        FILE* file = fdopen(fd, "wb");
        if (file) {
            fwrite(data.data(), 1, data.size(), file);

            if (!ferror(file))
                result = JNI_TRUE;

            fclose(file);
        }
    }

    return result;
}

DEF_JNI_FUNC(void, io, MediaTag, nSetMetadata, jint handle, jint metaKey, jstring value) {
    CMediaTag* tag = (CMediaTag*)handle;

    TagLib::String sValue = TagLib::String();
    const char* nValue = NULL;

    if (value != NULL) {
        nValue = env->GetStringUTFChars(value, NULL);
        sValue = TagLib::String(nValue, TagLib::String::Type::UTF8);
    }

    tag->setMetadata((CMediaTag::MetaKeys)metaKey, sValue);

    if (nValue)
        env->ReleaseStringUTFChars(value, nValue);
}

static void setAlbumArt(CMediaTag* tag, FILE* file, CMediaTag::ImageType type) {
    fseek(file, 0, SEEK_END);
    long position = ftell(file);

    if (position >= 0) {
        size_t size = (size_t)position;
        uint8_t *data = (uint8_t *) malloc(size);
        fseek(file, 0, SEEK_SET);
        fread(data, 1, size, file);

        TagLib::ByteVector vector((const char*)data, size);
        tag->setAlbumArtData(vector, type);

        free(data);
    }
}

DEF_JNI_FUNC(void, io, MediaTag, nSetAlbumArt, jint handle, jstring filePath, jint imageType) {
    CMediaTag* tag = (CMediaTag*)handle;

    const char* nFilePath = NULL;

    if (filePath != NULL) {
        nFilePath = env->GetStringUTFChars(filePath, NULL);

        FILE *file = fopen(nFilePath, "rb");

        if (file) {
            setAlbumArt(tag, file, (CMediaTag::ImageType)imageType);

            fclose(file);
        }

        env->ReleaseStringUTFChars(filePath, nFilePath);
    }
    else
        tag->removeAlbumArt();
}

DEF_JNI_FUNC(void, io, MediaTag, nSetAlbumArtFd, jint handle, jint fd, jint imageType) {
    CMediaTag* tag = (CMediaTag*)handle;

    FILE* file = fdopen(fd, "rb");

    if (file) {
        setAlbumArt(tag, file, (CMediaTag::ImageType)imageType);

        fclose(file);
    }
}

DEF_JNI_FUNC(void, io, MediaTag, nRemoveAlbumArt, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    tag->removeAlbumArt();
}

DEF_JNI_FUNC(jboolean, io, MediaTag, nSave, jint handle) {
    CMediaTag* tag = (CMediaTag*)handle;

    return (jboolean)tag->save();
}
