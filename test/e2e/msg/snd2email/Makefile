INPUT_FILE:=/tmp/mzntestinput
OUTPUT_FILE:=/tmp/manzan.out
TEXT="Test content"
EXPECTED_OUTPUT=DEBUG SMTP: message successfully delivered to mail server

setup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}
	touch ${INPUT_FILE} ${OUTPUT_FILE}

cleanup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}

run:
	nohup node /home/GITHUBADM/smtp-server/server.js &
	/opt/manzan/bin/manzan --configdir=$(CURDIR) > ${OUTPUT_FILE} 2>&1 &
	sleep 10
	echo ${TEXT} >> ${INPUT_FILE}
	sleep 10
	ps -ef | grep 'node /home/GITHUBADM/smtp-server/server.js' | grep -v 'grep' | awk '{ print $$2 }' | xargs kill

checkresult:
	echo "Expected output: '${EXPECTED_OUTPUT}'"
	grep -i '${EXPECTED_OUTPUT}' ${OUTPUT_FILE}
	cat ${OUTPUT_FILE}
