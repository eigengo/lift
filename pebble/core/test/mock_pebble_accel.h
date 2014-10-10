#pragma once
#include <pebble.h>

#ifdef __cplusplus
extern "C" {
#endif

void mock_accel_send_data(AccelRawData *data, uint32_t num_samples, uint64_t timestamp);

#ifdef __cplusplus
}
#endif
