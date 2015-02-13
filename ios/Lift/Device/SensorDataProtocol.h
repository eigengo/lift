#ifndef __Lift__SensorDataProtocol__
#define __Lift__SensorDataProtocol__

#include <stdint.h>

/**
 * Packed 5 B of the accelerometer values
 */
typedef struct __attribute__((__packed__)) {
    int16_t x_val : 13;
    int16_t y_val : 13;
    int16_t z_val : 13;
} lift_accelerometer_data;

/**
 * Encodes the values in x, y, z into ``lift_accelerometer_data`` pointed to in
 * ``data``. The memory in ``data`` must be at least 5 B long.
 */
void encode_lift_accelerometer_data(int16_t x, int16_t y, int16_t z, void* data);

/**
 * Decodes the values in x, y, z from the ``buffer``.
 * The memory in ``buffer`` must be at least 5 B long.
 */
void decode_lift_accelerometer_data(const void* buffer, int16_t* x, int16_t* y, int16_t* z);

#endif