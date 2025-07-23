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
#include <sstream>
#include <iomanip>

int to_utf8(char *out, size_t out_len, const char *in, int from_ccsid)
{
  QtqCode_T tocode;
  memset(&tocode, 0, sizeof(tocode));
  tocode.CCSID = 1208;
  QtqCode_T fromcode;
  memset(&fromcode, 0, sizeof(fromcode));
  fromcode.CCSID = from_ccsid;

  iconv_t cd = QtqIconvOpen(&tocode, &fromcode);

  size_t inleft = strlen(in); // No +1. We shouldn't include the null terminator here.
  size_t outleft = out_len;
  char *input = (char *)in;
  char *output = out;

  int rc = iconv(cd, &input, &inleft, &output, &outleft);
  if (rc == -1)
  {
    DEBUG_ERROR("Error in converting characters. %d: %s\n", errno, strerror(errno));
    iconv_close(cd);
    return 9;
  }

  // If less than out_len bytes written, add null terminator to end of string. Otherwise
  // we add it to out_len - 1 to make sure we don't overflow the buffer.
  if ((size_t)(output - out) < out_len)
    *output = '\0';
  else
    out[out_len - 1] = '\0';

  DEBUG_INFO("CONVERTED %s to %s\n", in, out);
  DEBUG_INFO("Conversion to UTF-8 successful.\n");

  return iconv_close(cd);
}

char *convert_field(const std::string &field, int from_ccsid)
{
  char *out_buf = get_utf8_output_buf(field);
  to_utf8(out_buf, get_utf8_output_buf_length(field), field.c_str(), from_ccsid == 65535 ? 0 : from_ccsid);
  return out_buf;
}

#include <string>

std::string json_encode(const char *_src)
{
  std::string result;
  const unsigned char *src = reinterpret_cast<const unsigned char *>(_src);

  for (size_t i = 0; src[i] != 0; ++i)
  {
    unsigned char c = src[i];
    if (c == 0x22 || c == 0x5C) // '"' or '\'
    {
      result += 0x5C; // backslash
      result += c;
    }
    else if (c == 0x08) // \b
    {
      result += 0x5C; // backslash
      result += 0x62; // 'b'
    }
    else if (c == 0x0C) // \f
    {
      result += 0x5C; // backslash
      result += 0x66; // 'f'
    }
    else if (c == 0x0A) // \n
    {
      result += 0x5C; // backslash
      result += 0x6E; // 'n'
    }
    else if (c == 0x0D) // \r
    {
      result += 0x5C; // backslash
      result += 0x72; // 'r'
    }
    else if (c == 0x09) // \t
    {
      result += 0x5C; // backslash
      result += 0x74; // 't'
    }
    else if (c == 0x00) // null char
    {
      continue;
    }
    else
    {
      result += c;
    }
  }

  return result;
}

void append_json_element(std::string &_str, const char *_key, const char *_value)
{
  // Here we append the utf-8 bytes directly to avoid ccsid issues. The exception is for the
  // convert_field on _key which is just using English alphabet characters. Those are the same code
  // point in every ccsid, so we just hardcode 37.
  _str += std::string("\x22", 1); // "
  _str += convert_field(_key, 37);
  _str += std::string("\x22\x3A\x22", 3); // ":"
  _str += _value;
  _str += std::string("\x22", 1); // "
}

void append_json_element(std::string &_str, const char *_key, const int _value)
{
  // Here we append the utf-8 bytes directly to avoid ccsid issues. The exception is for the
  // convert_field on _key and value which are just using English alphabet characters and digits. Those are the same code
  // point in every ccsid, so we just hardcode 37.
  _str += std::string("\x22", 1); // "
  _str += convert_field(_key, 37);
  _str += std::string("\x22\x3A\x20", 3); // ":\s
  ITOA(value, _value);
  _str += convert_field(value, 37);
}

