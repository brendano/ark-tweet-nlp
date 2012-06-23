#!/bin/bash

# Set up classpath and invoke 'java' with it

set -eu
root=$(dirname $0)/..

cp=""
# Eclipse and IDEA defaults
cp=$cp:$root/bin
cp=$cp:$root/out/production/ark-tweet-nlp
# our build dir
cp=$cp:$root/mybuild

cp=$cp:$(echo $root/lib/*.jar $root/lib/*/*.jar | tr ' ' :)
# Twitter Commons text library stuff
cp=$cp:$(echo $root/lib_twitter/*.jar | tr ' ' :)

exec java -ea -cp "$cp" "$@"

