#include "am.h"

static int _am_count = 0;
static int _am_last_error = 0;
static int _am_last_error_distance = 0;
static int _am_error_count = 0;
static uint32_t _am_tag = 0;

void _am_outbox_failed(DictionaryIterator* iterator, AppMessageResult reason, void* context) {
    _am_error_count++;
    _am_last_error_distance = 0;
    _am_last_error = -1200 - reason;
}

void _am_gfs_sample_callback(uint8_t* buffer, uint16_t size) {
    _am_count++;
    _am_last_error_distance++;

    DictionaryIterator *iter;
    AppMessageResult app_message_result;
    if ((app_message_result = app_message_outbox_begin(&iter)) != APP_MSG_OK) {
        _am_error_count++;
        _am_last_error_distance = 0;
        _am_last_error = -1000 - app_message_result;
        return;
    }
    DictionaryResult dictionary_result;
    if ((dictionary_result = dict_write_data(iter, 0xface0fb0, buffer, size)) != DICT_OK) {
        _am_error_count++;
        _am_last_error_distance = 0;
        _am_last_error = -2000 - dictionary_result;
        return;
    }
    dict_write_end(iter);
    if ((app_message_result = app_message_outbox_send()) != APP_MSG_OK) {
        _am_error_count++;
        _am_last_error_distance = 0;
        _am_last_error = -1100 - app_message_result;
        return;
    }
}

gfs_sample_callback_t am_start() {
    app_message_open(APP_MESSAGE_INBOX_SIZE_MINIMUM, APP_MESSAGE_OUTBOX_SIZE_MINIMUM);
    app_message_register_outbox_failed(_am_outbox_failed);

    return &_am_gfs_sample_callback;
}

void am_stop() {
    _am_last_error = 0;
    _am_last_error_distance = -1;
    _am_error_count = 0;
    _am_count = 0;
}

int am_count() {
    return _am_count;
}

int am_last_error() {
    return _am_last_error;
}

int am_last_error_distance() {
    return _am_last_error_distance;
}

int am_error_count() {
    return _am_error_count;
}

uint32_t am_tag() {
    return _am_tag;
}
