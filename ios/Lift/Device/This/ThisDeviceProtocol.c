#include <stdint.h>
#include "ThisDeviceProtocol.h"

#define SIGNED_12_MAX(x) (uint16_t)((x) > 4095 ? 4095 : ((x) < -4095 ? -4095 : (x)))

void encode_lift_accelerometer_data(int x, int y, int z, void* data) {
    lift_accelerometer_data *add = (lift_accelerometer_data*)data;
    add->x_val = SIGNED_12_MAX(x);
    add->y_val = SIGNED_12_MAX(y);
    add->z_val = SIGNED_12_MAX(z);
}
