#ifndef CSTRING_FIFO_H
#define CSTRING_FIFO_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

typedef struct Node
{
    char *data;
    struct Node *next;
} Node;

typedef struct
{
    Node *front, *rear;
    pthread_mutex_t mutex;
} CStringFIFO;

static inline void cstr_fifo_init(CStringFIFO *fifo)
{
    fifo->front = fifo->rear = NULL;
    pthread_mutex_init(&fifo->mutex, NULL);
}

static inline void cstr_fifo_push(CStringFIFO *fifo, const char *str)
{
    Node *newNode = (Node *) malloc(sizeof(Node));
    if(!newNode) return;
    newNode->data = strdup(str);
    newNode->next = NULL;

    pthread_mutex_lock(&fifo->mutex);
    if(fifo->rear) fifo->rear->next = newNode;
    fifo->rear = newNode;
    if(!fifo->front) fifo->front = newNode;
    pthread_mutex_unlock(&fifo->mutex);
}

static inline char *cstr_fifo_pop(CStringFIFO *fifo)
{
    pthread_mutex_lock(&fifo->mutex);
    if(!fifo->front)
    {
        pthread_mutex_unlock(&fifo->mutex);
        return NULL;
    }

    Node *temp = fifo->front;
    char *data = temp->data;
    fifo->front = temp->next;
    if(!fifo->front) fifo->rear = NULL;
    pthread_mutex_unlock(&fifo->mutex);

    free(temp);
    return data;
}

static inline void cstr_fifo_destroy(CStringFIFO *fifo)
{
    pthread_mutex_lock(&fifo->mutex);
    while(fifo->front)
    {
        Node *temp = fifo->front;
        fifo->front = temp->next;
        free(temp->data);
        free(temp);
    }
    fifo->rear = NULL;
    pthread_mutex_unlock(&fifo->mutex);
    pthread_mutex_destroy(&fifo->mutex);
}

#endif // CSTRING_FIFO_H