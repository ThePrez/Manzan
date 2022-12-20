
.PHONY: ile camel test

ile:
	gmake -C ile

camel:
	gmake -C camel

test:
	gmake -C test

all: ile camel

install:
	gmake -C config install
	gmake -C ile
	gmake -C camel install

uninstall:
	gmake -C ile uninstall
	gmake -C config uninstall
