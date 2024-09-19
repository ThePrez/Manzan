#!/bin/bash
# Deploy Manzan to IBM i, build the Java and ILE code, and run the E2E tests

source ./scripts/buildAll.sh

./scripts/testE2e.sh