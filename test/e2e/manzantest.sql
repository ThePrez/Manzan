﻿SELECT MESSAGE_DATA_UTF8 FROM TABLE(QSYS2.DATA_QUEUE_ENTRIES(
                                             DATA_QUEUE => 'MANZANDTAQ', 
                                             DATA_QUEUE_LIBRARY => 'JESSEG'))
  ORDER BY ORDINAL_POSITION;
  

    CALL QSYS2.CLEAR_DATA_QUEUE('MANZANDTAQ', 'JESSEG');  