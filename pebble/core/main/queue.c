#include "queue.h"

Queue* queue_create() {
    Queue* queue = (Queue*)malloc(sizeof(Queue));
    queue->length = 0;
    queue->first = (struct QueueNode*)malloc(sizeof(QueueNodeStruct));
    queue->first->buffer = NULL;
    queue->first->size = 0;
    queue->first->next = NULL;
    return queue;
}

void queue_add(Queue* queue, uint8_t* buffer, uint16_t size) {
    struct QueueNode* last = (struct QueueNode*)malloc(sizeof(QueueNodeStruct));
    last->buffer = (uint8_t*)malloc(size);
    memcpy(last->buffer, buffer, size);
    last->size = size;
    last->next = NULL;
    
    if (queue->length > 0) {
        struct QueueNode* current = queue->first;
        while (current->next != NULL) current = current->next;
        current->next = last;
        queue->length = queue->length + 1;
    } else {
        struct QueueNode* empty = queue->first;
        queue->first = last;
        queue->length = 1;
        free(empty);
    }
}

void queue_peek(Queue* queue, uint8_t** buffer, uint16_t* size) {
    (*buffer) = queue->first->buffer;
    (*size) = queue->first->size;
}

void queue_pop(Queue* queue, uint8_t** buffer, uint16_t* size) {
    struct QueueNode* node = queue->first;
    (*buffer) = node->buffer;
    (*size) = node->size;
    
    if (node->next == NULL) {
        node->buffer = NULL;
        node->size = 0;
        queue->length = 0;
    } else {
        queue->length = queue->length - 1;
        queue->first = node->next;
        free(node);
    }
}