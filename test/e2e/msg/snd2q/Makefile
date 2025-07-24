TESTLIB:=MZNTEST
INPUT_MSGQ:=QCPFMSG
OUTPUT_FILE:=/tmp/mzntestoutput
MESSAGE_ID:=*IMMED
MESSAGE:=My snd2q message\"-_=+~%^*
ENCODED_MESSAGE:=My snd2q message"-_=+~%^*
MSGTYPE:=*INFO
SEVERITY:=80

/qsys.lib/${TESTLIB}.lib/msgs.msgq:
	system "CRTMSGQ ${TESTLIB}/MSGS TEXT('Manzan Testing')"
	system "CRTMSGQ ${TESTLIB}/MSGSUNI TEXT('Manzan Testing') CCSID(1208)"
	system "CRTMSGQ ${TESTLIB}/MSGSUSA TEXT('Manzan Testing') CCSID(37)"
	system "CRTMSGQ ${TESTLIB}/MSGSGER TEXT('Manzan Testing') CCSID(273)"
	system "CRTMSGQ ${TESTLIB}/MSGSHEB TEXT('Manzan Testing') CCSID(424)"


setup: /qsys.lib/${TESTLIB}.lib/msgs.msgq
	sed -i 's|TESTLIB|${TESTLIB}|g' data.ini
	rm -f ${OUTPUT_FILE}

cleanup:
	system -kKv "ENDWCH SSNID(TEST)"
	system -kKv "ENDWCH SSNID(TEST_UNI)"
	system -kKv "ENDWCH SSNID(TEST_USA)"
	system -kKv "ENDWCH SSNID(TEST_GER)"
	system -kKv "ENDWCH SSNID(TEST_HEB)"

	system -kKv "DLTMSGQ MSGQ(${TESTLIB}/MSGS)"
	system -kKv "DLTMSGQ MSGQ(${TESTLIB}/MSGSUNI)"
	system -kKv "DLTMSGQ MSGQ(${TESTLIB}/MSGSUSA)"
	system -kKv "DLTMSGQ MSGQ(${TESTLIB}/MSGSGER)"
	system -kKv "DLTMSGQ MSGQ(${TESTLIB}/MSGSHEB)"

	rm -f ${OUTPUT_FILE}

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) > /tmp/manzan.out 2>&1 &
	sleep 15
	system -kKv "SNDMSG TOMSGQ(${TESTLIB}/MSGS) MSG('${MESSAGE}1') MSGTYPE(${MSGTYPE})"
	system -kKv "SNDMSG TOMSGQ(${TESTLIB}/MSGSUNI) MSG('${MESSAGE}2') MSGTYPE(${MSGTYPE})"
	system -kKv "SNDMSG TOMSGQ(${TESTLIB}/MSGSUSA) MSG('${MESSAGE}3') MSGTYPE(${MSGTYPE})"
	system -kKv "SNDMSG TOMSGQ(${TESTLIB}/MSGSGER) MSG('${MESSAGE}4') MSGTYPE(${MSGTYPE})"
	system -kKv "SNDMSG TOMSGQ(${TESTLIB}/MSGSHEB) MSG('${MESSAGE}5') MSGTYPE(${MSGTYPE})"

	sleep 5
	cat /tmp/manzan.out

checkresult:
	cat ${OUTPUT_FILE}
	grep -Fi '${MESSAGE_ID} (severity ${SEVERITY}): ${ENCODED_MESSAGE}1' ${OUTPUT_FILE}
	grep -Fi '${MESSAGE_ID} (severity ${SEVERITY}): ${ENCODED_MESSAGE}2' ${OUTPUT_FILE}
	grep -Fi '${MESSAGE_ID} (severity ${SEVERITY}): ${ENCODED_MESSAGE}3' ${OUTPUT_FILE}
	grep -Fi '${MESSAGE_ID} (severity ${SEVERITY}): ${ENCODED_MESSAGE}4' ${OUTPUT_FILE}
	grep -Fi '${MESSAGE_ID} (severity ${SEVERITY}): ${ENCODED_MESSAGE}5' ${OUTPUT_FILE}

