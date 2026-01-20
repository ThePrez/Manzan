BUILDLIB?=MANZAN
BUILDVERSION:="Development build \(built with Make\)"

all: init /qsys.lib/${BUILDLIB}.lib/handler.pgm

init: /qsys.lib/${BUILDLIB}.lib /qsys.lib/${BUILDLIB}.lib/manzanaud.file /qsys.lib/${BUILDLIB}.lib/manzanmsg.file /qsys.lib/${BUILDLIB}.lib/manzanoth.file /qsys.lib/${BUILDLIB}.lib/manzanpal.file /qsys.lib/${BUILDLIB}.lib/manzanvlog.file /qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq

.PHONY: src/mzversion.h

src/mzversion.h:
	rm -f $@
	echo "#ifndef __MZVERSION_H" > $@
	echo "#define __MZVERSION_H" >> $@
	echo "#define MANZAN_VERSION \"${BUILDVERSION}\"" >> $@
	echo "#define MANZAN_BUILDDATE \"$(shell date --universal)\"" >> $@
	echo "#endif" >> $@
	grep MANZAN_ $@

/qsys.lib/${BUILDLIB}.lib:
	system "RUNSQL SQL('create schema ${BUILDLIB}')  COMMIT(*NONE) NAMING(*SQL) "

/qsys.lib/${BUILDLIB}.lib/handler.pgm: /qsys.lib/${BUILDLIB}.lib/handler.module /qsys.lib/${BUILDLIB}.lib/pub_json.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/debug.module /qsys.lib/${BUILDLIB}.lib/pub_db2.module /qsys.lib/${BUILDLIB}.lib/userconf.module /qsys.lib/${BUILDLIB}.lib/SockClient.module
/qsys.lib/${BUILDLIB}.lib/%.module: src/%.cpp src/mzversion.h
	@echo "Compiling $< -> ${BUILDLIB}/$*"
	@if ! system "CRTCPPMOD MODULE(${BUILDLIB}/$*) SRCSTMF('$(CURDIR)/$<') \
	               OPTION(*EVENTF) SYSIFCOPT(*IFS64IO) DBGVIEW(*SOURCE) \
	               TERASPACE(*YES *TSIFC) STGMDL(*SNGLVL) DTAMDL(*p128) \
	               DEFINE(DEBUG_ENABLED) OUTPUT(*PRINT) TGTCCSID(*JOB)"; then \
		echo "CRTCPPMOD failed for ${BUILDLIB}/$*; retrieving error details..."; \
		echo '===== Job Log (Last 50 Messages) ====='; \
		system "DSPJOBLOG OUTPUT(*PRINT)" > /dev/null 2>&1 || true; \
		system "CPYSPLF FILE(QPJOBLOG) TOFILE(*TOSTMF) JOB(*) SPLNBR(*LAST) TOSTMF('/tmp/joblog.txt') STMFOPT(*REPLACE)" > /dev/null 2>&1 && tail -50 /tmp/joblog.txt || echo 'Could not retrieve job log'; \
		echo '===== End of Job Log ====='; \
		exit 1; \
	fi

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.sqlc
	system "CRTSQLCPPI OBJ(${BUILDLIB}/$*) SRCSTMF('$(CURDIR)/$^') COMMIT(*NONE) DATFMT(*ISO) TIMFMT(*ISO) DBGVIEW(*SOURCE) CVTCCSID(*JOB) COMPILEOPT('INCDIR(''src'')') SQLPATH(${BUILDLIB}) DFTRDBCOL(${BUILDLIB}) OPTION(*SQL)" 

/qsys.lib/${BUILDLIB.lib}:
	-system "RUNSQL SQL('create schema ${BUILDLIB}') NAMING(*SYS)"

/qsys.lib/${BUILDLIB}.lib/manzandtaq.dtaq:
	-system "DLTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ)"
	system "CRTDTAQ DTAQ(${BUILDLIB}/MANZANDTAQ) MAXLEN(64512) SEQ(*KEYED) KEYLEN(10) SIZE(*MAX2GB) AUTORCL(*YES)"

/qsys.lib/${BUILDLIB}.lib/%.msgq:
	-system "DLTMSGQ MSGQ(${BUILDLIB}/$*)"
	system -kKv "CRTMSGQ MSGQ(${BUILDLIB}/$*) TEXT('Testing queue') CCSID(1208)"

/qsys.lib/${BUILDLIB}.lib/%.file: install_tasks/%.sql
	system -kKv "RUNSQLSTM SRCSTMF('$(CURDIR)/$<') COMMIT(*NONE) DFTRDBCOL(${BUILDLIB})"
	echo "Success"

uninstall:

	system "dltlib ${BUILDLIB}" || echo "could not delete library"



