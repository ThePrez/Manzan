
.PHONY: ile testing runtests

ile:
	gmake -C ../ile


TESTS:=$(shell find * -mindepth 1 -type d)
TESTLIB:=MZNTEST

# run before every test case
pretest:
	system "crtlib ${TESTLIB}" || system "clrlib ${TESTLIB}"

# Run after every test case
posttest:
	system "clrlib ${TESTLIB}"

runtests:
	./runTests.sh $(TESTS)

all: ile runtests
