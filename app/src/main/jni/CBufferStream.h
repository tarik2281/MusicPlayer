//
// Created by Tarik Karaca on 01.03.16.
//

#ifndef MUSICPLAYER_CBUFFERSTREAM_H
#define MUSICPLAYER_CBUFFERSTREAM_H

#include <stdint.h>

class CBufferStream {

public:

    CBufferStream();
    ~CBufferStream();

    bool hasSource() const;
    bool eof() const;

    void setSource(const uint8_t* buffer, size_t size);
    void setNullSource();

    size_t read(uint8_t* dest, size_t size);
    int64_t seek(int64_t offset);

private:

    const uint8_t* m_buffer;
    size_t m_size;

    const uint8_t* m_currentPosition;
    size_t m_sizeLeft;

};

#endif //MUSICPLAYER_CBUFFERSTREAM_H
