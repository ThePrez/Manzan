#!/QOpenSys/pkgs/bin/bash

TESTS=$(/QOpenSys/pkgs/bin/find * -mindepth 1 -type d)

num_fail=0
num_pass=0
num_error=0

for test in $TESTS
do
    echo "Running test $test..."
    gmake pretest
    echo "Performing test-specific setup for test $test..."
    gmake -C $test setup || exit 1
    echo "Running test $test..."
    gmake -C $test run
    if [[ "0" != "$?" ]]
    then 
        ((num_error+=1))
    fi 

    echo "Done running test $test. Exit code was $?"

    echo "Killing jobs..."
    for pid in $(ps | grep jre| awk '{print $1}')
    do
        echo killing pid $pid
        kill -INT $pid
        sleep 1
        kill -KILL $pid
    done
	echo "checking result...."
	echo "=================================="
	gmake -C $test checkresult
	  result="$?"
    echo "Test $test result is $result"
    if [[ "0" == "$result" ]]
    then 
        ((num_pass+=1))
    else
        ((num_fail+=1))
    fi 
	echo "=================================="
	echo "output"
	echo "=================================="
	cat $test/test.out
	echo "=================================="

    echo "Performing test-specific cleanup for test $test..."
    gmake -C $test cleanup || echo "no setup needed"
    echo "Performing cleanup..."
    gmake posttest
	echo "=================================="
    echo "Results:"
    echo "  $num_pass passed"
    echo "  $num_fail failed"
    echo "  $num_error errored"
done

if [ $num_fail -ne 0 ] || [ $num_error -ne 0 ]; then
  echo "Tests failed."
  exit 1
else
  echo "All tests passed."
  exit 0
fi