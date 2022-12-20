
.PHONY: ile testing

ile:
	gmake -C ../ile

testing: /qsys.lib/${BUILDLIB}.lib/MANZANQ.msgq  watch_start watch_testq watch_end

sndmsg.class: sndmsg.java
	javac -cp /QIBM/ProdData/OS400/jt400/lib/jt400.jar ndmsg.java

watch_testq: ./tester/sndmsg.class
	java -cp ./tester:/QIBM/ProdData/OS400/jt400/lib/jt400.jar sndmsg

watch_start:
	# Listens to chosen message queue for all messages
	# then calls the handler program
	# Check /tmp/manzan_debug.txt for logs
	system -kKv "STRWCH SSNID(TESTING) WCHPGM(${BUILDLIB}/HANDLER) CALLWCHPGM(*STRWCH) WCHMSG((*ALL)) WCHMSGQ((${BUILDLIB}/MANZANQ))"

watch_end:
	system -kKv "ENDWCH SSNID(TESTING)"
	# select rtrim(HANDLED_TIMESTAMP) as TS, rtrim(SESSION_ID) as SESSION, rtrim(MESSAGE_ID) as MSG_ID from ${BUILDLIB}.manzanmsg where SESSION_ID = 'TESTING   '

all: ile testing