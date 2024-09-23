INPUT_FILE:=/tmp/mzntestinput
OUTPUT_FILE:=/tmp/manzan.out
TEXT="Test content"
EXPECTED_OUTPUT=DEBUG SMTP: message successfully delivered to mail server

setup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}
	touch ${INPUT_FILE}

cleanup:
	# rm -f ${INPUT_FILE} ${OUTPUT_FILE}

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) > ${OUTPUT_FILE} 2>&1 &
	sleep 15
	echo ${TEXT} >> ${INPUT_FILE}
	sleep 10

checkresult:
	echo "Expected output: '${EXPECTED_OUTPUT}'"
	grep -i '${EXPECTED_OUTPUT}' ${OUTPUT_FILE}
	cat ${OUTPUT_FILE}
