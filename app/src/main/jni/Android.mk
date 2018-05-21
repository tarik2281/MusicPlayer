
LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/ffmpeg/Android.mk
include $(LOCAL_PATH)/taglib/Android.mk

include $(CLEAR_VARS)

SOURCES := $(wildcard $(LOCAL_PATH)/*.cpp)

LOCAL_MODULE    := MusicPlayer
LOCAL_SRC_FILES := $(SOURCES)
LOCAL_LDLIBS += -llog -latomic
LOCAL_STATIC_LIBRARIES := libavfilter libavformat libavcodec libswresample libswscale libavutil taglib
LOCAL_LDLIBS += -lm -lz

include $(BUILD_SHARED_LIBRARY)
