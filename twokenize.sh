#!/bin/bash

# Only run the tokenizer.

set -eu
java -XX:ParallelGCThreads=2 -Xmx100m -jar $(dirname $0)/ark-tweet-nlp-0.3.2.jar --just-tokenize "$@"
