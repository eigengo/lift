#include "dl.h"

static int _dl_count = 0;

static DataLoggingSessionRef session = NULL;

void gfs_sample_callback(uint8_t* buffer, uint16_t size) {
    if (_dl_count >= 0) _dl_count++;

    DataLoggingResult result = data_logging_log(session, buffer, size);
    if (result != DATA_LOGGING_SUCCESS) {
        _dl_count = -1000 - (int)result;
    }
}

gfs_sample_callback_t dl_start() {
    if (session != NULL) return &gfs_sample_callback;

    session = data_logging_create(DL_TAG, DATA_LOGGING_BYTE_ARRAY, sizeof(uint8_t), true);
    if (session == NULL) {
        _dl_count = -1001;
    } else {
        _dl_count = 0;
    }
    return &gfs_sample_callback;
}

void dl_stop() {
    if (session != NULL) data_logging_finish(session);
    _dl_count = -1000;
}

int dl_count() {
    return _dl_count;
}
