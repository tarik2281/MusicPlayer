//
// Created by Tarik on 15.11.2015.
//

#include "JNIMediaPlayer.h"

#include "CMediaStream.h"
#include "CMediaPlayer.h"
#include "ICallback.h"
#include "IErrorCallback.h"
#include "log.h"

class JNIBaseCallback {

public:

    JNIBaseCallback() : m_vm(NULL),
                        m_env(NULL),
                        m_object(NULL),
                        m_methodId(0) {

    }

    void attachThread() {
        m_vm->AttachCurrentThread(&m_env, NULL);
    }

    void detachThread() {
        m_vm->DetachCurrentThread();
    }

    JNIEnv* getEnv() {
        return m_env;
    }

    void setEnv(JNIEnv* env) {
        m_uiEnv = env;
        m_uiEnv->GetJavaVM(&m_vm);
    }

    void setMethod(jobject object, const char* name, const char* sig) {
        release();

        jclass clazz = m_uiEnv->GetObjectClass(object);

        m_object = m_uiEnv->NewGlobalRef(object);
        m_methodId = m_uiEnv->GetMethodID(clazz, name, sig);
    }

    void callMethod(bool attach, ...) {
        if (attach)
            attachThread();

        va_list args;
        va_start(args, attach);
        m_env->CallVoidMethodV(m_object, m_methodId, args);
        va_end(args);

        if (attach)
            detachThread();
    }

    void release() {
        if (m_object)
            m_uiEnv->DeleteGlobalRef(m_object);
    }

private:

    JavaVM* m_vm;
    JNIEnv* m_env;
    JNIEnv* m_uiEnv;
    jobject m_object;
    jmethodID  m_methodId;

};

class JNICallback : public ICallback {

public:

    JNICallback() : ICallback(), m_callback() {

    }

    ~JNICallback() {

    }

    void initialize(JNIEnv* env, jobject object) {
        m_callback.setEnv(env);
        m_callback.setMethod(object, "run", "()V");
    }

    virtual void call() {
        m_callback.callMethod(true);
    }

protected:

    virtual void onRelease() {
        m_callback.release();
    }

private:

    JNIBaseCallback m_callback;

};

class JNIErrorCallback : public IErrorCallback {

public:

    JNIErrorCallback() : IErrorCallback(), m_callback() {

    }

    void initialize(JNIEnv* env, jobject object) {
        m_callback.setEnv(env);
        m_callback.setMethod(object, "onError", "(I[Ljava/lang/Object;)V");

    }

protected:

    virtual void onCall(int errorCode, va_list args) {
        jobjectArray objects = NULL;

        switch (errorCode) {
            case ERROR_INVALID_FILE: {
                m_callback.attachThread();
                JNIEnv* env = m_callback.getEnv();

                const char* nFilePath = va_arg(args, const char*);
                jstring filePath = env->NewStringUTF(nFilePath);

                jclass clazz = env->FindClass("java/lang/Object");
                objects = env->NewObjectArray(1, clazz, filePath);

                m_callback.callMethod(false, errorCode, objects);

                m_callback.detachThread();
                break;
            }
            default:
                m_callback.callMethod(true, errorCode, NULL);
                break;
        }
    }

    virtual void onRelease() {
        m_callback.release();
    }

private:

    JNIBaseCallback m_callback;

};

DEF_JNI_FUNC(jint, playback, MediaPlayer, nNewInstance, jint sampleRate) {
    CMediaStream* stream = new CMediaStream();

    if (stream->initialize((uint32_t)sampleRate) < 0) {
        stream->release();
        delete stream;
        stream = NULL;
    }

    return (jint)stream;
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nReleaseInstance, jint handle) {
    CMediaStream* stream = (CMediaStream*)handle;

    if (stream) {
        stream->release();
        delete stream;
    }
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nSetNextData, jint playerHandle, jstring path, jint stateHandle, jboolean forceClear) {
    CMediaStream* stream = (CMediaStream*)playerHandle;
    CFilterState* filter = (CFilterState*)stateHandle;

    if (path) {
        const char* nPath = env->GetStringUTFChars(path, NULL);

        stream->setNextData(nPath, filter, forceClear);

        env->ReleaseStringUTFChars(path, nPath);
    }
    else
        stream->setNextData(NULL, filter, forceClear);
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nSetFinishCallback, jint handle, jobject cb) {
    CMediaStream* stream = (CMediaStream*)handle;

    JNICallback* callback = new JNICallback();

    callback->initialize(env, cb);

    stream->setFinishCallback(callback);

    callback->release();
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nSetErrorCallback, jint handle, jobject cb) {
    CMediaStream* stream = (CMediaStream*)handle;

    JNIErrorCallback* callback = new JNIErrorCallback();

    callback->initialize(env, cb);

    stream->setErrorCallback(callback);

    callback->release();
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nStartDecodeThread, jint handle) {
    CMediaStream* stream = (CMediaStream*)handle;

    stream->startDecodeThread();
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nStopDecodeThread, jint handle) {
    CMediaStream* stream = (CMediaStream*)handle;

    stream->stopDecodeThread();
}

DEF_JNI_FUNC(void, playback, MediaPlayer, nSeek, jint handle, jlong offset) {
    CMediaStream* stream = (CMediaStream*)handle;

    stream->seek(offset);
}

DEF_JNI_FUNC(jlong, playback, MediaPlayer, nGetCurrentPosition, jint handle) {
    CMediaStream* stream = (CMediaStream*)handle;

    return (jlong)stream->getCurrentPosition();
}

DEF_JNI_FUNC(jboolean, playback, MediaPlayer, nFillStreamArray, jint handle, jbyteArray array) {
    CMediaStream* stream = (CMediaStream*)handle;

    jsize size = env->GetArrayLength(array);
    jbyte* buf = env->GetByteArrayElements(array, NULL);

    jboolean res = (jboolean)stream->fillStreamBuffer((char*)buf, size);

    env->ReleaseByteArrayElements(array, buf, 0);

    return res;
}

DEF_JNI_FUNC(jboolean, playback, MediaPlayer, nFillStreamBuffer, jint handle, jobject buffer) {
    CMediaStream* stream = (CMediaStream*)handle;

    jbyte* buf = (jbyte*)env->GetDirectBufferAddress(buffer);
    jlong size = env->GetDirectBufferCapacity(buffer);

    return (jboolean)stream->fillStreamBuffer((char*)buf, size);
}