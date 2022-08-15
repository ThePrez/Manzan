BUILDLIB := "MANZAN"

/qsys.lib/${BUILDLIB}.lib/handler.pgm: /qsys.lib/${BUILDLIB}.lib/handler.module /qsys.lib/${BUILDLIB}.lib/pub_json.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/debug.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/userconf.module
	system "CRTPGM PGM(${BUILDLIB}/HANDLER) MODULE(${BUILDLIB}/HANDLER ${BUILDLIB}/PUB_JSON ${BUILDLIB}/PUB_DB2 ${BUILDLIB}/DEBUG ${BUILDLIB}/USERCONF) ACTGRP(*CALLER)""

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.cpp
	system "CRTCPPMOD MODULE(${BUILDLIB}/$*) SRCSTMF('$^') OPTION(*EVENTF) SYSIFCOPT(*IFS64IO) DBGVIEW(*SOURCE) TERASPACE(*YES *TSIFC) STGMDL(*SNGLVL) DTAMDL(*p128) DEFINE(DEBUG_ENABLED)"

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.sqlc
	system "CRTSQLCI OBJ(${BUILDLIB}/$*) SRCSTMF('$^') COMMIT(*NONE) DATFMT(*ISO) TIMFMT(*ISO) CVTCCSID(*JOB) COMPILEOPT('INCDIR(''src'')') SQLPATH(${BUILDLIB}) DFTRDBCOL(${BUILDLIB}) OPTION(*SQL)"

/qsys.lib/${BUILDLIB.lib}:
	system "RUNSQL SQL('create schema ${BUILDLIB}') NAMING(*SYS)"

/qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq:
	system "DLTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ)""
	system "CRTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ) MAXLEN(64512) SEQ(*KEYED) KEYLEN(10) SIZE(*MAX2GB) AUTORCL(*YES)"

/qsys.lib/${BUILDLIB}.lib/%.file: install_tasks/%.sql
	system -kKv "RUNSQLSTM SRCSTMF('$<') COMMIT(*NONE) DFTRDBCOL(${BUILDLIB})"
	echo "Success"

init: /qsys.lib/${BUILDLIB}.lib /qsys.lib/${BUILDLIB}.lib/manzanmsg.file /qsys.lib/${BUILDLIB}.lib/manzanoth.file /qsys.lib/${BUILDLIB}.lib/manzanpal.file /qsys.lib/${BUILDLIB}.lib/manzanvlog.file /qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq 

all: /qsys.lib/${BUILDLIB}.lib/handler.pgm 