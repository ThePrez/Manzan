# Only run test, without building
source ./.env
ssh $USER@$HOST "cd /home/$USER/mnzntest/test; gmake runtests TESTS=\"${TESTS[@]}\"" | tee tests.out
