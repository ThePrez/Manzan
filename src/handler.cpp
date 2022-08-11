#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qmhrtvm.h>
#include <except.h>
#include "manzan.h"
#include "pub_json.h"

static FILE *fd = NULL;

#pragma pack(1)

#pragma pack(1)
typedef struct
{
  //     0 0 BINARY(4) Length of watch information
  int watch_info_length;
  //     4 4 CHAR(7) Message watched
  char message_watched[7];
  //     11 B CHAR(1) Reserved
  char reserved0[1];
  //     12 C CHAR(10) Message queue name
  char message_queue_name[10];
  //     22 16 CHAR(10) Message queue library
  char message_queue_library[10];
  //     32 20 CHAR(10) Job name
  char job_name[10];
  //     42 2A CHAR(10) User name
  char user_name[10];
  //     52 34 CHAR(6) Job number
  char job_number[6];
  //     58 3A BINARY(4) Original replacement data length
  int original_replacement_data_length;
  //     62 3E CHAR(256) Sending program name
  char sending_program_name[256];
  //     318 13E CHAR(10) Sending module name
  char sending_module_name[10];
  //     328 148 BINARY(4) Offset to sending procedure name
  int offset_send_procedure_name;
  //     332 14C BINARY(4) Length of sending procedure name
  int length_send_procedure_name;
  //     336 150 CHAR(10) Receiving program name
  char receiving_program_name[10];
  //     346 15A CHAR(10) Receiving module name
  char receiving_module_name[10];
  //     356 164 BINARY(4) Offset to receiving procedure name
  int offset_rec_procedure_name;
  //     360 168 BINARY(4) Length of receiving procedure name
  int length_rec_procedure_name;
  //     364 16C BINARY(4) Message severity
  int message_severity;
  //     368 170 CHAR(10) Message type
  char message_type[10];
  //     378 17A CHAR(8) Message timestamp
  char message_timestamp[8];
  //     386 182 CHAR(4) Message key
  char message_key[4];
  //     390 186 CHAR(10) Message file name
  char message_file_name[10];
  //     400 190 CHAR(10) Message file library
  char message_file_library[10];
  //     410 19A CHAR(2) Reserved
  char reserved1[2];
  //     412 19C BINARY(4) Offset to comparison data
  int offset_comparison_data;
  //     416 1A0 BINARY(4) Length of comparison data
  int length_comparison_data;
  //     420 1A4 CHAR(10) Compare against
  char compare_against[10];
  //     430 1AE CHAR(2) Reserved
  char reserved2[2];
  //     432 1B0 BINARY(4) Comparison data CCSID
  int comparison_data_ccsid;
  //     436 1B4 BINARY(4) Offset where comparison data was found
  int offset_comparison_found;
  //     440 1B8 BINARY(4) Offset to replacement data
  int offset_replacement_data;
  //     444 1BC BINARY(4) Length of replacement data
  int length_replacement_data;
  //     448 1C0 BINARY(4) Replacement data CCSID
  int replacement_data_ccsid;
  //     452 1C4 CHAR(10) Sending user profile
  char sending_user_profile[10];
  //     462 1CE CHAR(10) Target job name
  char target_job_name[10];
  //     472 1D8 CHAR(10) Target job user name
  char target_job_user_name[10];
  //     482 1E2 CHAR(6) Target job number
  char target_job_number[6];
  //         **CHAR(*) Sending procedure name
  //         **CHAR(*) Receiving procedure name
  //         **CHAR(*) Message comparison data
  //         **CHAR(*) Message replacement data
} msg_event_raw;

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

int publish_message(const char *_session_id, const char *_msgid, const char *_msg_type, int _msg_severity, const char *_job, char *_message,
                    const char *_sending_program_name, const char *_sending_module_name, const char *_sending_procedure_name)
{
  return json_publish_message(_session_id, _msgid, _msg_type, _msg_severity, _job, _message,
                              _sending_program_name, _sending_module_name, _sending_procedure_name);
}

int publish_other(const char *_session_id, const char *_event_type)
{
  return json_publish_other(_session_id, _event_type);
}

int main(int _argc, char **argv)
{
  static volatile _INTRPT_Hndlr_Parms_T my_commarea;
// https://www.ibm.com/docs/en/i/7.1?topic=descriptions-exception-handler
#pragma exception_handler(oh_crap, my_commarea, _C1_ALL, _C2_ALL, _CTLA_HANDLE_NO_MSG, 0)
  STRDBG();
  BUFSTRN(watch_option, argv[1], 10);
  BUFSTRN(session_id, argv[2], 10);
  DEBUG("watch program called. Watch option setting is '%s'\n", watch_option.c_str());
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
    publish_message(
        session_id.c_str(),
        msgid.c_str(),
        message_type.c_str(),
        message_severity,
        job.c_str(),
        msg_info_buf.message,
        sending_program_name.c_str(),
        sending_module_name.c_str(),
        sending_procedure_name.c_str());
    DEBUG("Published\n");
    memset(argv[3], ' ', 10);
    DEBUG("DONE\n");
  }
  else
  {
    publish_other(session_id.c_str(), watch_option.c_str());
  }
  ENDDBG();
  return 0;
oh_crap:
  strncpy(argv[3], "*ERROR    ", 10);
  DEBUG("MCH exception happened!\n");
  ENDDBG();
  return 1;
}