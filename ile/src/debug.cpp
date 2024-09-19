
#include "manzan.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

FILE *debug_fd = NULL;

extern "C" void STRDBG()
{
#ifdef DEBUG_ENABLED
  debug_fd = fopen("/tmp/manzan_debug.txt", "a");
#endif
}

extern "C" void ENDDBG()
{
#ifdef DEBUG_ENABLED
  if (NULL != debug_fd)
  {
    fclose(debug_fd);
    debug_fd = NULL;
  }
#endif
}

extern "C" void DEBUG(const char *format, ...)
{
#ifdef DEBUG_ENABLED
  if (NULL != debug_fd)
  {
    va_list args;
    va_start(args, format);
    vfprintf(debug_fd, format, args);
    va_end(args);
    fflush(debug_fd);
  }
#endif
}