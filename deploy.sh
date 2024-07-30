#!/bin/bash
# Deploy Manzan to IBM i

source ./.env
rsync --exclude camel/target --exclude out -avz -e ssh . $USER@$HOST:~/mnzntest