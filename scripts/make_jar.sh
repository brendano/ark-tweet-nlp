#!/bin/bash

scripts/compile.sh
(cd mybuild && jar cf ../lib/ark-tweet-nlp.jar *)

exit

# for release...
# rm -rf mybuild
# rm -rf lib_build
# rm -rf .git
