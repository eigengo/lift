#include "dl.h"

static int _dl_count = 0;
static int _dl_last_error = 0;
static uint32_t _dl_tag = 0;

static DataLoggingSessionRef session = NULL;

void _dl_gfs_sample_callback(uint8_t* buffer, uint16_t size) {
    if (_dl_count >= 0) _dl_count++;

    DataLoggingResult result = data_logging_log(session, buffer, size);
    if (result != DATA_LOGGING_SUCCESS) {
        _dl_last_error = (int)result;
    }
}

gfs_sample_callback_t dl_start() {
    if (session != NULL) data_logging_finish(session);

    _dl_tag = (uint32_t)rand();

    session = data_logging_create(_dl_tag, DATA_LOGGING_BYTE_ARRAY, sizeof(uint8_t), false);
    if (session == NULL) {
        _dl_last_error = -1;
    } else {
        _dl_count = 0;
    }
    return &_dl_gfs_sample_callback;
}

void dl_stop() {
    if (session != NULL) data_logging_finish(session);

    session = NULL;
    _dl_count = -1;
}

int dl_count() {
    return _dl_count;
}

int dl_last_error() {
    return _dl_last_error;
}

uint32_t dl_tag() {
    return _dl_tag;
}
