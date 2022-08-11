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

#define PUBLISH_MESSAGE_FUNCTION_SIGNATURE const char *_session_id,           \
                                   const char *_msgid,                \
                                   const char *_msg_type,             \
                                   int _msg_severity,                 \
                                   const char *_job,                  \
                                   const char *_sending_usrprf,       \
                                   const char *_message,              \
                                   const char *_sending_program_name, \
                                   const char *_sending_module_name,  \
                                   const char *_sending_procedure_name

#define PUBLISH_OTHER_FUNCTION_SIGNATURE const char *_session_id, const char *_event_type

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