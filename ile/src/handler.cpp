#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qmhrtvm.h>
#include <qp0ztrc.h>
#include <except.h>
#include <QWCCVTDT.h>
#include "manzan.h"
#include "event_data.h"
#include "userconf.h"
#include "mzversion.h"
#include "pub_json.h"
#include "SockClient.h"
#include <qusrjobi.h>

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
      (void *)"*DTS      ",
      // 2 	Input variable 	Input 	Char(*)
      (void *)_in,
      // 3 	Output format 	Input 	Char(10)
      (void *)"*YYMD     ",
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

int getCurrentJobCcsid() {
    char buffer[400];
    memset(buffer, 0x00, sizeof(buffer));

    QUSRJOBI(
        buffer, sizeof(buffer),
        "JOBI0400",
        "*                         ",  // current job
        "                "             // current thread
    );

    int jobCcsid = 0;
    memcpy(&jobCcsid, buffer + 372, sizeof(int));

    DEBUG_INFO("Job CCSID: %d\n", jobCcsid);

    return jobCcsid;
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
  if ((2 <= _argc) && (0 == strcmp("*VERSION", argv[1]) || 0 == strcmp("*VERSION  ", argv[1]) || 0 == strcmp("--version", argv[1]) || 0 == strcmp("-v", argv[1])))
  {
    Qp0zLprintf("Version: %s\n", MANZAN_VERSION);
    Qp0zLprintf("Build date (UTC): %s\n", MANZAN_BUILDDATE);
    printf("Version: %s\nBuild date (UTC): %s\n", MANZAN_VERSION, MANZAN_BUILDDATE);
    return 0;
  }

  BUFSTRN(watch_option, argv[1], 10);
  BUFSTRN(session_id, argv[2], 10);

  DEBUG_INFO("Watch program called. Watch option setting is '%s'\n", watch_option.c_str());
  publisher_info_set *publishers = conf_get_publisher_info(session_id.c_str());
  int num_publishers = publishers->num_publishers;
  if (0 == num_publishers)
  {
    DEBUG_ERROR("No publishers found for watch option '%s' and session ID '%s'\n", watch_option.c_str(), session_id.c_str());
    ENDDBG();
    return 0;
  }
  if (watch_option == "*MSGID")
  {
    DEBUG_INFO("Handling message\n");
    msg_event_raw *msg_event = (msg_event_raw *)argv[4];
    BUFSTR(msgid, msg_event->message_watched);
    BUFSTR(job_name, msg_event->job_name);
    BUFSTR(user_name, msg_event->user_name);
    BUFSTR(job_number, msg_event->job_number);
    std::string job = job_number + "/" + user_name + "/" + job_name;
    BUFSTR(message_type, msg_event->message_type);
    std::string message_timestamp = get_iso8601_timestamp(msg_event->message_timestamp);
    DEBUG_INFO("TIMESTAMP IS '%s'\n", message_timestamp.c_str());
    int message_severity = msg_event->message_severity;
    BUFSTR(sending_usrprf, msg_event->sending_user_profile);
    BUFSTRN(sending_procedure_name, (char *)msg_event + msg_event->offset_send_procedure_name, msg_event->length_send_procedure_name);
    BUFSTR(sending_module_name, msg_event->sending_module_name);
    BUFSTR(sending_program_name, msg_event->sending_program_name);
    sending_program_name.erase(1 + sending_program_name.find_last_not_of(" "));

    int replacement_data_offset = msg_event->offset_replacement_data;
    int replacement_data_len = msg_event->length_replacement_data;
    DEBUG_INFO("REPLACEMENT DATA OFFSET IS '%d'\n", replacement_data_offset);
    DEBUG_INFO("REPLACEMENT DATA LENGTH IS '%d'\n", replacement_data_len);
    DEBUG_INFO("REPLACEMENT DATA CCSID IS %d\n",msg_event->replacement_data_ccsid );
    char message_watched[8];
    message_watched[7] = 0x00;
    memcpy(message_watched, msg_event->message_watched, 7);
    DEBUG_INFO("MESSAGE WATCHED IS '%s'\n", message_watched);
    char qualified_msg_file[21];
    qualified_msg_file[20] = 0x00;
    memcpy(qualified_msg_file, msg_event->message_file_name, 10);
    memcpy(qualified_msg_file+10, msg_event->message_file_library, 10);
    DEBUG_INFO("MESSAGE FILE AND NAME IS '%s'\n", qualified_msg_file);
    char *replacement_data = (0 == replacement_data_len) ? (char *)"" : (((char *)msg_event) + replacement_data_offset);
    char *replacement_data_aligned = (char *)malloc(1 + replacement_data_len);
    memset(replacement_data_aligned, 0x00, 1 + replacement_data_len);
    memcpy(replacement_data_aligned, replacement_data, (size_t)replacement_data_len);

    size_t msg_info_buf_size = 128 + sizeof(RTVM0100) + replacement_data_len;
    RTVM0100 *msg_info_buf = (RTVM0100 *)malloc(msg_info_buf_size);
    memset(msg_info_buf, 0x00, msg_info_buf_size);
    if (' ' == qualified_msg_file[0])
    {
      DEBUG_INFO("Message not from message file\n");
      strncpy(msg_info_buf->message, replacement_data_aligned, replacement_data_len);
    }
    else
    {
      char err_plc[64];
      memset(err_plc, 0x00, sizeof(err_plc));
      DEBUG_INFO("About to format...\n");

      QMHRTVM(
          // 1 	Message information 	Output 	Char(*)
          msg_info_buf,
          // 2 	Length of message information 	Input 	Binary(4)
          msg_info_buf_size - 1,
          // 3 	Format name 	Input 	Char(8)
          "RTVM0100",
          // 4 	Message identifier 	Input 	Char(7)
          message_watched,
          // 5 	Qualified message file name 	Input 	Char(20)
          qualified_msg_file,
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
      DEBUG_INFO("Done formatting \n");
      DEBUG_INFO("The full message is '%s'\n", msg_info_buf->message);
    }
    free(replacement_data_aligned);
    DEBUG_INFO("About to publish...\n");

    // We can uncomment the next two lines if we are debugging ccsid issues
    // std::string message_asstr(msg_info_buf->message);
    // printHex("PRINTING HEX BYTES original ccsid: ",message_asstr );

    int job_ccsid = getCurrentJobCcsid();

    char *session_id_utf8           = convert_field(session_id, job_ccsid);
    char *msgid_utf8                = convert_field(msgid, job_ccsid);
    char *msg_type_utf8             = convert_field(message_type, job_ccsid);
    char *msg_timestamp_utf8        = convert_field(message_timestamp, job_ccsid);
    char *job_utf8                  = convert_field(job, job_ccsid);
    char *sending_usrprf_utf8       = convert_field(sending_usrprf, job_ccsid);
    char *message_utf8_before_encode = convert_field(msg_info_buf->message, msg_event->replacement_data_ccsid);
    char *sending_program_name_utf8= convert_field(sending_program_name, job_ccsid);
    char *sending_module_name_utf8  = convert_field(sending_module_name, job_ccsid);
    char *sending_procedure_name_utf8 = convert_field(sending_procedure_name, job_ccsid);

    std::string message_utf8 = json_encode(message_utf8_before_encode);

    // We can uncomment the next two lines if we are debugging ccsid issues
    // printHex("PRINTING HEX BYTES for message encoded and utf8: ", message_utf8 );


    for (int i = 0; i < num_publishers; i++)
    {
      msg_publish_func func = publishers->array[i].msg_publish_func_ptr;
      func(
          session_id_utf8,
          msgid_utf8,
          msg_type_utf8,
          message_severity,
          msg_timestamp_utf8,
          job_utf8,
          sending_usrprf_utf8,
          message_utf8.c_str(),
          sending_program_name_utf8,
          sending_module_name_utf8,
          sending_procedure_name_utf8);
      DEBUG_INFO("Published\n");
    }
    free(msg_info_buf);
    memset(argv[3], ' ', 10);
    DEBUG_INFO("DONE\n");
  }
  else if (watch_option == "*LICLOG")
  {
    DEBUG_INFO("Handling LIC log\n");
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
    DEBUG_INFO("Handling PAL Entry\n");
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
  if (4 <= _argc)
    strncpy(argv[3], "*ERROR    ", 10);
  DEBUG_ERROR("MCH exception happened!\n");
  ENDDBG();
  return 1;
}