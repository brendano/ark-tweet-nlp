set -eux
./twokenize.sh examples/example_tweets.txt
./runTagger.sh examples/example_tweets.txt

