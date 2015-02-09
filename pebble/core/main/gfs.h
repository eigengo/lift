#pragma once
#include <pebble.h>

#define E_GFS_ALREADY_RUNNING -1
#define E_GFS_MEM -2

// buffer size in B
#define GFS_BUFFER_SIZE (uint16_t)635 // 630 = 126 samples per call

// power-of-two samples at a time
#define GFS_NUM_SAMPLES 2

#define GFS_HEADER_TYPE (uint16_t)0xad

/**
 * 5 B in header
 */
struct __attribute__((__packed__)) gfs_header {
    uint8_t  type;                   // 1
    uint8_t  count;                  // 2
    uint8_t  samples_per_second;     // 3
    uint8_t  sample_size;            // 4
    uint8_t  _padding;               // 5
};

/**
 * Packed 5 B of the accelerometer values
 */
struct __attribute__((__packed__)) gfs_packed_accel_data {
    int16_t x_val : 13;
    int16_t y_val : 13;
    int16_t z_val : 13;
};

typedef enum {
    GFS_SAMPLING_10HZ = 10,
    GFS_SAMPLING_25HZ = 25,
    GFS_SAMPLING_50HZ = 50,
    GFS_SAMPLING_100HZ = 100
} gfs_sampling_rate_t;

typedef void (*gfs_sample_callback_t) (uint8_t* buffer, uint16_t size);

#ifdef __cplusplus
extern "C" {
#endif

int gfs_start(gfs_sample_callback_t callback, gfs_sampling_rate_t frequency);
int gfs_stop();

#ifdef __cplusplus
}
#endif
