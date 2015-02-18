#pragma once
#include "gfs.h"
#include "queue.h"
#include "debug_macros.h"

#define DL_TAG 0x0fb0

#ifdef __cplusplus
extern "C" {
#endif

gfs_sample_callback_t am_start();
void am_stop();
int am_count();
uint32_t am_tag();
int am_last_error();
char* am_last_error_text();
int am_last_error_distance();
int am_error_count();
int am_queue_length();

#ifdef __cplusplus
}
#endif
