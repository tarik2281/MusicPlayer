//
// Created by Tarik on 03.08.2016.
//

#ifndef MUSICPLAYER_JNIHELPER_H
#define MUSICPLAYER_JNIHELPER_H

extern "C" {

#include <jni.h>

}

#define APP_PACKAGE Java_com_example_musicplayer

#define __JNIH_PASTEW(a,b) a##_##b
#define __JNIH_PASTE(a,b) __JNIH_PASTEW(a,b)
#define GET_PKG(pkg) __JNIH_PASTE(APP_PACKAGE, pkg)
#define GET_CLASS(pkg, clazz) __JNIH_PASTE(GET_PKG(pkg), clazz)
#define GET_FUNC(pkg, clazz, name) __JNIH_PASTE(GET_CLASS(pkg, clazz), name)
#define DECL_JNI_FUNC(ret, pkg, clazz, name, ...) extern "C" JNIEXPORT ret JNICALL GET_FUNC(pkg, clazz, name)(JNIEnv*, jobject, ##__VA_ARGS__)
#define DEF_JNI_FUNC(ret, pkg, clazz, name, ...) ret GET_FUNC(pkg, clazz, name)(JNIEnv* env, jobject obj, ##__VA_ARGS__)

#endif //MUSICPLAYER_JNIHELPER_H
