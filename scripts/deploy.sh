#!/bin/bash
# Deploy Manzan to IBM i

source ./checkEnv.sh

rsync --exclude camel/target --exclude out -avz -e ssh . $USER@$HOST:~/mnzntest