#!/bin/bash

$(dirname $0)/scripts/classwrap.sh -Xmx1g edu.cmu.cs.lti.ark.tweetnlp.RunPOSTagger "$@"

