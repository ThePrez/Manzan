
BUILDLIB?=MANZAN

.PHONY: mkdirs app.ini

mkdirs:
	install -m 700 -o qsys -D -d ${INSTALL_ROOT}/opt/manzan ${INSTALL_ROOT}/opt/manzan/bin ${INSTALL_ROOT}/opt/manzan/lib
	install -m 700 -o qsys -D -d ${INSTALL_ROOT}/QOpenSys/etc/manzan-default

app.ini: app.ini.tpl
	cat $< | /QOpenSys/usr/bin/sed  's|library=.*|library=${BUILDLIB}|g' > $@

# Always install the template files — the shell wrapper (camel/scripts/manzan)
# handles all migration logic on the target at first start, using
# /opt/manzan/lib/templates/ as the reference point for what is "unedited".
copyfiles: app.ini data.ini dests.ini
	install -m 600 -o qsys $^ ${INSTALL_ROOT}/QOpenSys/etc/manzan-default

install: mkdirs copyfiles
	chown -R qsys ${INSTALL_ROOT}/QOpenSys/etc/manzan-default

uninstall:
	rm -fr  ${INSTALL_ROOT}/opt/manzan ${INSTALL_ROOT}/QOpenSys/etc/manzan-default
	# Also remove the legacy single-instance config directory if it still exists.
	rm -fr  ${INSTALL_ROOT}/QOpenSys/etc/manzan

all: 
