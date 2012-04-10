#!/bin/sh
set -eux
# Test the tagger on annotated data
testfile=${3:-"$HOME/ark/blitz2011/blitz2011/data/newsplit/dev.goldtags.tab"}
java -Xmx2g -Xms2g -cp "$(dirname $0)/../ark-tweet-nlp/target/bin/ark-1.0-SNAPSHOT.jar" \
      edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger
      --trainOrTest test \
      --testSet "$testfile" \
      --numLabeledSentences 100000 \
      --maxSentenceLength 200 \
      --useGlobalForLabeledData \
      --useStandardMultinomialMStep  \
      --useStandardFeatures \
      --regularizationWeight 424242 \
      --regularizationBias 0.0 \
      --initialWeightsLower -0.01 \
      --initialWeightsUpper 0.01 \
      --iters 1000 \
      --printRate 100 \
      --runOutput "$2"     \
      --execPoolDir tmp2  \
      --modelFile "$1"     \
      --noahsFeaturesFile noah.feats
