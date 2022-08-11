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

int to_utf8(char *out, size_t out_len, const char *in)
{
  QtqCode_T tocode;
  memset(&tocode, 0, sizeof(tocode));
  tocode.CCSID = 1208;
  QtqCode_T fromcode;
  fromcode.CCSID = 37;
  memset(&fromcode, 0, sizeof(fromcode));
  iconv_t cd = QtqIconvOpen(&tocode, &fromcode);

  size_t inleft = 1 + strlen(in);
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

int json_publish(std::string &_json)
{
  int json_len = 1 + _json.length();
  char *utf8 = (char *)malloc(56 + _json.length() * 2);

  to_utf8(utf8, json_len, _json.c_str());
  DEBUG("Publishing JSON\n");
  DEBUG("%s\n", _json.c_str());
  QSNDDTAQ("MANZANDTAQ", "JESSEG    ", strlen(utf8), utf8);
  free(utf8); 
  return 0;
}

int json_publish_message(const char *_session_id, const char *_msgid, const char *_msg_type, int _msg_severity, const char *_job, char *_message,
                    const char *_sending_program_name, const char *_sending_module_name, const char *_sending_procedure_name)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "event_type", "message");
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "session_id", _session_id);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "msgid", _msgid);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "msgtype", _msg_type);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "severity", _msg_severity);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "job", _job);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "message", _message);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_program_name", _sending_program_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_module_name", _sending_module_name);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "sending_procedure_name", _sending_procedure_name);

  jsonStr += "\n}";
  return json_publish(jsonStr);
}

int json_publish_other(const char *_session_id, const char *_event_type)
{
  std::string jsonStr;
  jsonStr += "{\n    ";
  append_json_element(jsonStr, "event_type", _event_type);
  jsonStr += ",\n    ";
  append_json_element(jsonStr, "session_id", _session_id);
  jsonStr += "\n}";
  return json_publish(jsonStr);
}