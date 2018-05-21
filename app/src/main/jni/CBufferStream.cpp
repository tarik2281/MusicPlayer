//
// Created by Tarik Karaca on 01.03.16.
//

#include "CBufferStream.h"

#include <string.h>

#define min(a, b) ((a) < (b) ? (a) : (b))
#define max(a, b) ((a) > (b) ? (a) : (b))

CBufferStream::CBufferStream() {
    setNullSource();
}

CBufferStream::~CBufferStream() {

}

bool CBufferStream::hasSource() const {
    return (m_buffer != NULL);
}

bool CBufferStream::eof() const {
    return (m_sizeLeft <= 0);
}

void CBufferStream::setSource(const uint8_t *buffer, size_t size) {
    m_buffer = buffer;
    m_size = size;

    m_currentPosition = m_buffer;
    m_sizeLeft = m_size;
}

void CBufferStream::setNullSource() {
    m_buffer = NULL;
    m_size = 0;

    m_currentPosition = NULL;
    m_sizeLeft = 0;
}

size_t CBufferStream::read(uint8_t *dest, size_t size) {
    size_t bufferSize = min(size, m_sizeLeft);

    memcpy(dest, m_currentPosition, bufferSize);

    m_currentPosition += bufferSize;
    m_sizeLeft -= bufferSize;

    return bufferSize;
}

int64_t CBufferStream::seek(int64_t offset) {
    if (offset > m_size)
        return -1;

    m_currentPosition = m_buffer + offset;
    m_sizeLeft = (size_t)(m_size - offset);

    return offset;
}