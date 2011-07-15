#!/bin/bash

# Cleans up compiled stuff from our build

set -eu
cd $(dirname $0)/..
echo "cleaning in $(pwd)"
set -x

# bin/ is eclipse, out/ is IDEA
rm -rf bin out mybuild
