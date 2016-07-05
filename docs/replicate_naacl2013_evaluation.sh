#!/bin/bash
# These are the commands described in training.txt
# Training takes <10 minutes
set -eux
java -cp ark-tweet-nlp-0.3.2.jar  cmu.arktweetnlp.Train data/twpos-data-v0.3/oct27.conll mymodel
java -cp ark-tweet-nlp-0.3.2.jar cmu.arktweetnlp.RunTagger --input-format conll --model mymodel data/twpos-data-v0.3/daily547.conll > pred

# this was run 2016-07-05 by brendan with version 0.3.2
# i got the result:
# 7184 / 7707 correct = 0.9321 acc, 0.0679 err
# 547 tweets in 2.0 seconds, 268.8 tweets/sec

