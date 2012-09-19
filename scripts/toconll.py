#!/usr/bin/env python
# Take the pretsv format and make it CoNLL-like ("supertsv", having tweet metadata headers)
import sys,json
from datetime import datetime

for line in sys.stdin:
    parts = line.split('\t')
    tokens = parts[0].split()
    tags = parts[1].split()
    try:
        d = json.loads(parts[-1])
        print "TWEET\t{}\t{}".format(d['id'], datetime.strptime(d['created_at'], '%a %b %d %H:%M:%S +0000 %Y').strftime("%Y-%m-%dT%H:%M:%S"))
        print "TOKENS"
    except:
        pass

    for tok,tag in zip(tokens,tags):
        print "{}\t{}".format(tag,tok)
    print ""

