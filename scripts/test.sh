#!/bin/sh

# Test the tagger on annotated data -- this script needs changes to work

root=$(dirname $0)/..
jhome=/usr/bin

classpath="build:lib/jargs.jar:lib/posBerkeley.jar:lib/commons-codec-1.4.jar"

set -eux
(
cd $root

testfile=/Users/brendano/ark/blitz2011/blitz2011/data/newsplit/dev.goldtags.tab
${jhome}/java -Xmx2g -Xms2g -cp ${classpath} \
      edu.cmu.cs.lti.ark.ssl.pos.SemiSupervisedPOSTagger \
      --trainOrTest test \
      --testSet $testfile \
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
      --runOutput $2     \
      --execPoolDir tmp2  \
      --modelFile $1     \
      --noahsFeaturesFile noah.feats
) 2>&1
