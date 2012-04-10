#!/bin/sh
# Train the tagger on annotated data
set -eux
model_dir=$1
trainset=${2:-"$HOME/ark/blitz2011/blitz2011/data/newsplit/train.goldtags.tab"}
mkdir -p "$model_dir"
java -Xmx2g -Xms2g -cp "$(dirname $0)/../ark-tweet-nlp/target/bin/ark-1.0-SNAPSHOT.jar" \
    edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger \
    --trainOrTest train \
    --trainSet "$trainset" \
    --numLabeledSentences 40000 \
    --maxSentenceLength 200 \
    --useStandardMultinomialMStep  \
    --useStandardFeatures \
    --useGlobalForLabeledData \
    --regularizationWeight 2.23 \
    --regularizationBias 0.0 \
    --initialWeightsLower -0.01 \
    --initialWeightsUpper 0.01 \
    --iters 1000 \
    --printRate 100 \
    --execPoolDir tmp --modelFile "$model_dir/model" \
    --noahsFeaturesFile noah.feats \
    "$@"
