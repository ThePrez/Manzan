#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qmhrtvm.h>
#include <qsnddtaq.h>
#include <qtqiconv.h>
#include <except.h>

static FILE *fd = NULL;
#define DEBUG_ENABLED 1
#ifdef DEBUG_ENABLED
#define STRDBG() fd = fopen("/home/LINUX/m.txt", "a")
#define DEBUG(...) fprintf(fd, __VA_ARGS__); fflush(fd)
#define ENDDBG() fclose(fd)
#else
#define STRDBG()
#define DEBUG(...)
#define ENDDBG()
#endif

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
#define MIN(a, b) ((a) < (b) ? (a) : (b))

void myhandler(_INTRPT_Hndlr_Parms_T *_parms)
{
  DEBUG("MCH exception happened!\n");
  exit(-1);
}

char *
extract_nts_trim(char *_dest, int _dest_len, char *_src, int _src_len)
{
  memset(_dest, 0x00, _dest_len);
  size_t strncpylen = MIN(-1 + _dest_len, _src_len);
  strncpy(_dest, _src, strncpylen);
  for (int i = 0; i < strncpylen; i++)
  {
    if (' ' == _dest[i])
    {
      _dest[i] = 0x00;
      return _dest;
    }
    if (0x00 == _dest[i])
    {
      return _dest;
    }
  }
  return _dest;
}
int to_utf8(char *out, size_t out_len, const char *in)
{
  QtqCode_T tocode;
  memset(&tocode, 0, sizeof(tocode));
  tocode.CCSID = 819;
  QtqCode_T fromcode;
  fromcode.CCSID = 37;
  memset(&fromcode, 0, sizeof(fromcode));
  iconv_t cd = QtqIconvOpen(&tocode, &fromcode);

  size_t inleft = 1+strlen(in);
  size_t outleft = out_len;
  char *input = (char *)in;
  char *output = out;

  int rc = iconv(cd, &input, &inleft, &output, &outleft);
  if (rc == -1)
  {
    DEBUG("Error in converting characters\n");
    return 9;
  }
  return iconv_close(cd);
}

#define BUFSTR(dest, src) std::string dest(src, sizeof(src))
#define BUFSTRN(dest, src, len) std::string dest(src, len)

void json_encode(std::string& str, const char* _src)
{
  for(int i=0; _src[i] != 0; i++)
  {
    char c = _src[i];
    switch(c) {
      case '"':
        str += "\\\"";
        break;
      case '\\':
        str += "\\\\";
        break;
      case '\b':
        str += "\\b";
        break;
      case '\f':
        str += "\\f";
        break;
      case '\n':
        str += "\\n";
        break;
      case '\r':
        str += "\\r";
        break;
      case '\t':
        str += "\\t";
        break;
      case '\0':
        str += "\\0";
        return;
      default:
        str += c;
        break;
    }
  }
}
void append_json_element(std::string& _str, const char *_key, const char *_value)
{
  _str += "\"";
  _str += _key;
  _str += "\":\"";
  std::string encoded;
  json_encode(encoded, _value);
  _str += encoded;
  _str += "\"";
}

