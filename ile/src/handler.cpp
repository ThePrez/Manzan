#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qmhrtvm.h>
#include <except.h>
#include <QWCCVTDT.h>
#include "manzan.h"
#include "event_data.h"
#include "userconf.h"

static FILE *fd = NULL;

#pragma pack(1)

typedef struct
{
  // 0 	0 	BINARY(4) 	Bytes returned
  int bytes_returned;
  // 4 	4 	BINARY(4) 	Bytes available
  int bytes_available;
  // 8 	8 	BINARY(4) 	Length of message returned
  int length_message_returned;
  // 12 	C 	BINARY(4) 	Length of message available
  int length_message_available;
  // 16 	10 	BINARY(4) 	Length of message help returned
  int length_message_help_returned;
  // 20 	14 	BINARY(4) 	Length of message help available
  int length_message_help_available;
  // 24 	18 	CHAR(*) 	Message
  char message[1024];
} RTVM0100;

#pragma pack(pop)

std::string get_iso8601_timestamp(const char *_in)
{
  char output[32];
  memset(output, 0, sizeof(output));
  char err[32];
  memset(err, 0, sizeof(err));
  QWCCVTDT(
      // 1 	Input format 	Input 	Char(10)
      "*DTS      ",
      // 2 	Input variable 	Input 	Char(*)
      (void *)_in,
      // 3 	Output format 	Input 	Char(10)
      "*YYMD     ",
      // 4 	Output variable 	Output 	Char(*)
      output,
      // 5 	Error code 	I/O 	Char(*)
      err);
  // ISO8601
  std::string formatted;
  formatted += std::string(output, 4);
  formatted += "-";
  formatted += std::string(output + 4, 2);
  formatted += "-";
  formatted += std::string(output + 6, 2);
  formatted += " ";
  formatted += std::string(output + 8, 2);
  formatted += ":";
  formatted += std::string(output + 10, 2);
  formatted += ":";
  formatted += std::string(output + 12, 2);
  formatted += ".";
  formatted += std::string(output + 14, 6);
  return formatted;
}

