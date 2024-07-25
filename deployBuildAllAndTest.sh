#!/bin/bash

source ./.env
rsync --exclude camel/target --exclude out -avz -e ssh . $USER@$HOST:~/mnzntest
ssh $USER@$HOST "cd /home/$USER/mnzntest; gmake install" | tee install.out
./test.sh

