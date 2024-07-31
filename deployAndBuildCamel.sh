#!/bin/bash
# Deploy Manzan to IBM i, and build the java code

source ./.env
./deploy.sh
ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake camel" | tee camel.out
