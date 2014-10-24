#include "dl.h"

static int _dl_count = 0;
static int _dl_last_error = 0;
static int32_t _dl_tag = 0;

static DataLoggingSessionRef session = NULL;

void gfs_sample_callback(uint8_t* buffer, uint16_t size) {
    if (_dl_count >= 0) _dl_count++;

    DataLoggingResult result = data_logging_log(session, buffer, size);
    if (result != DATA_LOGGING_SUCCESS) {
        _dl_last_error = (int)result;
    }
}

gfs_sample_callback_t dl_start() {
    if (session != NULL) data_logging_finish(session);

    _dl_tag = rand();

    session = data_logging_create(_dl_tag, DATA_LOGGING_BYTE_ARRAY, sizeof(uint8_t), false);
    if (session == NULL) {
        _dl_last_error = -1;
    } else {
        _dl_count = 0;
    }
    return &gfs_sample_callback;
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

int32_t dl_tag() {
    return _dl_tag;
}
