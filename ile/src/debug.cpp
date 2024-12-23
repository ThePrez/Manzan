
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
    if (!debug_fd) {
        perror("Failed to open debug file");
    }

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



// Helper function to handle the common logic
extern "C" void DEBUG_LOG(const char *level, const char *label, const char *format, va_list args) {
#ifdef DEBUG_ENABLED
    char *env_level = getenv("MANZAN_DEBUG_LEVEL");
    if (env_level == NULL){
      env_level = "2"; // Show errors and warnings only
    }
    env_level = "3";
    if (strcmp(env_level, level) >= 0 && debug_fd != NULL) {
        fprintf(debug_fd, "%s ", label); // Prepend the log level
        vfprintf(debug_fd, format, args);
        fflush(debug_fd);
    }
#endif
}

// Wrapper functions for different debug levels

extern "C" void DEBUG_INFO(const char *format, ...) {
#ifdef DEBUG_ENABLED
    va_list args;
    va_start(args, format);
    DEBUG_LOG("3", "[INFO]", format, args);
    va_end(args);
#endif
}

extern "C" void DEBUG_WARNING(const char *format, ...) {
#ifdef DEBUG_ENABLED
    va_list args;
    va_start(args, format);
    DEBUG_LOG("2", "[WARNING]", format, args);
    va_end(args);
#endif
}

extern "C" void DEBUG_ERROR(const char *format, ...) {
#ifdef DEBUG_ENABLED
    va_list args;
    va_start(args, format);
    DEBUG_LOG("1", "ERROR", format, args);
    va_end(args);
#endif
}
