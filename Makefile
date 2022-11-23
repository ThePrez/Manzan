BUILDLIB=MANZAN

init: /qsys.lib/${BUILDLIB}.lib /qsys.lib/${BUILDLIB}.lib/manzanmsg.file /qsys.lib/${BUILDLIB}.lib/manzanoth.file /qsys.lib/${BUILDLIB}.lib/manzanpal.file /qsys.lib/${BUILDLIB}.lib/manzanvlog.file /qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq 

all: /qsys.lib/${BUILDLIB}.lib/handler.pgm

/qsys.lib/${BUILDLIB}.lib/handler.pgm: /qsys.lib/${BUILDLIB}.lib/handler.module /qsys.lib/${BUILDLIB}.lib/pub_json.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/debug.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/userconf.module

/qsys.lib/${BUILDLIB}.lib/%.pgm:
	system "CRTPGM PGM(${BUILDLIB}/$*) MODULE($(patsubst %.module,$(BUILDLIB)/%,$(notdir $^))) ACTGRP(*CALLER)"

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.cpp
	system "CRTCPPMOD MODULE(${BUILDLIB}/$*) SRCSTMF('$^') OPTION(*EVENTF) SYSIFCOPT(*IFS64IO) DBGVIEW(*SOURCE) TERASPACE(*YES *TSIFC) STGMDL(*SNGLVL) DTAMDL(*p128) DEFINE(DEBUG_ENABLED)"

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.sqlc
	system "CRTSQLCI OBJ(${BUILDLIB}/$*) SRCSTMF('$^') COMMIT(*NONE) DATFMT(*ISO) TIMFMT(*ISO) CVTCCSID(*JOB) COMPILEOPT('INCDIR(''src'')') SQLPATH(${BUILDLIB}) DFTRDBCOL(${BUILDLIB}) OPTION(*SQL)"

/qsys.lib/${BUILDLIB.lib}:
	-system "RUNSQL SQL('create schema ${BUILDLIB}') NAMING(*SYS)"

/qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq:
	-system "DLTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ)"
	system "CRTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ) MAXLEN(64512) SEQ(*KEYED) KEYLEN(10) SIZE(*MAX2GB) AUTORCL(*YES)"

/qsys.lib/${BUILDLIB}.lib/%.msgq:
	-system "DLTMSGQ MSGQ(${BUILDLIB}/$*)"
	system -kKv "CRTMSGQ MSGQ(${BUILDLIB}/$*) TEXT('Testing queue') CCSID(1208)"

/qsys.lib/${BUILDLIB}.lib/%.file: install_tasks/%.sql
	system -kKv "RUNSQLSTM SRCSTMF('$<') COMMIT(*NONE) DFTRDBCOL(${BUILDLIB})"
	echo "Success"

.PHONY: testing
testing: /qsys.lib/${BUILDLIB}.lib/MANZANQ.msgq watch_start watch_testq watch_end

watch_testq:
	-java -cp ./tester:/QIBM/ProdData/OS400/jt400/lib/jt400.jar sndmsg

watch_start:
	# Listens to chosen message queue for all messages
	# then calls the handler program
	# Check /tmp/manzan_debug.txt for logs
	system -kKv "STRWCH SSNID(TESTING) WCHPGM(${BUILDLIB}/HANDLER) CALLWCHPGM(*STRWCH) WCHMSG((*ALL)) WCHMSGQ((${BUILDLIB}/MANZANQ))"

watch_end:
	system -kKv "ENDWCH SSNID(TESTING)"
	# select rtrim(HANDLED_TIMESTAMP) as TS, rtrim(SESSION_ID) as SESSION, rtrim(MESSAGE_ID) as MSG_ID from jesseg.manzanmsg where SESSION_ID = 'TESTING   '