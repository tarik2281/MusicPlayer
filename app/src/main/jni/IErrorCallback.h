//
// Created by Tarik on 31.07.2016.
//

#ifndef MUSICPLAYER_IERRORCALLBACK_H
#define MUSICPLAYER_IERRORCALLBACK_H

#include "IObject.h"

class IErrorCallback : public IObject {

public:

    IErrorCallback() : IObject() {

    }

    void call(int errorCode, ...) {
        va_list list;
        va_start(list, errorCode);
        onCall(errorCode, list);
        va_end(list);
    }

protected:

    virtual void onCall(int errorCode, va_list list) = 0;

};

#endif //MUSICPLAYER_IERRORCALLBACK_H
