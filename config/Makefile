
BUILDLIB:=MANZAN

.PHONY: mkdirs app.ini

mkdirs: 
	install -m 755 -o qsys -D -d ${INSTALL_ROOT}/opt/manzan ${INSTALL_ROOT}/opt/manzan/bin ${INSTALL_ROOT}/opt/manzan/lib ${INSTALL_ROOT}/QOpenSys/etc/manzan

app.ini: app.ini.tpl
	cat $< | /QOpenSys/usr/bin/sed  's|library=.*|library=${BUILDLIB}|g' > $@


copyfiles: app.ini data.ini dests.ini
	install -m 555 -o qsys $^ ${INSTALL_ROOT}/QOpenSys/etc/manzan

install: copyfiles
	/QOpenSys/usr/bin/find  ${INSTALL_ROOT}/QOpenSys/etc/manzan/ -type f -print -exec chmod 644 {} \;
	/QOpenSys/usr/bin/find  ${INSTALL_ROOT}/QOpenSys/etc/manzan/ -type l -print -exec chmod 644 {} \;
	chown -R qsys ${INSTALL_ROOT}/QOpenSys/etc/manzan

uninstall:
	rm -fr  ${INSTALL_ROOT}/opt/manzan ${INSTALL_ROOT}/QOpenSys/etc/manzan

all: 
