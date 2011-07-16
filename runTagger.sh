#!/bin/bash

$(dirname $0)/scripts/classwrap.sh -Xmx2g edu.cmu.cs.lti.ark.tweetnlp.RunPOSTagger "$@"
