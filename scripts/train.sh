#!/bin/bash

# Train the tagger on annotated data -- this script needs changes to work

jhome=/usr/bin
root=$(dirname $0)/..

classpath="build:lib/jargs.jar:lib/posBerkeley.jar:lib/commons-codec-1.4.jar"

set -eux
model_dir=$1
shift
mkdir -p $model_dir

(
cd $root

${jhome}/java -Xmx2g -Xms2g -cp ${classpath} \
edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger \
--trainOrTest train \
--trainSet /Users/brendano/ark/blitz2011/blitz2011/data/newsplit/train.goldtags.tab \
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
--execPoolDir tmp \
--modelFile $model_dir/model \
--noahsFeaturesFile noah.feats \
"$@"
) 2>&1
