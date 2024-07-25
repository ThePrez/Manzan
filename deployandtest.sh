#!/bin/bash

source ./.env
rsync --exclude camel/target --exclude out -avz -e ssh . $USER@$HOST:~/mnzntest
./test.sh