#!/bin/bash
# Check the USER and HOST are set in the .env file

source ./.env

if [ -z "$USER" ]; then
  echo "USER is not set."
  exit 1
fi

if [ -z "$HOST" ]; then
  echo "HOST is not set."
  exit 1
fi