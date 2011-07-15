#!/bin/zsh

# Our own quick-and-dirty build system
# If you have the eclipse or idea scala plug-ins,
# you might not even need this

# (zsh is used to get recursive globbing)

cd $(dirname $0)/..
echo "building from $(pwd) to $(pwd)/mybuild"

set -eux

rm -rf mybuild
mkdir -p mybuild

scripts/compile_scala.sh

javac -cp mybuild:$(echo lib/*.jar|tr ' ' :) -d mybuild src/**/*.java

set +x
echo "All the .class files now in $(pwd)/mybuild"
