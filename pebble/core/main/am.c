#include "am.h"

static int _am_count = 0;
static int _am_last_error = 0;
static char* _am_last_error_text = "";
static int _am_last_error_distance = 0;
static int _am_error_count = 0;
static uint32_t _am_tag = 0;

static Queue* _am_message_queue;

char* _am_get_error_message(int code) {
    switch (code) {
        case APP_MSG_OK: return "";
        case APP_MSG_SEND_TIMEOUT: return "TO";
        case APP_MSG_SEND_REJECTED: return "REJ";
        case APP_MSG_NOT_CONNECTED: return "NC";
        case APP_MSG_APP_NOT_RUNNING: return "NR";
        case APP_MSG_INVALID_ARGS: return "INV";
        case APP_MSG_BUSY: return "BUSY";
        case APP_MSG_BUFFER_OVERFLOW: return "OVER";
        case APP_MSG_ALREADY_RELEASED: return "ARED";
        case APP_MSG_CALLBACK_ALREADY_REGISTERED: return "AREG";
        case APP_MSG_CALLBACK_NOT_REGISTERED: return "CNR";
        case APP_MSG_OUT_OF_MEMORY: return "MEMO";
        case APP_MSG_CLOSED: return "CL";
        case APP_MSG_INTERNAL_ERROR: return "INTE";
        default: return "UNK";
    }
}

void _pop_message() {
    uint8_t* buffer;
    uint16_t size;
    
    queue_pop(_am_message_queue, &buffer, &size);
    if (buffer != NULL)
        free(buffer);
}

void _send_next_message() {
    uint8_t* buffer;
    uint16_t size;
    queue_peek(_am_message_queue, &buffer, &size);
    if (buffer == NULL)
        return;

    DictionaryIterator* message;
    AppMessageResult app_message_result;
    if ((app_message_result = app_message_outbox_begin(&message)) != APP_MSG_OK) {
        
        if (app_message_result == APP_MSG_BUSY) {
            return;
        }
        
        _am_error_count++;
        _am_last_error_distance = 0;
        _am_last_error = -1000 - app_message_result;
        _am_last_error_text = _am_get_error_message(app_message_result);
        return;
    }

    DictionaryResult dictionary_result;
    if ((dictionary_result = dict_write_data(message, 0xface0fb0, buffer, size)) != DICT_OK) {
        _am_error_count++;
        _am_last_error_distance = 0;
        _am_last_error = -2000 - dictionary_result;
        _am_last_error_text = _am_get_error_message(dictionary_result);
        return;
    }
    dict_write_end(message);
    if (message == NULL)
        return;
    
    if ((app_message_result = app_message_outbox_send()) != APP_MSG_OK) {
        _am_error_count++;
        _am_last_error_distance = 0;
        _am_last_error = -1100 - app_message_result;
        _am_last_error_text = _am_get_error_message(app_message_result);
        return;
    }
}

void _am_outbox_sent(DictionaryIterator *iterator, void *context) {
    _pop_message();
    _am_count++;
    _am_last_error_distance++;
    _send_next_message();
}

void _am_outbox_failed(DictionaryIterator* iterator, AppMessageResult reason, void* context) {
    _pop_message();
    _am_error_count++;
    _am_last_error_distance = 0;
    _am_last_error = -1200 - reason;
    _am_last_error_text = _am_get_error_message(reason);
    
    if (_am_message_queue->length > 10) { // Avoid out of memory situations.
        while (_am_message_queue->length > 0) _pop_message();
    }
    
    _send_next_message();
}

void _am_gfs_sample_callback(uint8_t* buffer, uint16_t size) {
    queue_add(_am_message_queue, buffer, size);
    _send_next_message();
}

gfs_sample_callback_t am_start() {
    _am_message_queue = queue_create();
    
    app_message_open(APP_MESSAGE_INBOX_SIZE_MINIMUM, APP_MESSAGE_OUTBOX_SIZE_MINIMUM);
    app_message_register_outbox_sent(_am_outbox_sent);
    app_message_register_outbox_failed(_am_outbox_failed);

    return &_am_gfs_sample_callback;
}

void am_stop() {
    for (int i = 0; i < 10; ++i) {
        DictionaryIterator *iter;
        if (app_message_outbox_begin(&iter) == APP_MSG_OK) {
            dict_write_int8(iter, 0x0000dead, 1);
            dict_write_end(iter);
            if (app_message_outbox_send() == APP_MSG_OK) break;
        }
    }
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

char* am_last_error_text() {
    return _am_last_error_text;
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

int am_queue_length() {
    if (_am_message_queue == NULL)
        return 0;
    else
        return _am_message_queue->length;
}

