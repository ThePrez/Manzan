#include "manzan.h"
#include "pub_json.h"

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>
#include <qsnddtaq.h>
#include <qtqiconv.h>
#include <bcd.h>

int to_utf8(char *out, size_t out_len, const char *in)
{
  QtqCode_T tocode;
  memset(&tocode, 0, sizeof(tocode));
  tocode.CCSID = 1208;
  QtqCode_T fromcode;

  // Setting to 0 allows the system to automatically detect ccsid (hopefully)
  memset(&fromcode, 0, sizeof(fromcode));
  iconv_t cd = QtqIconvOpen(&tocode, &fromcode);

  size_t inleft = 1 + strlen(in);
  size_t outleft = out_len;
  char *input = (char *)in;
  char *output = out;

  int rc = iconv(cd, &input, &inleft, &output, &outleft);
  if (rc == -1)
  {
    DEBUG_ERROR("Error in converting characters. %d: %s\n", errno, strerror(errno));
    return 9;
  }
  DEBUG_INFO("Conversion to UTF-8 successful.\n");
  return iconv_close(cd);
}

void json_encode(std::string &str, const char *_src)
{
  for (int i = 0; _src[i] != 0; i++)
  {
    char c = _src[i];
    switch (c)
    {
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
void append_json_element(std::string &_str, const char *_key, const char *_value)
{
    _str += "\"";
  _str += _key;
  _str += "\":\"";
  std::string encoded;
  json_encode(encoded, _value);
  _str += encoded;
  _str += "\"";
}
void append_json_element(std::string &_str, const char *_key, const int _value)
{
  _str += "\"";
  _str += _key;
  _str += "\": ";
  ITOA(value, _value);
  _str += value;
}

size_t get_utf8_output_buf_length(std::string str){
  // Each UTF-8 encoded byte can be up to 4 bytes, and add one for the null terminator.
  return str.length() * 4 + 1;
}

/*
* Return an output buffer containing enough space for the utf-8 encoded message.
* Return NULL if there is no space remaining on the heap.
* Remember to free the buffer after use.
*/
char* get_utf8_output_buf(std::string str){
  char *buf = (char *)malloc(get_utf8_output_buf_length(str));
    if (buf == NULL) {
        DEBUG_ERROR("No heap space available to allocate buffer for %s\n", str.c_str());
        return NULL;
    }
    return buf;
}

int json_publish(const char *_session_id, std::string &_json)
{
  char *utf8 = get_utf8_output_buf(_json);
  if (utf8 == NULL){
    DEBUG_ERROR("No heap space available. Aborting publishing message %s\n", utf8);
    return -1;
  }

  to_utf8(utf8, get_utf8_output_buf_length(_json), _json.c_str());
  DEBUG_INFO("Publishing JSON\n");
  DEBUG_INFO("%s\n", _json.c_str());

  __attribute__((aligned(16))) char  dtaq_key[11];
  memset(dtaq_key, ' ', 11);
  memcpy(dtaq_key, _session_id, MIN(11, strlen(_session_id)));

  _DecimalT<5,0> len2 = __D("0"); 
  len2 += strlen(utf8);
  _DecimalT<3,0> keyLen = __D("10.0");

  DEBUG_INFO("About to call QSNDDTAQ\n");
  QSNDDTAQ("MANZANDTAQ",
           "MANZAN    ", // TODO: How to properly resolve the library here?
           len2,
           utf8,
           keyLen,
           &dtaq_key);
  DEBUG_INFO("About to free up stuff\n");
  free(utf8);
  DEBUG_INFO("Done publishing JSON\n");
  return 0;
}

std::string construct_json_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "event_type", "message");
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "session_id", _session_id);
  jsonStr += ",\n    "; 
  append_json_element(jsonStr, "job", _job);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "msgid", _msgid);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "msgtype", _msg_type);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "severity", _msg_severity);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "message_timestamp", _msg_timestamp);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_usrprf", _sending_usrprf);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "message", _message);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_program_name", _sending_program_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_module_name", _sending_module_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_procedure_name", _sending_procedure_name);
  jsonStr += "\n}";
  return jsonStr;
}

std::string kill_me_now2(PUBLISH_MESSAGE_FUNCTION_SIGNATURE)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "event_type", "message");
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "session_id", _session_id);
  jsonStr += ",\n    "; 
  append_json_element(jsonStr, "job", _job);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "msgid", _msgid);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "msgtype", _msg_type);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "severity", _msg_severity);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "message_timestamp", _msg_timestamp);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_usrprf", _sending_usrprf);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "message", _message);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_program_name", _sending_program_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_module_name", _sending_module_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_procedure_name", _sending_procedure_name);
  jsonStr += "\n}";
  return jsonStr;
}

/**
 * Publish json message to DTAQ
 */
extern "C" int json_publish_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE)
{
  std::string jsonStr = construct_json_message(_session_id, _msgid, _msg_type, _msg_severity, _msg_timestamp, _job, _sending_usrprf,
  _message, _sending_program_name, _sending_module_name, _sending_procedure_name);
  
  return json_publish(_session_id, jsonStr);
}

extern "C" int json_publish_vlog(PUBLISH_VLOG_FUNCTION_SIGNATURE)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "event_type", "vlog");
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "session_id", _session_id);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "major_code", _major_code);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "minor_code", _minor_code);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "log_id", _log_id);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "timestamp", _timestamp);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "tde_number", _tde_number);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "task_name", _task_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "server_type", _server_type);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "exception_id", _exception_id);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "job", _job);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "thread_id", _thread_id);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "module_offset", _module_offset);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "module_ru_name", _module_ru_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "module_name", _module_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "module_entry_point_name", _module_entry_point_name);

  jsonStr += "\n}";
  return json_publish(_session_id, jsonStr);
}

extern "C" int json_publish_pal(PUBLISH_PAL_FUNCTION_SIGNATURE)
{
  return 0; // TODO: Implement this
}

extern "C" int json_publish_other(PUBLISH_OTHER_FUNCTION_SIGNATURE)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "event_type", _event_type);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "session_id", _session_id);
  jsonStr += "\n}";
  return json_publish(_session_id, jsonStr);
}