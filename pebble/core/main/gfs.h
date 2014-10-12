#ifndef GFS_H
#define GFS_H

#define E_GFS_ALREADY_RUNNING -1
#define E_GFS_MEM -2

// buffer size in B
#define GFS_BUFFER_SIZE 12000

// power-of-two samples at a time
#define GFS_NUM_SAMPLES 16

#define GFS_HEADER_H1 (uint8_t)0xf1
#define GFS_HEADER_H2 (uint8_t)0x1f

/**
* The first four bytes of the protocol are:
*
* 0x1e, 0x01, n, n'; followed by n bytes
*
* where n is little-endian unsigned 16-bit integer.
*/
struct __attribute__((__packed__)) gfs_header {
    int8_t h1;
    int8_t h2;
    uint8_t size;
};

/**
 * The accelerometer values
 */
struct __attribute__((__packed__)) gfs_packed_accel_data {
    int16_t x_val : 10;
    int16_t y_val : 10;
    int16_t z_val : 10;
};

typedef void (*accel_sample_callback) (uint8_t* samples, int samples_size);

#ifdef __cplusplus
extern "C" {
#endif

int gfs_start(accel_sample_callback callback, int frequency);
int gfs_stop();

#ifdef __cplusplus
}
#endif

#endif
