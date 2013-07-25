#!/bin/bash
awk '
$2=="(" { $2="-LRB-" }
$2==")" { $2="-RRB-" }
{print $1 "\t" $2}'
