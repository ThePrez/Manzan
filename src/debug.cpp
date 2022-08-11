
#include "manzan.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
extern "C" FILE * debug_fd;
FILE *debug_fd = NULL;

void STRDBG()
{
#ifdef DEBUG_ENABLED
  debug_fd = fopen("/tmp/manzan_debug.txt", "a");
#endif
}
void ENDDBG()
{
#ifdef DEBUG_ENABLED
  if (NULL != debug_fd)
  {
    fclose(debug_fd);
    debug_fd = NULL;
  }
#endif
}