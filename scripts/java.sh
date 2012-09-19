#!/bin/bash

# Invoke 'java' for development.
# Set up the classpath, with all dependency jars, then run 'java' with it

set -eu
root=$(dirname $0)/..

# make sure cygwin doesn't use windows find
FIND=/usr/bin/find

cp=""
# Eclipse and IntelliJ defaults
cp=$cp:$root/bin
cp=$cp:$root/out/production/ark-tweet-nlp

# Our build dir from "compile.sh"
cp=$cp:$root/mybuild
# Maven compiles into here
cp=$cp:$root/ark-tweet-nlp/target/classes

# Resources
cp=$cp:$root/ark-tweet-nlp/src/main/resources

# Jar dependencies:
#  - lib/ are the ones we include
#  - ark-tweet-nlp/target are ones that Maven copies in.
# ... warning, if "mvn package" was executed, then the fully built jar will get
# on the classpath, but the Eclipse versions should have higher priority above
cp=$cp:$($FIND $root/lib $root/ark-tweet-nlp/target/bin -name '*.jar' | tr '\n' :)

# Change to semicolons for cygwin/windows
case $OSTYPE in
cygwin*|msys)
	cp=$(echo $cp | tr ':' ';')
	;;
esac

# set -eux
exec java -ea -Xmx2g -cp "$cp" "$@"
