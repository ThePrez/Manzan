
.PHONY: ile testing runtests

ile:
	gmake -C ../ile


TESTS:=$(shell find * -type d)
TESTLIB:=MZNTEST

# run before every test case
pretest:
	echo "Doing pre-test setup"
	system "crtlib ${TESTLIB}" || system "clrlib ${TESTLIB}"

# Run after every test case
posttest:
	echo "Doing post-test cleanup"
	system -kKv "ENDWCH SSNID(TESTING)" || echo "watch not ended"
	system "clrlib ${TESTLIB}"

runtests:
	echo "Running tests"
	./runTests.sh
	ps -ef | grep manzan | awk '{ print $2 }' | xargs kill -9

testing: /qsys.lib/${BUILDLIB}.lib/MANZANQ.msgq  watch_start watch_testq watch_end


watch_end:
	system -kKv "ENDWCH SSNID(TESTING)"
	# select rtrim(HANDLED_TIMESTAMP) as TS, rtrim(SESSION_ID) as SESSION, rtrim(MESSAGE_ID) as MSG_ID from ${BUILDLIB}.manzanmsg where SESSION_ID = 'TESTING   '

all: ile runtests
