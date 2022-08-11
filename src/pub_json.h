#ifndef _MANZAN_JSON_PUB_H_
#define _MANZAN_JSON_PUB_H_

int json_publish_message(const char *_session_id, const char *_msgid, const char *_msg_type, int _msg_severity, const char *_job, char *_message,
                         const char *_sending_program_name, const char *_sending_module_name, const char *_sending_procedure_name);

int json_publish_other(const char *_session_id, const char *_event_type);

#endif