
#include "userconf.h"
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
  strcpy(ret->array[0].library, "JESSEG");
  strcpy(ret->array[0].srvpgm, "MZNDTAQ");
  strncpy(ret->array[0].name, "Default Data Queue publisher", -1 + sizeof(ret->array[0].name));
  strcpy(ret->array[0].library, "JESSEG");
  strcpy(ret->array[0].srvpgm, "MZNDB2");
  strncpy(ret->array[0].name, "Default Db2 publisher", -1 + sizeof(ret->array[0].name));
  g_publisher_info_map[map_key] = ret;
  return ret;
}
void conf_free_publisher_info(publisher_info *_publisher_info)
{
}