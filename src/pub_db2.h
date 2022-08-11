#ifndef _MANZAN_PUB_DB2_H_
#define _MANZAN_PUB_DB2_H_

int db2_publish_message(const char *_session_id, const char *_msgid, const char *_msg_type, int _msg_severity, const char *_job, char *_message,
                         const char *_sending_program_name, const char *_sending_module_name, const char *_sending_procedure_name);

int db2_publish_other(const char *_session_id, const char *_event_type);

#endif