int publish_message(const char *_msgid, const char *_job, char* _message)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "msgid", _msgid);
  jsonStr+=",\n    ";
  append_json_element(jsonStr, "job", _job);
  jsonStr+=",\n    ";
  append_json_element(jsonStr, "message", _message);

  jsonStr += "\n}";

  int json_len = 1+jsonStr.length();
  char* utf8 = (char*)malloc(56+json_len*2);

  to_utf8(utf8, json_len, jsonStr.c_str());
  DEBUG("%s\n", jsonStr.c_str());
  QSNDDTAQ("MANZANDTAQ", "JESSEG    ", strlen(utf8), utf8);
  free(utf8);
  return 0;
}
int main(int _argc, char **argv)
{
  static volatile _INTRPT_Hndlr_Parms_T my_commarea;
// https://www.ibm.com/docs/en/i/7.1?topic=descriptions-exception-handler
#pragma exception_handler(myhandler, my_commarea, _C1_ALL, _C2_ALL, _CTLA_HANDLE, 0)
  STRDBG();

#ifdef DEBUG_ENABLED
  if(NULL == fd) { 
    return 0;
  }
#endif
  //system("CHGJOB LOG(4 00 *SECLVL)");
  Qp0zLprintf("Liam was here\n");
      DEBUG("watch program called.\n");
  BUFSTRN(watch_option, argv[1], 10);
  DEBUG("Watch option setting is '%s'\n", watch_option.c_str());
  if (0 == strncmp("*MSGID", watch_option.c_str(), 6))
  {
    DEBUG("Handling message\n");
    // handling message
    msg_event_raw *msg_event = (msg_event_raw *)argv[4];
    int len = msg_event->watch_info_length;
    DEBUG("Watch info length is '%d'\n", len);

    BUFSTR(msgid, msg_event->message_watched);
    BUFSTR(job_name, msg_event->job_name);
    BUFSTR(user_name, msg_event->user_name);
    BUFSTR(job_number, msg_event->job_number);
    std::string job = job_number+"/"+user_name+"/"+job_name;
    BUFSTR(message_type, msg_event->message_type);
    BUFSTR(sending_usrprf, msg_event->sending_user_profile);
    BUFSTRN(sending_procedure_name, (char *)msg_event + msg_event->offset_send_procedure_name, msg_event->length_send_procedure_name);
    BUFSTR(sending_module_name, msg_event->sending_module_name);
    BUFSTR(sending_program_name, msg_event->sending_program_name);
    sending_program_name.erase(1 + sending_program_name.find_last_not_of(" "));

    if (0 == strcmp(job_name.c_str(), "QSCWCHPS"))
    {
      DEBUG("hiding my secrets\n");
      //  system("SBMJOB CMD(ENDWCH BARRY)");
      strncpy(argv[3], "*ERROR    ", 10);
      ENDDBG();
      return 0;
    }
    DEBUG("Message watched is '%s'\n", msgid.c_str());
    DEBUG("Message type is '%s'\n", message_type.c_str());
    DEBUG("Sending user profile is is '%s'\n",sending_usrprf.c_str());

    int replacement_data_offset = msg_event->offset_replacement_data;
    int replacement_data_len = msg_event->length_replacement_data;
    DEBUG("Replacement data offset is '%d'\n", replacement_data_offset);
    DEBUG("REPLACEMENT DATA LENGTH IS '%d'\n", replacement_data_len);
    DEBUG("Sending procedure is '%s'\n", sending_procedure_name.c_str());
    DEBUG("Sending module is '%s'\n", sending_module_name.c_str());
    DEBUG("Sending program is '%s'\n", sending_program_name.c_str());
    char *replacement_data = (0 == replacement_data_len) ? "":(((char *)msg_event) + replacement_data_offset);
    char* replacement_data_aligned = (char*)malloc(replacement_data_len);
    memcpy(replacement_data_aligned, replacement_data, replacement_data_len);

    RTVM0100 msg_info_buf;
    memset(&msg_info_buf, 0x00, sizeof(msg_info_buf));
    char err_plc[64];
    memset(err_plc, 0x00, sizeof(err_plc));
    DEBUG("About to call RTVM0100\n");
    QMHRTVM(
        // 1 	Message information 	Output 	Char(*)
        &msg_info_buf,
        // 2 	Length of message information 	Input 	Binary(4)
        -1+sizeof(msg_info_buf),
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
    DEBUG("RTVM0100 returned\n");
    free(replacement_data_aligned);
    DEBUG("The full message is '%s'\n", msg_info_buf.message);

    DEBUG("About to publish...\n");
    publish_message(msgid.c_str(), job.c_str(), msg_info_buf.message);
    DEBUG("Published\n"); 
    //memset(argv[3], ' ', 10);
    DEBUG("DONE\n");
  }
  ENDDBG();
  return 0;
}