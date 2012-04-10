#!/bin/bash

java -Xmx1g -jar $(dirname $0)/ark-tweet-nlp/target/bin/ark-1.0-SNAPSHOT.jar "$@"
