#pragma once

#include "pebble.h"

#ifdef __cplusplus
extern "C" {
#endif

struct QueueNode {
    uint8_t* buffer;
    uint16_t size;
    struct QueueNode* next;
} QueueNodeStruct;

typedef struct {
    int length;
    struct QueueNode* first;
} Queue;
    
Queue* queue_create();
void queue_add(Queue* queue, uint8_t* buffer, uint16_t size);
void queue_peek(Queue* queue, uint8_t** buffer, uint16_t* size);
void queue_pop(Queue* queue, uint8_t** buffer, uint16_t* size);

#ifdef __cplusplus
}
#endif