
#include "manzan.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>

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


extern "C" {

// Helper function to handle the common logic
void DEBUG_LOG(const char *level, const char *format, va_list args) {
#ifdef DEBUG_ENABLED
    const char *env_level = getenv("MANZAN_DEBUG_LEVEL");
    if (env_level != NULL && strcmp(env_level, level) >= 0 && debug_fd != NULL) {
        vfprintf(debug_fd, format, args);
        fflush(debug_fd);
    }
#endif
}

// Wrapper functions for different debug levels

void DEBUG_INFO(const char *format, ...) {
#ifdef DEBUG_ENABLED
    va_list args;
    va_start(args, format);
    DEBUG_LOG("3", format, args);
    va_end(args);
#endif
}

void DEBUG_WARNING(const char *format, ...) {
#ifdef DEBUG_ENABLED
    va_list args;
    va_start(args, format);
    DEBUG_LOG("2", format, args);
    va_end(args);
#endif
}

void DEBUG_ERROR(const char *format, ...) {
#ifdef DEBUG_ENABLED
    va_list args;
    va_start(args, format);
    DEBUG_LOG("1", format, args);
    va_end(args);
#endif
}

}