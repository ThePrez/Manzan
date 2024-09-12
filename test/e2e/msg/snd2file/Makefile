INPUT_FILE:=/tmp/mzntestinput
OUTPUT_FILE:=/tmp/mzntestoutput
TEXT:=My snd2file message

setup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}
	touch ${INPUT_FILE}

cleanup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) > /tmp/manzan.out 2>&1 &
	sleep 15
	echo ${TEXT} >> ${INPUT_FILE}
	sleep 5
	cat /tmp/manzan.out

checkresult:
	cat ${OUTPUT_FILE}
	grep -i "${TEXT}" ${OUTPUT_FILE}
