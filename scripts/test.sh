#!/bin/bash
# Only run test, without building

scripts/deploy.sh

ssh $USER@$HOST "cd ~/mnzntest/test; gmake runtests TESTS=\"${TESTS[@]}\"" | tee tests.out