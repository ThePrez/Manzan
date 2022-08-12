
#include "userconf.h"
#include "pub_json.h"
#include "pub_db2.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <stdarg.h>
#include <map>

bool conf_is_enabled()
{
  return true;
}

static std::map<std::string, publisher_info_set *> g_publisher_info_map;
publisher_info_set *conf_get_publisher_info(const char *_session_id, const char *_watch_event)
{
  std::string map_key = std::string(_session_id) + "::" + std::string(_watch_event);
  publisher_info_set *cached = g_publisher_info_map[map_key];
  if (cached)
  {
    return cached;
  }
  int num_publishers = 2;
  size_t alloc_len = sizeof(publisher_info_set) + num_publishers * sizeof(publisher_info);
  publisher_info_set *ret = (publisher_info_set *)malloc(alloc_len);
  memset(ret, 0, alloc_len);
  ret->num_publishers = num_publishers;
  ret->array[0].msg_publish_func_ptr = &json_publish_message;
  ret->array[0].vlog_publish_func_ptr = &json_publish_vlog;
  ret->array[0].pal_publish_func_ptr = &json_publish_pal;
  ret->array[0].other_publish_func_ptr = &json_publish_other;
  strncpy(ret->array[0].name, "Default Data Queue publisher", -1 + sizeof(ret->array[0].name));
  ret->array[1].msg_publish_func_ptr = &db2_publish_message;
  ret->array[1].vlog_publish_func_ptr = &db2_publish_vlog;
  ret->array[1].pal_publish_func_ptr = &db2_publish_pal;
  ret->array[1].other_publish_func_ptr = &db2_publish_other;
  strncpy(ret->array[1].name, "Default Db2 publisher", -1 + sizeof(ret->array[0].name));

  g_publisher_info_map[map_key] = ret;
  return ret;
}
void conf_free_publisher_info(publisher_info *_publisher_info)
{
}