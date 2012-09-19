#!/bin/bash
set -eu

# Run the tagger (and tokenizer).
java -Xmx500m -jar $(dirname $0)/ark-tweet-nlp-0.3.jar "$@"
