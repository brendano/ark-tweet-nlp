#!/bin/bash

# Set up classpath and invoke 'java' with it

set -eu
root=$(dirname $0)/..

# make sure cygwin doesn't use windows find
FIND=/usr/bin/find

cp=""
# Eclipse and IDEA defaults
cp=$cp:$root/bin
cp=$cp:$root/out/production/ark-tweet-nlp
# our build dir from "compile.sh"
cp=$cp:$root/mybuild
cp=$cp:$root/ark-tweet-nlp/target/classes
# Jar dependencies
cp=$cp:$($FIND $root/lib $root/ark-tweet-nlp/target -name '*.jar' | tr '\n' :)
case $OSTYPE in
cygwin*|msys)
	cp=$(echo $cp | tr ':' ';')
	;;
esac
exec java -ea -Xmx2g -cp "$cp" "$@"
