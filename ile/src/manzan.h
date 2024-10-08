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
    {                                             \
      dest.erase(1 + lastChar);                   \
    }                                             \
    else                                          \
    {                                             \
      dest.clear();                               \
    }                                             \
  }

#define ITOA(dest, src) \
  char dest[32];        \
  sprintf(dest, "%d", src);

#define MIN(a, b) ((a) < (b) ? (a) : (b))

#define PUBLISH_MESSAGE_FUNCTION_SIGNATURE const char *_session_id,           \
                                           const char *_msgid,                \
                                           const char *_msg_type,             \
                                           int _msg_severity,                 \
                                           const char *_msg_timestamp,        \
                                           const char *_job,                  \
                                           const char *_sending_usrprf,       \
                                           const char *_message,              \
                                           const char *_sending_program_name, \
                                           const char *_sending_module_name,  \
                                           const char *_sending_procedure_name

#define PUBLISH_VLOG_FUNCTION_SIGNATURE const char *_session_id,     \
                                        const char *_major_code,     \
                                        const char *_minor_code,     \
                                        const char *_log_id,         \
                                        const char *_timestamp,      \
                                        const char *_tde_number,     \
                                        const char *_task_name,      \
                                        const char *_server_type,    \
                                        const char *_exception_id,   \
                                        const char *_job,            \
                                        const char *_thread_id,      \
                                        const char *_module_offset,  \
                                        const char *_module_ru_name, \
                                        const char *_module_name,    \
                                        const char *_module_entry_point_name

#define PUBLISH_PAL_FUNCTION_SIGNATURE const char *_session_id,            \
                                       const char *_system_reference_code, \
                                       const char *_device_name,           \
                                       const char *_device_type,           \
                                       const char *_model,                 \
                                       const char *_serial_number,         \
                                       const char *_resource_name,         \
                                       const char *_log_identifier,        \
                                       const char *_pal_timestamp,         \
                                       const char *_reference_code,        \
                                       const char *_secondary_code,        \
                                       const char *_table_identifier,      \
                                       int _sequence

#define PUBLISH_OTHER_FUNCTION_SIGNATURE const char *_session_id, const char *_event_type

#ifdef __cplusplus
extern "C"
{
#endif
  extern void STRDBG();
  extern void ENDDBG();
  extern void DEBUG_INFO(const char *format, ...);
  extern void DEBUG_WARNING(const char *format, ...);
  extern void DEBUG_ERROR(const char *format, ...);


#define DEBUG_ENABLED 1

#ifdef __cplusplus
}
#endif
#endif