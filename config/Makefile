
BUILDLIB?=MANZAN

.PHONY: mkdirs app.ini

mkdirs: 
	install -m 700 -o qsys -D -d ${INSTALL_ROOT}/opt/manzan ${INSTALL_ROOT}/opt/manzan/bin ${INSTALL_ROOT}/opt/manzan/lib ${INSTALL_ROOT}/QOpenSys/etc/manzan

app.ini: app.ini.tpl
	cat $< | /QOpenSys/usr/bin/sed  's|library=.*|library=${BUILDLIB}|g' > $@


copyfiles: app.ini $(CURDIR)/data.ini dests.ini
	install -m 555 -o qsys $^ ${INSTALL_ROOT}/QOpenSys/etc/manzan

install: mkdirs copyfiles
	/QOpenSys/usr/bin/find  ${INSTALL_ROOT}/QOpenSys/etc/manzan/ -type f -print -exec chmod 600 {} \;
	/QOpenSys/usr/bin/find  ${INSTALL_ROOT}/QOpenSys/etc/manzan/ -type l -print -exec chmod 600 {} \;
	chown -R qsys ${INSTALL_ROOT}/QOpenSys/etc/manzan

uninstall:
	rm -fr  ${INSTALL_ROOT}/opt/manzan ${INSTALL_ROOT}/QOpenSys/etc/manzan

all: 
