TESTLIB:=MZNTEST
INPUT_MSGQ:=QCPFMSG
OUTPUT_FILE:=/tmp/mzntestoutput
MESSAGE_ID:=*IMMED
MESSAGE:=My snd2q message
MSGTYPE:=*INFO
SEVERITY:=80

/qsys.lib/${TESTLIB}.lib/msgs.msgq:
	system "CRTMSGQ ${TESTLIB}/MSGS TEXT('Manzan Testing')"

setup: /qsys.lib/${TESTLIB}.lib/msgs.msgq
	echo "\nstrwch=WCHMSG((*ALL)) WCHMSGQ((${TESTLIB}/MSGS))" >> data.ini
	rm -f ${OUTPUT_FILE}

cleanup:
	system -kKv "ENDWCH SSNID(TESTING)"
	system -kKv "DLTMSGQ MSGQ(${TESTLIB}/MSGS)"
	rm -f ${OUTPUT_FILE}

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) > /tmp/manzan.out 2>&1 &
	sleep 15
	system -kKv "SNDMSG TOMSGQ(${TESTLIB}/MSGS) MSG('${MESSAGE}') MSGTYPE(${MSGTYPE})"
	sleep 5
	cat /tmp/manzan.out

checkresult:
	cat ${OUTPUT_FILE}
	grep -i "${MESSAGE_ID} (severity ${SEVERITY}): ${MESSAGE}" ${OUTPUT_FILE}
