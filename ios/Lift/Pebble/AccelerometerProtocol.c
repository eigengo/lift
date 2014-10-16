#include "AccelerometerProtocol.h"

void gfs_unpack_accel_data(const uint8_t *packed_memory, const uint16_t count, struct gfs_accel_data *unpacked) {
    struct gfs_packed_accel_data *packed = (struct gfs_packed_accel_data*)packed_memory;
    
    for (size_t i = 0; i < count; i++) {
        unpacked[i].x_val = packed[i].x_val;
        unpacked[i].y_val = packed[i].y_val;
        unpacked[i].z_val = packed[i].z_val;
    }
}
