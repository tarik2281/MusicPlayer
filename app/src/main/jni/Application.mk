NDK_DEBUG := 1
APP_ABI := armeabi-v7a
APP_STL := gnustl_static
APP_CPPFLAGS += -std=c++11 -frtti -Wno-deprecated-declarations -D__STDC_CONSTANT_MACROS
APP_PLATFORM := android-23