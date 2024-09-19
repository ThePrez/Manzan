#!/bin/bash
# Deploy Manzan to IBM i, and build the ILE code

source ./scripts/deploy.sh

ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake ile" | tee camel.out