#!/bin/bash

VERSION=0.3
DIR=ark-tweet-nlp-$VERSION

set -eux

rm -rf $DIR
mkdir $DIR

# mvn clean
# mvn package
cp ark-tweet-nlp/target/bin/ark-tweet-nlp-${VERSION}.jar $DIR

cp -r examples $DIR
cp -r scripts $DIR
rm $DIR/scripts/prepare_release.sh
rm $DIR/scripts/java.sh
cp *.sh $DIR
cp *.txt $DIR

# these dont work, need to fix
rm $DIR/examples/barackobama*
