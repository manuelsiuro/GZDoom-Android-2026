
#include <stdio.h>

#include "LogWritter.h"


static FILE *file;

void LogWritter_Init(const char *filename)
{
    file = fopen(filename, "w");
}

void LogWritter_Write(const char *msg)
{
    fprintf(file, "%s\n", msg);
    fflush(file);
}

void LogWritter_Close()
{
    fclose(file);
}