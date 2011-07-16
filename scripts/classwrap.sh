#!/bin/zsh

# Set up classpath and invoke 'java' with it

set -eu
root=$(dirname $0)/..

cp=""
# Eclipse and IDEA defaults
cp=$cp:$root/bin
cp=$cp:$(print $root/out/production/*/ | tr ' ' :)
# our build dir
cp=$cp:$root/mybuild

cp=$cp:$(print $root/lib/*.jar | tr ' ' :)
# Twitter Commons text library stuff
cp=$cp:$(print $root/lib_twitter/*.jar | tr ' ' :)

exec java -cp "$cp" "$@"

