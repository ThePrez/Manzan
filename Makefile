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
	install -m 600 -o qsys service-commander-def.yaml ${INSTALL_ROOT}/opt/manzan/lib/manzan.yaml

uninstall:
	gmake -C ile uninstall BUILDLIB=${BUILDLIB}
	gmake -C config uninstall  BUILDLIB=${BUILDLIB}

/QOpenSys/pkgs/bin/zip:
	/QOpenSys/pkgs/bin/yum install zip

/QOpenSys/pkgs/bin/wget:
	/QOpenSys/pkgs/bin/yum install wget

appinstall.jar: /QOpenSys/pkgs/bin/wget
	/QOpenSys/pkgs/bin/wget -O appinstall.jar https://github.com/ThePrez/AppInstall-IBMi/releases/download/v0.0.6/appinstall-v0.0.6.jar

manzan-installer-v%.jar: /QOpenSys/pkgs/bin/zip appinstall.jar
	echo "Building version $*"
	system "dltlib ${BUILDLIB}" || echo "could not delete"
	system "crtlib ${BUILDLIB}"
	system "dltlib ${BUILDLIB}"
	: > config/app.ini
	rm -fr /QOpenSys/etc/manzan
	rm -fr /opt/manzan
	gmake -C config BUILDVERSION="$*" install BUILDLIB=${BUILDLIB}
	gmake -C ile BUILDVERSION="$*" BUILDLIB=${BUILDLIB}
	gmake -C camel BUILDVERSION="$*" clean install
	install -m 600 -o qsys service-commander-def.yaml ${INSTALL_ROOT}/opt/manzan/lib/manzan.yaml
	mkdir -p ${INSTALL_ROOT}/QOpenSys/etc/sc/services
	ln -sf /opt/manzan/lib/manzan.yaml ${INSTALL_ROOT}/QOpenSys/etc/sc/services/manzan.yaml
	mkdir -p ${INSTALL_ROOT}/opt/manzan/.install-marker
	$(eval BUILD_TS := $(shell date +%Y%m%d-%H%M%S))
	echo "Manzan v$* build $(BUILD_TS) - workaround for AppInstall bug" > ${INSTALL_ROOT}/opt/manzan/.install-marker/.build-$(BUILD_TS)
	/QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit/bin/java -jar appinstall.jar -o $@ --qsys manzan --file /opt/manzan --file /QOpenSys/etc/sc/services/manzan.yaml --fileIfMissing /QOpenSys/etc/manzan/app.ini --fileIfMissing /QOpenSys/etc/manzan/data.ini --fileIfMissing /QOpenSys/etc/manzan/dests.ini --fileIfMissing /opt/manzan/.install-marker/.build-$(BUILD_TS)
	