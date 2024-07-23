TESTLIB:=MZNTEST

/qsys.lib/${TESTLIB}.lib/msgs.msgq:
	system "CRTMSGQ ${TESTLIB}/MSGS"


sndmsg.class: sndmsg.java
	/QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit/bin/javac -cp /QIBM/ProdData/OS400/jt400/lib/jt400.jar sndmsg.java

setup: /qsys.lib/${TESTLIB}.lib/msgs.msgq sndmsg.class
	rm -f $(CURDIR)/test.out
	touch $(CURDIR)/test.out
	echo "strwch=WCHMSG((*ALL)) WCHMSGQ((${TESTLIB}/MSGS))" >> data.ini
	echo "file=$(CURDIR)/test.out" >> dests.ini
	# echo "[install]" > app.ini
	# echo "library=${TESTLIB}" >> app.ini

cleanup:
	rm sndmsg.class
	rm -f $(CURDIR)/test.out

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) &
	ps -ef | grep manzan | awk '{ print $2 }' | xargs kill -9
	sleep 12
	/QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit/bin/java -cp $(CURDIR):/QIBM/ProdData/OS400/jt400/lib/jt400.jar sndmsg ${TESTLIB} msgs
	sleep 5

checkresult:
    echo "contents of file"
    cat $(CURDIR)/test.out
	grep -i CAE0023 $(CURDIR)/test.out
