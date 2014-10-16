#pragma once
#include <pebble.h>

#define E_GFS_ALREADY_RUNNING -1
#define E_GFS_MEM -2

/*
 * The messages are 304 bytes each, each containing 50 samples.
 *
 * At 100 Hz sampling rate, we expect to transmit 100 / 58 * 295 bps = 506 B/s
 *
 */
// buffer size in B (314)
#define GFS_BUFFER_SIZE (uint16_t)295 // 290 = 58 samples per call

// power-of-two samples at a time
#define GFS_NUM_SAMPLES 2

#define GFS_HEADER_TYPE (uint16_t)0xfef7

/**
 */
struct __attribute__((__packed__)) gfs_header {
    uint16_t type;
    uint16_t count;
    uint8_t samples_per_second;
};

/**
 * The accelerometer values
 */
struct __attribute__((__packed__)) gfs_packed_accel_data {
    int16_t x_val : 11;
    int16_t y_val : 11;
    int16_t z_val : 11;
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
