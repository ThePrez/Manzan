#include <string>

#ifndef _MANZAN_JSON_PUB_H_
#define _MANZAN_JSON_PUB_H_
extern "C" {
int json_publish_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE);
int json_publish_vlog(PUBLISH_VLOG_FUNCTION_SIGNATURE);
int json_publish_pal(PUBLISH_PAL_FUNCTION_SIGNATURE);
int json_publish_other(PUBLISH_OTHER_FUNCTION_SIGNATURE);
int to_utf8(char *out, size_t out_len, const char *in, int from_ccsid);
char* get_utf8_output_buf(std::string str);
size_t get_utf8_output_buf_length(std::string str);
}

std::string construct_json_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE);
void printHex(const std::string &label, const std::string &data);
char* convert_field(const std::string& field, int from_ccsid);
void json_encode(std::string &str, const char *_src);


#endif