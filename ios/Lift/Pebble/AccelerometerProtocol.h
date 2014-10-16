#ifndef Lift_Protocol_h
#define Lift_Protocol_h
#include <stdio.h>

const uint8_t GFS_HEADER_TYPE = (uint8_t)0x40;

/**
 * The accelerometer values
 */
struct __attribute__((__packed__)) gfs_accel_data {
    int16_t x_val;
    int16_t y_val;
    int16_t z_val;
};

/**
 * The accelerometer values
 */
struct __attribute__((__packed__)) gfs_packed_accel_data {
    int16_t x_val : 11;
    int16_t y_val : 11;
    int16_t z_val : 11;
};

/**
 */
struct __attribute__((__packed__)) gfs_header {
    uint8_t type;
    uint16_t count;
    uint8_t samples_per_second;
};

void gfs_unpack_accel_data(const uint8_t *packed_memory, const uint16_t count, struct gfs_accel_data *unpacked);

#endif
