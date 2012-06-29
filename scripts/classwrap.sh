#!/bin/bash

# Set up classpath and invoke 'java' with it

set -eu
root=$(dirname $0)/..

cp=""
# Eclipse and IDEA defaults
cp=$cp:$root/bin
cp=$cp:$root/out/production/ark-tweet-nlp
# our build dir from "compile.sh"
cp=$cp:$root/mybuild

# Jar dependencies
cp=$cp:$(find $root/lib $root/ark-tweet-nlp/target -name '*.jar' | tr '\n' :)

exec java -ea -cp "$cp" "$@"

