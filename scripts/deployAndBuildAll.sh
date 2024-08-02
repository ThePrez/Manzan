#!/bin/bash
# Deploy Manzan to IBM i, and build the java and ile code

./deploy.sh

ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake install" | tee install.out
