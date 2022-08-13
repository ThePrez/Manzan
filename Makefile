BUILDLIB := "MANZAN"

/qsys.lib/${BUILDLIB}.lib/handler.pgm: /qsys.lib/${BUILDLIB}.lib/handler.module /qsys.lib/${BUILDLIB}.lib/pub_json.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/debug.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/userconf.module
	system "CRTPGM PGM(${BUILDLIB}/HANDLER) MODULE(${BUILDLIB}/HANDLER ${BUILDLIB}/PUB_JSON ${BUILDLIB}/PUB_DB2 ${BUILDLIB}/DEBUG ${BUILDLIB}/USERCONF) ACTGRP(*CALLER)""

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.cpp
	system "CRTCPPMOD MODULE(${BUILDLIB}/$*) SRCSTMF('$^') OPTION(*EVENTF) SYSIFCOPT(*IFS64IO) DBGVIEW(*SOURCE) TERASPACE(*YES *TSIFC) STGMDL(*SNGLVL) DTAMDL(*p128) DEFINE(DEBUG_ENABLED)"

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.sqlc
	system "CRTSQLCI OBJ(${BUILDLIB}/$*) SRCSTMF('$^') DATFMT(*ISO) TIMFMT(*ISO) CVTCCSID(*JOB) COMPILEOPT('INCDIR(''src'')')""

/qsys.lib/${BUILDLIB.lib}:
	system "RUNSQL SQL('create schema ${BUILDLIB}') NAMING(*SYS)"

/qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq:
	system "DLTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ)""
	system "CRTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ) MAXLEN(64512) SEQ(*KEYED) KEYLEN(10) SIZE(*MAX2GB) AUTORCL(*YES)"

/qsys.lib/${BUILDLIB}.lib/%.file: install_tasks/%.sql
	mkdir -p ./.build/install_tasks || echo "Failed to create .build/install_tasks"
	cat $^ | /QOpenSys/usr/bin/sed -e 's|%%LIB%%|'${BUILDLIB}'|g' > ./.build/$^
	system -kKv "RUNSQLSTM SRCSTMF('./.build/$^') COMMIT(*NONE)"
	echo "Success"

init: /qsys.lib/${BUILDLIB}.lib /qsys.lib/${BUILDLIB}.lib/manzanmsg.file /qsys.lib/${BUILDLIB}.lib/manzanoth.file /qsys.lib/${BUILDLIB}.lib/manzanpal.file /qsys.lib/${BUILDLIB}.lib/manzanvlog.file /qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq 

all: /qsys.lib/${BUILDLIB}.lib/handler.pgm 