# Only run test, without building
source ./.env

if [ -z "$USER" ]; then
  echo "USER is not set."
  exit 1
fi

if [ -z "$HOST" ]; then
  echo "HOST is not set."
  exit 1
fi

ssh $USER@$HOST "cd ~/mnzntest/test; gmake runtests TESTS=\"${TESTS[@]}\"" | tee tests.out
