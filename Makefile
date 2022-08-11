BUILDLIB := "MANZAN"

/qsys.lib/${BUILDLIB}.lib/handler.pgm: /qsys.lib/${BUILDLIB}.lib/handler.module
	system "CRTPGM PGM(${BUILDLIB}/HANDLER) MODULE(${BUILDLIB}/HANDLER) ACTGRP(*CALLER)""

/qsys.lib/${BUILDLIB}.lib/handler.module: src/handler.cpp
	system "CRTCPPMOD MODULE(${BUILDLIB}/HANDLER) SRCSTMF('src/handler.cpp') OPTION(*EVENTF) SYSIFCOPT(*IFS64IO) DBGVIEW(*SOURCE) TERASPACE(*YES *TSIFC) STGMDL(*SNGLVL) DTAMDL(*p128) DEFINE(DEBUG_ENABLED)"

all: /qsys.lib/${BUILDLIB}.lib/handler.pgm