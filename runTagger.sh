#!/bin/bash

# For development
# java -Xmx1g -jar $(dirname $0)/ark-tweet-nlp/target/bin/ark-tweet-nlp-0.3-SNAPSHOT.jar "$@"

# For release
java -Xmx1g -jar $(dirname $0)/ark-tweet-nlp-0.3.jar "$@"
