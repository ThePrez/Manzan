#ifndef _MANZAN_H_
#define _MANZAN_H_
#include <stdio.h>

#define BUFSTR(dest, src)                         \
  std::string dest(src, sizeof(src));             \
  {                                               \
    size_t lastChar = dest.find_last_not_of(" "); \
    if (lastChar != std::string::npos)            \
      dest.erase(1 + lastChar);                   \
  }
#define BUFSTRN(dest, src, len)                   \
  std::string dest(src, len);                     \
  {                                               \
    size_t lastChar = dest.find_last_not_of(" "); \
    if (lastChar != std::string::npos)            \
      dest.erase(1 + lastChar);                   \
  }
#define ITOA(dest, src) \
  char dest[32];        \
  sprintf(dest, "%d", src);

extern "C" FILE *debug_fd;
void STRDBG();
void ENDDBG();

#define DEBUG_ENABLED 1
#ifdef DEBUG_ENABLED
#define DEBUG(...)                  \
  if (NULL != debug_fd)             \
  {                                 \
    fprintf(debug_fd, __VA_ARGS__); \
    fflush(debug_fd);               \
  }
#else
#define STRDBG()
#define DEBUG(...)
#define ENDDBG()
#endif
#endif