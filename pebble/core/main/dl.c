#include "dl.h"

static int _dl_count = 0;

static DataLoggingSessionRef session = NULL;

void gfs_sample_callback(uint8_t* buffer, uint16_t size, uint16_t count) {
    _dl_count++;
    struct dl_header h = {.count = count, .type = DL_SAMPLE_PACKET };

    data_logging_log(session, &h, sizeof(struct dl_header));
    data_logging_log(session, buffer, size);
}

gfs_sample_callback_t dl_start() {
    if (session != NULL) return &gfs_sample_callback;

    session = data_logging_create(DL_TAG, DATA_LOGGING_BYTE_ARRAY, sizeof(uint8_t), false);
    return &gfs_sample_callback;
}

void dl_stop() {
    if (session != NULL) data_logging_finish(session);
}

int dl_count() {
    return _dl_count;
}
