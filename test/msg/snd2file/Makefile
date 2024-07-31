TESTLIB:=MZNTEST
INPUT_FILE:=/tmp/mzntestinput
OUTPUT_FILE:=/tmp/mzntestoutput
TEXT:=ayooooooo


setup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}
	touch ${INPUT_FILE}

cleanup:
	rm -f ${INPUT_FILE} ${OUTPUT_FILE}

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) &
	sleep 12
	echo ${TEXT} >> ${INPUT_FILE}
	sleep 5

checkresult:
	grep -i ${TEXT} ${OUTPUT_FILE}
