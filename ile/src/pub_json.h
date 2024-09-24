#include <string>

#ifndef _MANZAN_JSON_PUB_H_
#define _MANZAN_JSON_PUB_H_
extern "C" {
int json_publish_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE);
int json_publish_vlog(PUBLISH_VLOG_FUNCTION_SIGNATURE);
int json_publish_pal(PUBLISH_PAL_FUNCTION_SIGNATURE);
int json_publish_other(PUBLISH_OTHER_FUNCTION_SIGNATURE);
}

std::string construct_json_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE);

#endif