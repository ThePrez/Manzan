BUILDLIB := "MANZAN"

/qsys.lib/${BUILDLIB}.lib/handler.pgm: /qsys.lib/${BUILDLIB}.lib/handler.module /qsys.lib/${BUILDLIB}.lib/pub_json.module /qsys.lib/${BUILDLIB}.lib/debug.module
	system "CRTPGM PGM(${BUILDLIB}/HANDLER) MODULE(${BUILDLIB}/HANDLER ${BUILDLIB}/PUB_JSON ${BUILDLIB}/DEBUG) ACTGRP(*CALLER)""

/qsys.lib/${BUILDLIB}.lib/%.module: src/%.cpp
	system "CRTCPPMOD MODULE(${BUILDLIB}/$*) SRCSTMF('$^') OPTION(*EVENTF) SYSIFCOPT(*IFS64IO) DBGVIEW(*SOURCE) TERASPACE(*YES *TSIFC) STGMDL(*SNGLVL) DTAMDL(*p128) DEFINE(DEBUG_ENABLED)"

all: /qsys.lib/${BUILDLIB}.lib/handler.pgm