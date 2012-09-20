# convert 'supertsv' format to simple CoNLL
awk -F'\t' -vOFS='\t' '$1 && NF==2 && $1 !~ /TWEET|INFO|TOKENS/ { print $2,$1}  !$1{print}'