size_t get_utf8_output_buf_length(std::string str)
{
  // Each UTF-8 encoded byte can be up to 4 bytes, and add one for the null terminator.
  return str.length() * 4 + 1;
}

size_t get_utf8_output_buf_length(const char *str)
{
  // Each UTF-8 encoded byte can be up to 4 bytes, and add one for the null terminator.
  return strlen(str) * 4 + 1;
}

/*
 * Return an output buffer containing enough space for the utf-8 encoded message.
 * Return NULL if there is no space remaining on the heap.
 * Remember to free the buffer after use.
 */
char *get_utf8_output_buf(std::string str)
{
  char *buf = (char *)malloc(get_utf8_output_buf_length(str));
  if (buf == NULL)
  {
    DEBUG_ERROR("No heap space available to allocate buffer for %s\n", str.c_str());
    return NULL;
  }
  return buf;
}

/*
 * Return an output buffer containing enough space for the utf-8 encoded message.
 * Return NULL if there is no space remaining on the heap.
 * Remember to free the buffer after use.
 */
char *get_utf8_output_buf(const char *str)
{
  char *buf = (char *)malloc(get_utf8_output_buf_length(str) * 4 + 1);
  if (buf == NULL)
  {
    DEBUG_ERROR("No heap space available to allocate buffer for %s\n", str);
    return NULL;
  }
  return buf;
}

int json_publish(const char *_session_id, std::string &_json)
{
  char *utf8 = get_utf8_output_buf(_json);
  if (utf8 == NULL)
  {
    DEBUG_ERROR("No heap space available. Aborting publishing message %s\n", utf8);
    return -1;
  }

  to_utf8(utf8, get_utf8_output_buf_length(_json), _json.c_str(), 0);
  DEBUG_INFO("Publishing JSON\n");
  DEBUG_INFO("%s\n", _json.c_str());

  __attribute__((aligned(16))) char dtaq_key[11];
  memset(dtaq_key, ' ', 11);
  memcpy(dtaq_key, _session_id, MIN(11, strlen(_session_id)));

  _DecimalT<5, 0> len2 = __D("0");
  len2 += strlen(utf8);
  _DecimalT<3, 0> keyLen = __D("10.0");

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

// Debug function to print bytes in hex
void printHex(const std::string &label, const std::string &data)
{
  std::ostringstream oss;
  oss << label << " (length = " << data.size() << "):\n";
  for (size_t i = 0; i < data.size(); ++i)
  {
    unsigned char c = static_cast<unsigned char>(data[i]);
    oss << std::hex << std::uppercase
        << std::setfill('0') << std::setw(2)
        << static_cast<int>(c) << ' ';
  }
  DEBUG_INFO(oss.str().c_str());
}

std::string construct_json_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE)
{
  std::string jsonStr;

  jsonStr += std::string("\x7B\x0A\x20\x20\x20\x20", 6); // {\n\s\s\s\s
  append_json_element(jsonStr, "EVENT_TYPE", convert_field("message", 37));

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "SESSION_ID", _session_id);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "JOB", _job);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "MESSAGE_ID", _msgid);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "MESSAGE_TYPE", _msg_type);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "SEVERITY", _msg_severity);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "MESSAGE_TIMESTAMP", _msg_timestamp);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "SENDING_USRPRF", _sending_usrprf);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "MESSAGE", _message);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "SENDING_PROGRAM_NAME", _sending_program_name);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "SENDING_MODULE_NAME", _sending_module_name);

  jsonStr += std::string("\x2C\x0A\x20\x20\x20\x20", 6); // ,\n\s\s\s\s
  append_json_element(jsonStr, "SENDING_PROCEDURE_NAME", _sending_procedure_name);

  jsonStr += std::string("\x0A\x7D", 2); // \n}

  // Uncomment this if you need to see the raw bytes of the message.
  // Advanced debugging only.
  // printHex("Final JSON in UTF-8 bytes: ", jsonStr);
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