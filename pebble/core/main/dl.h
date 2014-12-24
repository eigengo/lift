#pragma once
#include "gfs.h"

#define DL_TAG 0x0fb0

#ifdef __cplusplus
extern "C" {
#endif

gfs_sample_callback_t dl_start();
void dl_stop();
int dl_count();
uint32_t dl_tag();
int dl_last_error();

#ifdef __cplusplus
}
#endif
