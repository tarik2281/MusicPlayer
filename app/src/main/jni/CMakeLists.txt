cmake_minimum_required(VERSION 3.4.1)

include (${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/CMakeLists.txt)
include (taglib/CMakeLists.txt)

set ( main_SRCS
    CAudioFilter.cpp
    CBufferStream.cpp
    CDecoder.cpp
    CFilterState.cpp
    CFrame.cpp
    CMediaContext.cpp
    CMediaStream.cpp
    CMediaTag.cpp
    CStream.cpp
    JNIDecoder.cpp
    JNIFilterState.cpp
    JNIMediaPlayer.cpp
    JNIMediaTag.cpp )

find_library(lib-log log)
find_library(lib-m m)
find_library(lib-z z)

add_library( MusicPlayer SHARED ${main_SRCS} )

target_link_libraries(  MusicPlayer
                        ${lib-log}
                        ${lib-m}
                        ${lib-z}
                        tag
                        libavfilter
                        libavcodec
                        libavformat
                        libavutil
                        libswscale
                        libswresample )