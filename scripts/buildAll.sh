#!/bin/bash
# Deploy Manzan to IBM i, and build the Java and ILE code

source ./scripts/deploy.sh

ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake install" | tee install.out
