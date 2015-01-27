#ifndef Lift_ThisDeviceProtocol_h
#define Lift_ThisDeviceProtocol_h

/**
 * 5 B in header
 */
typedef struct __attribute__((__packed__)) {
    uint8_t type;                   // 1
    uint8_t count;                  // 2
    uint8_t samples_per_second;     // 3
    uint16_t last;                  // 4, 5
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
 * Thanks to human physiology, it's sufficient to keep the hear rate as
 * 8bit unsigned integer. Anything < 30 and > 200 usually means the user 
 * is not worried about exercise.
 */
typedef struct __attribute__((__packed__)) {
    uint8_t hr;
} lift_hr_data;

#endif
