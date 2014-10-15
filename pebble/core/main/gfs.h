#pragma once

#define E_GFS_ALREADY_RUNNING -1
#define E_GFS_MEM -2

// buffer size in B
#define GFS_BUFFER_SIZE (uint16_t)12000

// power-of-two samples at a time
#define GFS_NUM_SAMPLES 16

#define GFS_HEADER_H1 (uint8_t)0x40
#define GFS_HEADER_H2 (uint8_t)0x41

/**
* The first four bytes of the protocol are:
*
* 0x1e, 0x01, n, n'; followed by n bytes
*
* where n is little-endian unsigned 16-bit integer.
*/
struct __attribute__((__packed__)) gfs_header {
    uint8_t h1;
    uint8_t h2;
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

typedef void (*gfs_sample_callback_t) (uint8_t* buffer, uint16_t size, uint16_t count);

#ifdef __cplusplus
extern "C" {
#endif

int gfs_start(gfs_sample_callback_t callback, AccelSamplingRate frequency);
int gfs_stop();

#ifdef __cplusplus
}
#endif
