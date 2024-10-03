INPUT_FILE:=/tmp/mzntestinput
OUTPUT_FILE:=/tmp/manzan.out
TEXT="Test content"
EXPECTED_OUTPUT=DEBUG SMTP: message successfully delivered to mail server

setup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}
	touch ${INPUT_FILE} ${OUTPUT_FILE}
	cd ../../../smtp-server; npm install; cd ../e2e/msg/snd2email
	ps -ef | grep 'smtp-server/server.js' | grep -v 'grep' | awk '{ print $$2 }' | xargs kill

cleanup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}

run:
	nohup node ../../../smtp-server/server.js &
	/opt/manzan/bin/manzan --configdir=$(CURDIR) > ${OUTPUT_FILE} 2>&1 &
	sleep 15
	echo ${TEXT} >> ${INPUT_FILE}
	sleep 15
	ps -ef | grep 'smtp-server/server.js' | grep -v 'grep' | awk '{ print $$2 }' | xargs kill

checkresult:
	echo "Expected output: '${EXPECTED_OUTPUT}'"
	grep -i '${EXPECTED_OUTPUT}' ${OUTPUT_FILE}
	cat ${OUTPUT_FILE}
