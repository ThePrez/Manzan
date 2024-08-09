BUILDLIB?=MANZAN
BUILDVERSION:="Development build \(built with Make\)"

.PHONY: ile camel test

ile:
	gmake -C ile BUILDLIB=${BUILDLIB}

camel:
	gmake -C camel

test: install
	gmake -C test/e2e runtests

testonly:
	gmake -C test/e2e runtests

all: ile camel

install:
	gmake -C config install BUILDLIB=${BUILDLIB}
	gmake -C ile BUILDLIB=${BUILDLIB}
	gmake -C camel install

uninstall:
	gmake -C ile uninstall BUILDLIB=${BUILDLIB}
	gmake -C config uninstall  BUILDLIB=${BUILDLIB}

/QOpenSys/pkgs/bin/zip:
	/QOpenSys/pkgs/bin/yum install zip

/QOpenSys/pkgs/bin/wget:
	/QOpenSys/pkgs/bin/yum install wget

appinstall.jar: /QOpenSys/pkgs/bin/wget
	/QOpenSys/pkgs/bin/wget -O appinstall.jar https://github.com/ThePrez/AppInstall-IBMi/releases/download/v0.0.3/appinstall-v0.0.3.jar

manzan-installer-v%.jar: /QOpenSys/pkgs/bin/zip appinstall.jar
	echo "Building version $*"
	system "dltlib ${BUILDLIB}" || echo "could not delete"
	system "crtlib ${BUILDLIB}"
	system "dltlib ${BUILDLIB}"
	rm -fr /QOpenSys/etc/manzan
	gmake -C config BUILDVERSION="$*" install BUILDLIB=${BUILDLIB}
	gmake -C ile BUILDVERSION="$*" BUILDLIB=${BUILDLIB}
	gmake -C camel BUILDVERSION="$*" clean install
	/QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit/bin/java -jar appinstall.jar --qsys manzan --dir /QOpenSys/etc/manzan --file /opt/manzan -o $@
