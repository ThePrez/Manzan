#!/bin/bash
# Deploy Manzan to IBM i

source ./.env

if [ -z "$USER" ]; then
  echo "USER is not set."
  exit 1
fi

if [ -z "$HOST" ]; then
  echo "HOST is not set."
  exit 1
fi

rsync --exclude camel/target --exclude out -avz -e ssh . $USER@$HOST:~/mnzntest