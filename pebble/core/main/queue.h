#pragma once

#include "pebble.h"
#include "debug_macros.h"

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
void queue_destroy(Queue** queue);
void queue_add(Queue* queue, uint8_t* buffer, uint16_t size);
void queue_peek(Queue* queue, uint8_t** buffer, uint16_t* size);
void queue_pop(Queue* queue, uint8_t** buffer, uint16_t* size);
int queue_length(Queue* queue);

#ifdef __cplusplus
}
#endif