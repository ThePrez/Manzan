
#include "userconf.h"
#include "pub_json.h"
#include "pub_db2.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <stdarg.h>
#include <map>

extern "C" bool conf_is_enabled()
{
  return true;
}

static std::map<std::string, publisher_info_set *> g_publisher_info_map;
extern "C" publisher_info_set *conf_get_publisher_info(const char *_session_id)
{
  std::string map_key(_session_id);
  publisher_info_set *cached = g_publisher_info_map[map_key];
  if (cached)
  {
    return cached;
  }
  int num_publishers = 1; // changing to 2 enables the JSON publisher
  size_t alloc_len = sizeof(publisher_info_set) + num_publishers * sizeof(publisher_info);
  publisher_info_set *ret = (publisher_info_set *)malloc(alloc_len);
  memset(ret, 0, alloc_len);
  ret->num_publishers = num_publishers;
  ret->array[1].msg_publish_func_ptr = &json_publish_message;
  ret->array[1].vlog_publish_func_ptr = &json_publish_vlog;
  ret->array[1].pal_publish_func_ptr = &json_publish_pal;
  ret->array[1].other_publish_func_ptr = &json_publish_other;
  strncpy(ret->array[1].name, "Default Data Queue publisher", -1 + sizeof(ret->array[1].name));
  ret->array[0].msg_publish_func_ptr = &db2_publish_message;
  ret->array[0].vlog_publish_func_ptr = &db2_publish_vlog;
  ret->array[0].pal_publish_func_ptr = &db2_publish_pal;
  ret->array[0].other_publish_func_ptr = &db2_publish_other;
  strncpy(ret->array[0].name, "Default Db2 publisher", -1 + sizeof(ret->array[0].name));

  // config style:
  // /QOpenSys/etc/manzan/publishers/
  //    default.conf (if no .conf file for this specific session ID)
  //    BARRY.conf
  //    JESSEG.conf

  // Looks like
  // obj=MANZAN/CAMELPUB

  // TODO: loop through some docs based on session id
  // TODO: find path to object(s) (service programs) for session id
  // APIs used:
  //    1. _RSLVSP2 - get lib pointer
  //    2. _RSLVSP4 - get obj pointer
  //    3. QleActBndPgmLong - activate service program
  //    4. QleGetExpLong - foreach event type, use API to get export function
  //         export msg_publish, export vlog_publish, etc
  //      4.1. if null pointer, don't support it
  //    5. put function pointers into ret array

  g_publisher_info_map[map_key] = ret;
  return ret;
}
