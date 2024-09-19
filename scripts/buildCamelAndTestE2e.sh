#!/bin/bash
# Deploy Manzan to IBM i, build the Java code, and run the E2E tests

source ./scripts/buildCamel.sh

./scripts/testE2e.sh
