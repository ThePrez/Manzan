
.PHONY: ile testing runtests

ile:
	gmake -C ../ile


TESTS:=$(shell find * -mindepth 1 -type d)
TESTLIB:=MZNTEST

# run before every test case
beforeAll:
	for dir in msg/*/; do printf "[install]\nlibrary=%s\n" "${TESTLIB}" > "$$dir/app.ini"; done

# Run after every test case
afterAll:
	system "clrlib ${TESTLIB}"

runtests:
	./runTests.sh $(TESTS)

all: ile runtests
