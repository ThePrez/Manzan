BUILDLIB:=MANZAN
MANZAN_TEMPLIB:=MANZANBLD
BUILDVERSION:="Development build \(built with Make\)"

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

manzan-v%.zip: /QOpenSys/pkgs/bin/zip
	echo "Building version $*"
	system "clrlib ${MANZAN_TEMPLIB}" || system "crtlib ${MANZAN_TEMPLIB}"
	system "dltlib ${BUILDLIB}" || echo "could not delete"
	system "crtlib ${BUILDLIB}"
	system "dltlib ${BUILDLIB}"
	rm -fr /QOpenSys/etc/manzan
	gmake -C config BUILDVERSION="$*" install
	gmake -C ile BUILDVERSION="$*"
	gmake -C camel BUILDVERSION="$*" clean install
	system "crtsavf ${MANZAN_TEMPLIB}/distqsys"
	system "crtsavf ${MANZAN_TEMPLIB}/diststmf"
	rm -f /qsys.lib/${MANZAN_TEMPLIB}.lib/*.MODULE
	system "SAV DEV('/qsys.lib/${MANZAN_TEMPLIB}.lib/distqsys.file') OBJ(('/qsys.lib/manzan.lib')) SAVACT(*YES)"
	cp /qsys.lib/${MANZAN_TEMPLIB}.lib/distqsys.file .
	system "SAV DEV('/qsys.lib/${MANZAN_TEMPLIB}.lib/diststmf.file') OBJ(('/opt/manzan') ('/QOpenSys/etc/manzan')) SAVACT(*YES)"
	cp /qsys.lib/${MANZAN_TEMPLIB}.lib/diststmf.file .
	/QOpenSys/pkgs/bin/zip -v -0 $@ diststmf.file distqsys.file
	rm diststmf.file distqsys.file
	system "dltlib ${MANZAN_TEMPLIB}"
