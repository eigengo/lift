#ifndef GFS_H
#define GFS_H

#define E_GFS_ALREADY_RUNNING -1
#define E_GFS_MEM -2

typedef void (*accel_sample_callback) (char* samples, int samples_size);

int gfs_start(accel_sample_callback callback, int frequency);
int gfs_stop();

#endif