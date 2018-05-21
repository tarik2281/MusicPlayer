//
// Created by Tarik Karaca on 23.02.16.
//

#ifndef MUSICPLAYER_CONFIG_H
#define MUSICPLAYER_CONFIG_H

#ifdef __cplusplus
extern "C" {
#endif

#include "libavutil/channel_layout.h"
#include "libavutil/samplefmt.h"
#include "libavutil/pixfmt.h"

#ifdef __cplusplus
};
#endif

#define DEFAULT_NUM_CHANNELS 2
#define DEFAULT_CHANNEL_LAYOUT AV_CH_LAYOUT_STEREO
#define DEFAULT_SAMPLE_FMT AV_SAMPLE_FMT_S16
#define DEFAULT_PIX_FMT AV_PIX_FMT_RGB565LE
#define DEFAULT_PIX_FMT_BYTES 2

#endif //MUSICPLAYER_CONFIG_H
