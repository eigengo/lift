#include <stdint.h>

/**
 * 5 B in header
 */
typedef struct __attribute__((__packed__)) {
    uint8_t type;                   // 1
    uint8_t count;                  // 2
    uint8_t samples_per_second;     // 3
    uint8_t sample_size;            // 4
    uint8_t padding;                // 5
} lift_header;

#define LIFT_ACCELEROMETER_TYPE (uint8_t)0xad
#define LIFT_GYROSCOPE_TYPE (uint8_t)0xbd
#define LIFT_GPS_TYPE (uint8_t)0xcd
#define LIFT_HR_TYPE (uint8_t)0xdd

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
