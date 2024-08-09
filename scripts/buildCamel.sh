#!/bin/bash
# Deploy Manzan to IBM i, and build the Java code

source ./scripts/deploy.sh

ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake camel" | tee camel.out