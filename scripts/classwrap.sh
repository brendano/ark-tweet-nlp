#!/bin/zsh

# Set up classpath and invoke 'java' with it

set -eu
root=$(dirname $0)/..
cp=$root/bin                 # Eclipse
cp=$cp:$(print $root/out/production/*/ | tr ' ' :)   # IDEA
cp=$cp:$root/mybuild         # Our own build dir

cp=$cp:$(print $root/lib/*.jar | tr ' ' :)
cp=$cp:$(print $root/lib_twitter/*.jar | tr ' ' :)

# set -x
exec java -cp "$cp" "$@"

