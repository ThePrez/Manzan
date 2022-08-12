#ifndef _USERCONF_H_
#define _USERCONF_H_

typedef struct {
   char library[11];
   char srvpgm[11];
   char name[512];
} publisher_info;

typedef struct {
  int num_publishers;
  publisher_info array[1];
} publisher_info_set;

bool conf_is_enabled();

publisher_info_set *conf_get_publisher_info(const char *_session_id, const char *_watch_event);
void conf_free_publisher_info(publisher_info_set *_publisher_info);

#endif