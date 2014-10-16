#pragma once
#include <pebble.h>
#include "gfs.h"

#define DL_TAG 0x0fb0
#define DL_HEADER_TYPE (int8_t)0x41

struct __attribute__((__packed__)) dl_header {
    int8_t type;
    uint16_t count;
};


#ifdef __cplusplus
extern "C" {
#endif

gfs_sample_callback_t dl_start();
void dl_stop();
int dl_count();

#ifdef __cplusplus
}
#endif
