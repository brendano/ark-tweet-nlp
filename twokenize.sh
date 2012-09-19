#!/bin/bash

# Only run the tokenizer.

set -eu
java -Xmx100m -jar $(dirname $0)/ark-tweet-nlp-0.3.jar --just-tokenize "$@"
