#include "manzan.h"
#include "pub_db2.h"

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <qp0ztrc.h>

extern "C" int db2_publish_message(PUBLISH_MESSAGE_FUNCTION_SIGNATURE)
{
  return 0;
}

extern "C" int db2_publish_vlog(PUBLISH_VLOG_FUNCTION_SIGNATURE)
{
  return 0;
}

int db2_publish_pal(PUBLISH_PAL_FUNCTION_SIGNATURE)
{
  return 0;
}

int db2_publish_other(PUBLISH_OTHER_FUNCTION_SIGNATURE)
{
  return 0;
}