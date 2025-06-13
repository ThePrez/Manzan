

BUILDVERSION:="Development build \(built with Make\)"

.PHONY: mkdirs

JAVA_SRCS := $(shell find src -type f)
target/manzan.jar: ${JAVA_SRCS} /QOpenSys/pkgs/bin/mvn
	JAVA_HOME=/QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit /QOpenSys/pkgs/bin/mvn -Djava.net.preferIPv4Stack=true -Dmanzan.version=${BUILDVERSION} package
	cp target/manzan-*-with-dependencies.jar target/manzan.jar

mkdirs:
	gmake -C ../config mkdirs

/QOpenSys/pkgs/bin/mvn:
	/QOpenSys/pkgs/bin/yum install maven

install: mkdirs all scripts/manzan
	install -m 700 -o qsys target/manzan.jar ${INSTALL_ROOT}/opt/manzan/lib/manzan.jar
	install -m 700 -o qsys scripts/manzan ${INSTALL_ROOT}/opt/manzan/bin/manzan

${INSTALL_ROOT}/opt/manzan/bin/manzan:

clean:
	rm -fr target

all: target/manzan.jar