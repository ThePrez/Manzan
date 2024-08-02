#!/QOpenSys/pkgs/bin/bash
IFS=' ' read -r -a TESTS < <(/QOpenSys/pkgs/bin/find * -mindepth 1 -type d | tr '\n' ' ')
ARGS=("$@")

if [[ ${#ARGS[@]} -ne 0 ]]; then
  TESTS=("${ARGS[@]}")
fi

echo "tests ${TESTS[*]}"

num_fail=0
num_pass=0
num_error=0

failedTests=""
erroredTests=""
passedTests=""


for test in "${TESTS[@]}"
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
        erroredTests="$erroredTests$test"$'\n'"  "
    fi 

    echo "Done running test $test. Exit code was $?"

    echo "Killing jobs..."
    echo "Printing processes"

    # Get user string only up to 8th character since it gets truncated in process table
    ps -ef | grep jre | grep "${USER:0:8}"
    for pid in $(ps -ef | grep jre | grep "${USER:0:8}" | awk '{print $2}')
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
        passedTests="$passedTests$test"$'\n'"  "
    else
        ((num_fail+=1))
        failedTests="$failedTests$test"$'\n'"  "
    fi

    echo "Performing test-specific cleanup for test $test..."
    gmake -C $test cleanup || echo "no setup needed"
    echo "Performing cleanup..."
    gmake posttest
done

echo "=================================="
echo "Results:"
echo "  $num_pass passed"
echo "  $passedTests"
echo "  $num_fail failed"
echo "  $failedTests"
echo "  $num_error errored"
echo "  $erroredTests"

if [ $num_fail -ne 0 ] || [ $num_error -ne 0 ]; then
  echo "Tests failed."
  exit 1
else
  echo "All tests passed."
  exit 0
fi
