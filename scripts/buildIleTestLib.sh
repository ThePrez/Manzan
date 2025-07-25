#!/bin/bash
# Deploy Manzan to IBM i, and build the ILE code

source ./scripts/deploy.sh
BUILDLIB=MZNTEST
ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake ile BUILDLIB=${BUILDLIB}" | tee camel.out