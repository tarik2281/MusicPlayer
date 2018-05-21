set(CMAKE_INCLUDE_CURRENT_DIR ON)

include_directories( ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/include )

add_library(            libavfilter SHARED IMPORTED )
set_target_properties(  libavfilter
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}/libavfilter.so )

add_library(            libavcodec SHARED IMPORTED )
set_target_properties(  libavcodec
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}/libavcodec.so )

add_library(            libavformat SHARED IMPORTED )
set_target_properties(  libavformat
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}/libavformat.so )

add_library(            libavutil SHARED IMPORTED )
set_target_properties(  libavutil
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}/libavutil.so )

add_library(            libswresample SHARED IMPORTED )
set_target_properties(  libswresample
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}/libswresample.so )

add_library(            libswscale SHARED IMPORTED )
set_target_properties(  libswscale
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}/libswscale.so )