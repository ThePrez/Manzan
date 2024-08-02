#!/bin/bash
# Only run ILE tests, without building

bazel test --cxxopt=-std=c++14 --verbose_failures --test_output=all //:test_util