#include <pebble.h>

static AccelRawDataHandler current_handler;

void accel_raw_data_service_subscribe(uint32_t samples_per_update, AccelRawDataHandler handler) {
    current_handler = handler;
}

void accel_send_data(AccelRawData *data, uint32_t num_samples, uint64_t timestamp) {
    if (current_handler != NULL) current_handler(data, num_samples, timestamp);
}
