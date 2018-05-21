//
// Created by Tarik on 07.07.2016.
//

#ifndef MUSICPLAYER_CFILTERSTATE_H
#define MUSICPLAYER_CFILTERSTATE_H

#include "IObject.h"

#define FILTER_STATE_EQ_BANDS 12
#define FILTER_STATE_COMP_ARGS 10

class CFilterState : public IObject {

public:

    bool bEqualizerEnabled;
    float fBandGains[FILTER_STATE_EQ_BANDS];

    bool bCompressorEnabled;
    double dCompressorArgs[FILTER_STATE_COMP_ARGS];

    CFilterState() : bEqualizerEnabled(false),
                     fBandGains(),
                     bCompressorEnabled(false),
                     dCompressorArgs() {

    }

protected:

    virtual void onRelease() {

    }

};

#endif //MUSICPLAYER_CFILTERSTATE_H
