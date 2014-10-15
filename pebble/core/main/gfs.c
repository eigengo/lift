#include <pebble.h>
#include "gfs.h"

/**
 * Context that holds the current callback and samples_per_second. It is used in the accelerometer
 * callback to calculate the G forces and to push the packed sample buffer to the callback.
 */
static struct {
    // the callback function
    gfs_sample_callback_t callback;
    // the samples_per_second
    uint16_t samples_per_second;
    // the buffer
    uint8_t* buffer;
    // the position in the buffer
    uint16_t buffer_position;
} gfs_context;

/**
 * Write the header and reset the buffer position
 */
void gfs_write_header() {
    struct gfs_header *h = (struct gfs_header *) gfs_context.buffer;
    h->h1 = GFS_HEADER_H1;
    h->h2 = GFS_HEADER_H2;
    h->samples_per_second = gfs_context.samples_per_second;
    gfs_context.buffer_position = sizeof(struct gfs_header);
}

/**
 * Handle the samples arriving.
 */
void gfs_raw_accel_data_handler(AccelRawData *data, uint32_t num_samples, uint64_t timestamp) {
    if (num_samples != GFS_NUM_SAMPLES) return /* FAIL */;

    size_t len = sizeof(struct gfs_packed_accel_data) * num_samples;
    if (gfs_context.buffer_position + len >= GFS_BUFFER_SIZE) {
        uint16_t count = (uint16_t)((gfs_context.buffer_position - sizeof(struct gfs_header)) / sizeof(struct gfs_packed_accel_data));
        gfs_context.callback(gfs_context.buffer, gfs_context.buffer_position, count);
        gfs_write_header();
    }

    // pack
    struct gfs_packed_accel_data *ad = (struct gfs_packed_accel_data *)(gfs_context.buffer + gfs_context.buffer_position);
    for (unsigned int i = 0; i < num_samples; ++i) {
        ad[i].x_val = data[i].x;
        ad[i].y_val = data[i].y;
        ad[i].z_val = data[i].z;
    }
    gfs_context.buffer_position += len;
}

int gfs_start(gfs_sample_callback_t callback, int frequency) {
    if (gfs_context.callback != NULL) return E_GFS_ALREADY_RUNNING;

    gfs_context.callback = callback;
    gfs_context.samples_per_second = (uint16_t) frequency;
    gfs_context.buffer = malloc(GFS_BUFFER_SIZE + sizeof(struct gfs_header));

    if (gfs_context.buffer == NULL) return E_GFS_MEM;

    gfs_write_header();
    accel_service_set_sampling_rate(frequency);
    accel_raw_data_service_subscribe(GFS_NUM_SAMPLES, gfs_raw_accel_data_handler);
 
    return 1;
}

int gfs_stop() {
    gfs_context.callback = NULL;
    if (gfs_context.buffer != NULL) free(gfs_context.buffer);
    return 1;
}
