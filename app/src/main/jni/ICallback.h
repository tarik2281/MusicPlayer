//
// Created by Tarik Karaca on 05.03.16.
//

#ifndef MUSICPLAYER_ICALLBACK_H
#define MUSICPLAYER_ICALLBACK_H

#include "IObject.h"

class ICallback : public IObject {

public:

    ICallback() : IObject() {

    }

    virtual void call() = 0;

};

#endif //MUSICPLAYER_ICALLBACK_H
