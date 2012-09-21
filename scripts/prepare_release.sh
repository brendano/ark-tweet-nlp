#!/bin/bash

VERSION=0.3
DIR=release/ark-tweet-nlp-$VERSION

set -eux

rm -rf $DIR $DIR.zip $DIR.tgz
mkdir -p $DIR

mvn clean
mvn package

cp ark-tweet-nlp/target/bin/ark-tweet-nlp-${VERSION}.jar $DIR

cp -r examples $DIR
cp -r scripts $DIR
cp -r docs $DIR
cp -r data $DIR
rm $DIR/scripts/prepare_release.sh
rm $DIR/scripts/java.sh
rm -f $DIR/**/.*un~
rm -f $DIR/**/.DS_Store
cp *.sh $DIR
cp {README,LICENSE}.txt $DIR

# these dont work, need to fix
# rm $DIR/examples/barackobama*

d=$(basename $DIR)
# (cd $(dirname $DIR) && zip -r $d.zip $d)
(cd $(dirname $DIR) && tar czf $d.tgz $d)

