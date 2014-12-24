#pragma once
#include "gfs.h"

#define DL_TAG 0x0fb0

#ifdef __cplusplus
extern "C" {
#endif

gfs_sample_callback_t am_start();
void am_stop();
int am_count();
uint32_t am_tag();
int am_last_error();
int am_last_error_distance();
int am_error_count();

#ifdef __cplusplus
}
#endif
