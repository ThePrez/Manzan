TESTLIB:=MZNTEST

/qsys.lib/${TESTLIB}.lib/msgs.msgq:
	system "CRTMSGQ ${TESTLIB}/MSGS"


sndmsg.class: sndmsg.java
	/QOpenSys/QIBM/ProdData/JavaVM/jdk80/64bit/bin/javac -cp /QIBM/ProdData/OS400/jt400/lib/jt400.jar sndmsg.java

setup:
	rm -f $(CURDIR)/test_input.txt
	touch $(CURDIR)/test_input.txt

cleanup:
	rm sndmsg.class
	rm -f $(CURDIR)/test.out

run:
	/opt/manzan/bin/manzan --configdir=$(CURDIR) &
	sleep 12
	echo "ayooooooo" $(CURDIR)/test_input.txt
	sleep 5

checkresult:
	grep -i CAE0023 $(CURDIR)/test.out
