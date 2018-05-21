//
// Created by Tarik on 31.07.2016.
//

#ifndef MUSICPLAYER_IOBJECT_H
#define MUSICPLAYER_IOBJECT_H

#define SAFE_RELEASE(object) { object->release(); object = NULL; }

class IObject {
public:

    IObject() : m_numRefs(1) {

    }

    void retain();
    void release();

protected:

    virtual void onRelease() = 0;

private:

    int m_numRefs;

};

inline void IObject::retain() {
    m_numRefs++;
}

inline void IObject::release() {
    if (--m_numRefs == 0) {
        onRelease();
        delete this;
    }
}

#endif //MUSICPLAYER_IOBJECT_H
