#include "queue.h"

#if 1 == 2
#define APP_LOG_QUEUE APP_LOG_DEBUG
#else
#define APP_LOG_QUEUE(...)
#endif

Queue* queue_create() {
    APP_LOG_QUEUE("queue_create - begin");
    
    Queue* queue = (Queue*)malloc(sizeof(Queue));
    queue->length = 0;
    queue->first = (struct QueueNode*)malloc(sizeof(QueueNodeStruct));
    queue->first->buffer = NULL;
    queue->first->size = 0;
    queue->first->next = NULL;
    
    APP_LOG_QUEUE("queue_create - end");
    
    return queue;
}

void queue_destroy(Queue** queue) {
    APP_LOG_QUEUE("queue_destroy - begin");
    
    Queue* pQueue = (*queue);
    if (pQueue == NULL) {
        APP_LOG_QUEUE("queue_destroy - end");
        return;
    }
    
    (*queue) = NULL;
    while (pQueue->length > 0) {
        uint8_t* buffer;
        uint16_t size;
        
        queue_pop(pQueue, &buffer, &size);
        if (buffer != NULL)
            free(buffer);
    };
    
    free(pQueue);
    
    APP_LOG_QUEUE("queue_destroy - end");
}

void queue_add(Queue* queue, uint8_t* buffer, uint16_t size) {
    APP_LOG_QUEUE("queue_add - begin");
    
    APP_LOG_QUEUE("queue_add - size: %d", size);
    
    if (queue->length > 0) {
        struct QueueNode* last = (struct QueueNode*)malloc(sizeof(QueueNodeStruct));
        void* ptr = malloc(size);
        memcpy(ptr, buffer, size);
        last->buffer = (uint8_t*)ptr;
        last->size = size;
        last->next = NULL;
        
        struct QueueNode* current = queue->first;
        while (current->next != NULL) current = current->next;
        current->next = last;
        queue->length = queue->length + 1;
    } else {
        struct QueueNode* last = queue->first;
        void* ptr = malloc(size);
        memcpy(ptr, buffer, size);
        last->buffer = (uint8_t*)ptr;
        last->size = size;
        last->next = NULL;
        queue->length = 1;
    }
    
    APP_LOG_QUEUE("queue_add - end");
}

void queue_peek(Queue* queue, uint8_t** buffer, uint16_t* size) {
    APP_LOG_QUEUE("queue_peek - begin");
    
    (*buffer) = queue->first->buffer;
    (*size) = queue->first->size;
    
    APP_LOG_QUEUE("queue_peek - end");
}

void queue_pop(Queue* queue, uint8_t** buffer, uint16_t* size) {
    APP_LOG_QUEUE("queue_pop - begin");
    
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
    
    APP_LOG_QUEUE("queue_pop - end");
}

int queue_length(Queue* queue) {
    if (queue == NULL)
        return 0;
    
    return queue->length;
}
