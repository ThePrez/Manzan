#include <stdio.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qmhrtvm.h>
#include <qsnddtaq.h>
#include <qtqiconv.h>

static FILE *fd = NULL;
#define DEBUG_ENABLED 1
#ifdef DEBUG_ENABLED
#define STRDBG() fd = fopen("/home/LINUX/m.txt", "a")
#define DEBUG(...) fprintf(fd, __VA_ARGS__)
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
char *
extract_nts_trim(char *_dest, int _dest_len, char *_src, int _src_len)
{
  memset(_dest, 0x00, _dest_len);
  size_t strncpylen = MIN(-1 + _dest_len, _src_len);
  strncpy(_dest, _src, strncpylen);
  for (int i = 0; i < strncpylen; i++)
  {
    printf("char '%c'\n", _dest[i]);
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
#define NTS(dest, src) extract_nts_trim(dest, sizeof(dest), src, sizeof(src))
#define CNTS(src) NTS(cheater_buf, src)

int publish_message(char *_msgid, char *_job_name, char *_user_name, char *_job_number, char* _message)
{ 
  DEBUG("Publishing to JSON\n");
  char json[1024];
  memset(json, 0x00, sizeof(json));
  sprintf(json, "{\n\"msg_id\": \"%s\",\n\"job\": \"%s/%s/%s\",\n\"message\": \"%s\"\n}\n", 
          _msgid,
          _job_name,
          _user_name,
          _job_number,
          _message);
  DEBUG("%s\n", json);
  char buffity_buf_utf8[1024];
  to_utf8(buffity_buf_utf8, sizeof(buffity_buf_utf8), json);
  QSNDDTAQ("MANZANDTAQ", "JESSEG    ", strlen(buffity_buf_utf8), buffity_buf_utf8);
  return 0;
}
int main(int _argc, char **argv)
{
  STRDBG();
  DEBUG("watch program called.\n");

  char cheater_buf[32];
  DEBUG("Watch option setting is '%s'\n", CNTS(argv[1]));
  if (0 == strcmp("*MSGID", CNTS(argv[1])))
  {
    DEBUG("Handling message\n");
    // handling message
    msg_event_raw *msg_event = (msg_event_raw *)argv[4];
    DEBUG("Message watched is '%s'\n", CNTS(msg_event->message_watched));
    DEBUG("Message type is '%s'\n", CNTS(msg_event->message_type));
    DEBUG("Sending user profile is is '%s'\n", CNTS(msg_event->sending_user_profile));
    int replacement_data_offset = msg_event->offset_replacement_data;
    int replacement_data_len = msg_event->length_replacement_data;
    char *replacement_data = (0 == replacement_data_len) ? "":((char *)msg_event + replacement_data_offset);

    RTVM0100 msg_info_buf;
    memset(&msg_info_buf, 0x00, sizeof(msg_info_buf));
    char err_plc[32];
    memset(err_plc, 0x00, sizeof(err_plc));
    QMHRTVM(
        // 1 	Message information 	Output 	Char(*)
        &msg_info_buf,
        // 2 	Length of message information 	Input 	Binary(4)
        sizeof(msg_info_buf.message),
        // 3 	Format name 	Input 	Char(8)
        "RTVM0100",
        // 4 	Message identifier 	Input 	Char(7)
        msg_event->message_watched,
        // 5 	Qualified message file name 	Input 	Char(20)
        "QCPFMSG   QSYS      ",
        // 6 	Replacement data 	Input 	Char(*)
        replacement_data,
        // 7 	Length of replacement data 	Input 	Binary(4)
        replacement_data_len,
        // 8 	Replace substitution values 	Input 	Char(10)
        "*YES      ",
        // 9 	Return format control characters 	Input 	Char(10)
        "*NO       ",
        // 10 	Error code 	I/O 	Char(*)
        err_plc);
    DEBUG("The full message is '%s'\n", msg_info_buf.message);

    char msgid[8];
    NTS(msgid, msg_event->message_watched);
    char job_name[11];
    NTS(job_name, msg_event->job_name);
    char user_name[11];
    NTS(user_name, msg_event->user_name);
    char job_number[11];
    NTS(job_number, msg_event->job_number);
    DEBUG("About to publish...\n");
    publish_message(msgid, job_name, user_name, job_number, msg_info_buf.message);
    DEBUG("Published\n");
    //memset(argv[3], ' ', 10);
    DEBUG("DONE\n");
  }
  ENDDBG();
}