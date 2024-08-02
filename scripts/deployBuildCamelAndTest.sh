#!/bin/bash
# Deploy Manzan to IBM i, build the java code and run the tests

./deployAndBuildCamel.sh

./test.sh