int main(int _argc, char **argv)
{
  static volatile _INTRPT_Hndlr_Parms_T my_commarea;
// https://www.ibm.com/docs/en/i/7.1?topic=descriptions-exception-handler
#pragma exception_handler(oh_crap, my_commarea, _C1_ALL, _C2_ALL, _CTLA_HANDLE, 0)
  if (!conf_is_enabled())
  {
    return 0;
  }
  STRDBG();

  BUFSTRN(watch_option, argv[1], 10);
  BUFSTRN(session_id, argv[2], 10);
  DEBUG("watch program called. Watch option setting is '%s'\n", watch_option.c_str());
  publisher_info_set *publishers = conf_get_publisher_info(session_id.c_str());
  int num_publishers = publishers->num_publishers;
  if (0 == num_publishers)
  {
    DEBUG("No publishers found for watch option '%s' and session ID '%s'\n", watch_option.c_str(), session_id.c_str());
    ENDDBG();
    return 0;
  }
  if (watch_option == "*MSGID")
  {
    DEBUG("Handling message\n");
    msg_event_raw *msg_event = (msg_event_raw *)argv[4];
    BUFSTR(msgid, msg_event->message_watched);
    BUFSTR(job_name, msg_event->job_name);
    BUFSTR(user_name, msg_event->user_name);
    BUFSTR(job_number, msg_event->job_number);
    std::string job = job_number + "/" + user_name + "/" + job_name;
    BUFSTR(message_type, msg_event->message_type);
    std::string message_timestamp = get_iso8601_timestamp(msg_event->message_timestamp);
    DEBUG("Timestamp is '%s'\n", message_timestamp.c_str());
    int message_severity = msg_event->message_severity;
    BUFSTR(sending_usrprf, msg_event->sending_user_profile);
    BUFSTRN(sending_procedure_name, (char *)msg_event + msg_event->offset_send_procedure_name, msg_event->length_send_procedure_name);
    BUFSTR(sending_module_name, msg_event->sending_module_name);
    BUFSTR(sending_program_name, msg_event->sending_program_name);
    sending_program_name.erase(1 + sending_program_name.find_last_not_of(" "));

    int replacement_data_offset = msg_event->offset_replacement_data;
    int replacement_data_len = msg_event->length_replacement_data;
    DEBUG("Replacement data offset is '%d'\n", replacement_data_offset);
    DEBUG("REPLACEMENT DATA LENGTH IS '%d'\n", replacement_data_len);
    char *replacement_data = (0 == replacement_data_len) ? "" : (((char *)msg_event) + replacement_data_offset);
    char *replacement_data_aligned = (char *)malloc(replacement_data_len);
    memcpy(replacement_data_aligned, replacement_data, replacement_data_len);

    RTVM0100 msg_info_buf;
    memset(&msg_info_buf, 0x00, sizeof(msg_info_buf));
    char err_plc[64];
    memset(err_plc, 0x00, sizeof(err_plc));
    QMHRTVM(
        // 1 	Message information 	Output 	Char(*)
        &msg_info_buf,
        // 2 	Length of message information 	Input 	Binary(4)
        -1 + sizeof(msg_info_buf),
        // 3 	Format name 	Input 	Char(8)
        "RTVM0100",
        // 4 	Message identifier 	Input 	Char(7)
        msg_event->message_watched,
        // 5 	Qualified message file name 	Input 	Char(20)
        "QCPFMSG   QSYS      ",
        // 6 	Replacement data 	Input 	Char(*)
        replacement_data_aligned,
        // 7 	Length of replacement data 	Input 	Binary(4)
        replacement_data_len,
        // 8 	Replace substitution values 	Input 	Char(10)
        "*YES      ",
        // 9 	Return format control characters 	Input 	Char(10)
        "*NO       ",
        // 10 	Error code 	I/O 	Char(*)
        err_plc);
    free(replacement_data_aligned);
    DEBUG("The full message is '%s'\n", msg_info_buf.message);

    DEBUG("About to publish...\n");
    for (int i = 0; i < num_publishers; i++)
    {
      msg_publish_func func = publishers->array[i].msg_publish_func_ptr;
      func(
          session_id.c_str(),
          msgid.c_str(),
          message_type.c_str(),
          message_severity,
          message_timestamp.c_str(),
          job.c_str(),
          sending_usrprf.c_str(),
          msg_info_buf.message,
          sending_program_name.c_str(),
          sending_module_name.c_str(),
          sending_procedure_name.c_str());
      DEBUG("Published\n");
    }
    memset(argv[3], ' ', 10);
    DEBUG("DONE\n");
  }
  else if (watch_option == "*LICLOG")
  {
    DEBUG("Handling LIC log\n");
    vlog_event_raw *lic_event = (vlog_event_raw *)argv[4];
    BUFSTR(major_code, lic_event->lic_log_major_code);
    BUFSTR(minor_code, lic_event->lic_log_minor_code);
    BUFSTR(log_id, lic_event->lic_log_identifier);
    std::string timestamp = get_iso8601_timestamp(lic_event->lic_log_timestamp);
    BUFSTR(tde_number, lic_event->tde_number);
    BUFSTR(task_name, lic_event->task_name);
    BUFSTR(server_type, lic_event->server_type);
    BUFSTR(exception_id, lic_event->exception_id);
    BUFSTR(lic_job_name, lic_event->lic_job_name);
    BUFSTR(lic_user_name, lic_event->lic_user_name);
    BUFSTR(lic_job_number, lic_event->lic_job_number);
    std::string job = lic_job_number + "/" + lic_user_name + "/" + lic_job_name;
    BUFSTR(thread_id, lic_event->thread_id);
    BUFSTR(lic_module_offset, lic_event->lic_module_offset);
    BUFSTR(lic_module_ru_name, lic_event->lic_module_ru_name);
    BUFSTR(lic_module_name, lic_event->lic_module_name);
    BUFSTR(lic_module_entry_point_name, lic_event->lic_module_entry_point_name);

    for (int i = 0; i < num_publishers; i++)
    {
      vlog_publish_func func = publishers->array[i].vlog_publish_func_ptr;
      func(
          session_id.c_str(),
          major_code.c_str(),
          minor_code.c_str(),
          log_id.c_str(),
          timestamp.c_str(),
          tde_number.c_str(),
          task_name.c_str(),
          server_type.c_str(),
          exception_id.c_str(),
          job.c_str(),
          thread_id.c_str(),
          lic_module_offset.c_str(),
          lic_module_ru_name.c_str(),
          lic_module_name.c_str(),
          lic_module_entry_point_name.c_str());
    }
  }
  else if (watch_option == "*PAL")
  {
    DEBUG("Handling PAL Entry\n");
    pal_event_raw *pal_event = (pal_event_raw *)argv[4];
    BUFSTR(system_reference_code, pal_event->system_reference_code);
    BUFSTR(device_name, pal_event->device_name);
    BUFSTR(device_type, pal_event->device_type);
    BUFSTR(model, pal_event->model);
    BUFSTR(serial_number, pal_event->serial_number);
    BUFSTR(resource_name, pal_event->resource_name);
    BUFSTR(log_identifier, pal_event->log_identifier);
    std::string pal_timestamp = get_iso8601_timestamp(pal_event->pal_timestamp);
    BUFSTR(reference_code, pal_event->reference_code);
    BUFSTR(secondary_code, pal_event->secondary_code);
    BUFSTR(table_identifier, pal_event->table_identifier);
    int sequence = pal_event->sequence;

    for (int i = 0; i < num_publishers; i++)
    {
      pal_publish_func func = publishers->array[i].pal_publish_func_ptr;
      func(
          session_id.c_str(),
          system_reference_code.c_str(),
          device_name.c_str(),
          device_type.c_str(),
          model.c_str(),
          serial_number.c_str(),
          resource_name.c_str(),
          log_identifier.c_str(),
          pal_timestamp.c_str(),
          reference_code.c_str(),
          secondary_code.c_str(),
          table_identifier.c_str(),
          sequence);
    }
  }
  else
  {
    for (int i = 0; i < num_publishers; i++)
    {
      other_publish_func func = publishers->array[i].other_publish_func_ptr;
      func(session_id.c_str(), watch_option.c_str());
    }
  }
  ENDDBG();
  return 0;
oh_crap:
  printf("Well, shit\n");
  strncpy(argv[3], "*ERROR    ", 10);
  DEBUG("MCH exception happened!\n");
  ENDDBG();
  return 1;
}