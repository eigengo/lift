#ifndef Lift_Protocol_h
#define Lift_Protocol_h

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

/**
 * The data logger header
 */
struct __attribute__((__packed__)) dl_header {
    int8_t type;
    uint16_t count;
};


#endif
