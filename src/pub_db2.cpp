#include "manzan.h"
#include "pub_json.h"

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>

int db2_publish_message(const char *_session_id, const char *_msgid, const char *_msg_type, int _msg_severity, const char *_job, char *_message,
                        const char *_sending_program_name, const char *_sending_module_name, const char *_sending_procedure_name)
{
  return 0;
}

int db2_publish_other(const char *_session_id, const char *_event_type)
{
  return 0;
}