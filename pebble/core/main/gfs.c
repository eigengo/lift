#include <pebble.h>
#include "gfs.h"

/**
 * Context that holds the current callback and frequency. It is used in the accelerometer
 * callback to calculate the G forces and to push the packed sample buffer to the callback.
 */
static struct {
    // the callback function
    accel_sample_callback callback; 
    // the frequency
    int frequency;
    // the buffer
    int8_t* buffer;
    // the position in the buffer
    int buffer_position;
} gfs_context;

/**
 * Write the header and reset the buffer position
 */
void gfs_write_header() {
    struct gfs_header h = { .h1 = GFS_HEADER_H1, .h2 = GFS_HEADER_H2, .size = GFS_BUFFER_SIZE };
    memcpy(gfs_context.buffer, &h, sizeof(struct gfs_header));
    gfs_context.buffer_position = sizeof(struct gfs_header);
}

/**
 * Handle the samples arriving.
 */
void gfs_raw_accel_data_handler(AccelRawData *data, uint32_t num_samples, uint64_t timestamp) {
    if (num_samples != GFS_NUM_SAMPLES) return /* FAIL */;

    if (gfs_context.buffer_position + sizeof(struct gfs_packed_accel_data) * num_samples >= GFS_BUFFER_SIZE) {
        gfs_context.callback(gfs_context.buffer, GFS_BUFFER_SIZE + sizeof(struct gfs_header));
        gfs_write_header();
    }

    // pack
    struct gfs_packed_accel_data *ad = (struct gfs_packed_accel_data *)(gfs_context.buffer + gfs_context.buffer_position);
    for (unsigned int i = 0; i < num_samples; ++i) {
        ad[i].x_val = data[i].x;
        ad[i].y_val = data[i].y;
        ad[i].z_val = data[i].z;
    }
    gfs_context.buffer_position += sizeof(struct gfs_packed_accel_data) * num_samples;
}

int gfs_start(accel_sample_callback callback, int frequency) {
    if (gfs_context.callback != NULL) return E_GFS_ALREADY_RUNNING;

    gfs_context.callback = callback;
    gfs_context.frequency = frequency;
    gfs_context.buffer = malloc(GFS_BUFFER_SIZE + sizeof(struct gfs_header));
    gfs_write_header();


    if (gfs_context.buffer == NULL) return E_GFS_MEM;

    accel_raw_data_service_subscribe(GFS_NUM_SAMPLES, gfs_raw_accel_data_handler);
 
    return 1;
}

int gfs_stop() {
    gfs_context.callback = NULL;
    if (gfs_context.buffer != NULL) free(gfs_context.buffer);
    return 1;
}
