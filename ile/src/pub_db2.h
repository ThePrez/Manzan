#ifndef _MANZAN_PUB_DB2_H_
#define _MANZAN_PUB_DB2_H_
#include "manzan.h"
#ifdef __cplusplus
extern "C" {
  #endif


extern int db2_publish_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE);
extern int db2_publish_vlog(PUBLISH_VLOG_FUNCTION_SIGNATURE);
extern int db2_publish_pal(PUBLISH_PAL_FUNCTION_SIGNATURE);
extern int db2_publish_other(PUBLISH_OTHER_FUNCTION_SIGNATURE);

#ifdef __cplusplus
}
#endif
#endif