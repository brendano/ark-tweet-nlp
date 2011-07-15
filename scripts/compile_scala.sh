#!/bin/zsh

# (zsh is used to get recursive globbing)

cd $(dirname $0)/..

set -eux

mkdir -p mybuild

java -Dscala.usejavacp=true -Xmx1g -cp lib_build/scala-compiler-2.9.0.1.jar:lib/scala-library-2.9.0.1.jar scala.tools.nsc.Main -d mybuild src/**/*.scala

