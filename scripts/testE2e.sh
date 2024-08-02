#!/bin/bash
# Only run E2E tests, without building

source ./scripts/deploy.sh

ssh $USER@$HOST "cd ~/mnzntest/test/e2e; gmake runtests TESTS=\"${TESTS[@]}\"" | tee tests.out