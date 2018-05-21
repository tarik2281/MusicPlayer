
TAGLIB_PATH := $(call my-dir)

include $(CLEAR_VARS)

SOURCES := $(wildcard $(TAGLIB_PATH)/*.cpp)
SOURCES += $(wildcard $(TAGLIB_PATH)/**/*.cpp)
SOURCES += $(wildcard $(TAGLIB_PATH)/**/**/*.cpp)
SOURCES += $(wildcard $(TAGLIB_PATH)/**/**/**/*.cpp)

HEADERS := $(TAGLIB_PATH)
HEADERS += $(sort $(dir $(wildcard $(TAGLIB_PATH)/**/)))
HEADERS += $(sort $(dir $(wildcard $(TAGLIB_PATH)/**/**/)))
HEADERS += $(sort $(dir $(wildcard $(TAGLIB_PATH)/**/**/**/)))

LOCAL_MODULE := taglib
LOCAL_SRC_FILES := $(SOURCES)
LOCAL_C_INCLUDES := $(HEADERS)
LOCAL_EXPORT_C_INCLUDES := $(HEADERS)

include $(BUILD_STATIC_LIBRARY)