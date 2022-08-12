#ifndef _USERCONF_H_
#define _USERCONF_H_
#include "manzan.h"

typedef int(msg_publish_func)(PUBLISH_MESSAGE_FUNCTION_SIGNATURE);
typedef int(vlog_publish_func)(PUBLISH_VLOG_FUNCTION_SIGNATURE);
typedef int(pal_publish_func)(PUBLISH_PAL_FUNCTION_SIGNATURE);
typedef int(other_publish_func)(PUBLISH_OTHER_FUNCTION_SIGNATURE);

typedef struct
{
  char name[512];
  msg_publish_func *msg_publish_func_ptr;
  vlog_publish_func *vlog_publish_func_ptr;
  pal_publish_func *pal_publish_func_ptr;
  other_publish_func *other_publish_func_ptr;
} publisher_info;

typedef struct
{
  int num_publishers;
  publisher_info array[1];
} publisher_info_set;

bool conf_is_enabled();

publisher_info_set *conf_get_publisher_info(const char *_session_id, const char *_watch_event);
void conf_free_publisher_info(publisher_info_set *_publisher_info);

#endif