
.PHONY: ile testing runtests

ile:
	gmake -C ../ile


TESTS:=$(shell find * -mindepth 1 -type d)
TESTLIB:=MZNTEST

# run before every test case
beforeAll:
	for dir in msg/*/; do \
		printf "[install]\nlibrary=%s\n" "${TESTLIB}" > "$$dir/app.ini"; \
		chmod 600 "$$dir/app.ini"; \
	done
	# Kill any leftover Manzan/Java processes from previous runs before starting
	-for pid in $$(ps -ef | grep -E 'jre|jdk' | grep "$${USER:0:8}" | awk '{print $$2}'); do \
		echo "Cleaning up leftover process: $$pid"; \
		kill -KILL $$pid 2>/dev/null || true; \
	done
	# Remove any stale lock files
	-rm -f /var/run/manzan/manzan-default.lock 2>/dev/null || true


# Run after every test case
afterAll:
	system "clrlib ${TESTLIB}"
	# Kill any remaining Manzan/Java processes
	-for pid in $$(ps -ef | grep -E 'jre|jdk' | grep "$${USER:0:8}" | awk '{print $$2}'); do \
		echo "Cleaning up process: $$pid"; \
		kill -KILL $$pid 2>/dev/null || true; \
	done
	# Remove lock files
	-rm -f /var/run/manzan/manzan-default.lock 2>/dev/null || true

runtests:
	./runTests.sh $(TESTS)

all: ile runtests
