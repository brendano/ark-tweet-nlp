# Take the pretsv format and make it easier to read

import sys
for line in sys.stdin:
    parts = line.split('\t')
    tokens = parts[0].split()
    tags = parts[1].split()
    pairs = ["%s/%s" % (tok, tag) for tok,tag in zip(tokens,tags)]
    print ' '.join(pairs)

