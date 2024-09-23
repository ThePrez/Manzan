#!/QOpenSys/pkgs/bin/bash
IFS=' ' read -r -a TESTS < <(/QOpenSys/pkgs/bin/find * -mindepth 1 -type d | tr '\n' ' ')
ARGS=("$@")

echo "Running tests"
if [[ ${#ARGS[@]} -ne 0 ]]; then
  TESTS=("${ARGS[@]}")
fi
echo "Test Suite: ${TESTS[*]}"

num_fail=0
num_pass=0
num_error=0

exit_code=0

failedTests=""
erroredTests=""
passedTests=""

# Function to run a command with tabbed output and get the exit code
run_command_with_output() {
  local command="$1"
  local exit_code=0

  stdbuf -oL $command | sed 's/^/\t\t/'
  exit_code=${PIPESTATUS[0]}

  return "$exit_code"
}

for test in "${TESTS[@]}"
do
  echo "Test: $test"
  
  # General and test specific setup
	echo -e "\tRunning pre-test setup..."
  gmake pretest 2>&1 | sed 's/^/\t\t/'
  echo -e "\tRunning test-specific setup..."
  gmake -C $test setup 2>&1 | sed 's/^/\t\t/' || exit 1
  
  # Execute tests
  echo -e "\tRunning test..."
  run_command_with_output "gmake -C $test run"
  exit_code=$?
  if [[ "0" != "$exit_code" ]]
  then
    ((num_error+=1))
    erroredTests="$erroredTests$test"$'\n'"  "
  fi  
  echo -e "\t\tExit code for test was $exit_code"
  
  # Kill processes
  echo -e "\tKilling processes..."
  ps -ef | grep jre | grep "${USER:0:8}" 2>&1 | sed 's/^/\t\t/' # Get user string only up to 8th character since it gets truncated in process table
  for pid in $(ps -ef | grep jre | grep "${USER:0:8}" | awk '{print $2}')
  do
    echo -e "\t\tkilling pid $pid"
    kill -INT $pid 2>&1 | sed 's/^/\t\t/'
    sleep 1
    kill -KILL $pid 2>&1 | sed 's/^/\t\t/'
  done

  # Check test result
  echo -e "\tChecking result...."
  run_command_with_output "gmake -C $test checkresult"
  result="$?"
  echo -e "\t\tTest $test result is $result"
  if [[ "0" == "$result" ]]
  then
    ((num_pass+=1))
    passedTests="$passedTests$test"$'\n'"  "
  else
    ((num_fail+=1))
    failedTests="$failedTests$test"$'\n'"  "
  fi
  
  # Cleanup
  echo -e "\tRunning test-specific cleanup..."
  gmake -C $test cleanup 2>&1 | sed 's/^/\t\t/' || echo -e "\t\tNo test-specific cleanup needed"
	echo -e "\tRunning post-test cleanup"
  gmake posttest 2>&1 | sed 's/^/\t\t/'
done

# Output results
echo "=================================="
echo "Results"
echo "  $num_pass passed"
echo "  $passedTests"
echo "  $num_fail failed"
echo "  $failedTests"
echo "  $num_error errored"
echo "  $erroredTests"
if [ $num_fail -ne 0 ] || [ $num_error -ne 0 ]; then
  echo "Tests failed."
  exit_code=1
else
  echo "All tests passed."
fi

echo "=================================="
exit $exit_code