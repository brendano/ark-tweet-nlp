#!/usr/bin/env Rscript

# Perform McNemar's test (actually, an exact binomial variant)
# on two CoNLL-formatted tagging files of format:
#  token \t tag
# (and we skip blank lines.)

# Usage:
#  scripts/paired_stats.r GoldDataFile Preds1 Preds2
#
# For example: make taggings from two different models, then evaluate the paired test.
#  scripts/java.sh cmu.arktweetnlp.RunTagger --input-format conll data/twpos-data-v0.3/daily547.conll --model model1 > preds1
#  scripts/java.sh cmu.arktweetnlp.RunTagger --input-format conll data/twpos-data-v0.3/daily547.conll --model model2 > preds2
#  scripts/paired_stats.r data/twpos-data-v0.3/daily547.conll preds1 preds2
#
# Or you could just edit this file if you don't want to use the commandline.

read.tsv = function(f) read.table(f,sep='\t',quote='',comment='',na.strings='',stringsAsFactors=FALSE)

# gold = read.tsv(pipe("grep . conll/daily547.all"))
# gold = read.tsv(pipe("grep . conll/random.test"))
# gold = read.tsv(pipe("grep . conll/daily547.test"))
# gold = read.tsv(pipe("grep . conll/acl11.test"))

# args = commandArgs(trailingOnly=T)
# goldfile = args[1]
# predfile1 = args[2]
# predfile2 = args[3]

read_with_sentid = function(filename) {
  cmd = sprintf("cat %s | awk 'NF{print (s*1),$0} NF==0{s+=1}'", filename)
  d = read.table(pipe(cmd), quote='', comment='', na.strings='', stringsAsFactors=FALSE)
  names(d) = c('sentid','word','tag')
  d
}

goldfile="ritter_eval.stanford.conll"
predfile1="t_eval.vcb-labelled.conll"
predfile2="ritter_eval.stanford.conll.pred"

gold = read_with_sentid(goldfile)
d1 = read_with_sentid(predfile1)
d2 = read_with_sentid(predfile2)
stopifnot(all(d1$word==d2$word) && all(d1$word==gold$word))

cat("\n")
cat(sprintf("%d tokens, %d tweets\n\n", nrow(gold), length(unique(gold$sentid))))

cat("Accuracy rates\n")
cat(sprintf("ACC\t%s\t%s\n", predfile1, mean(d1$tag==gold$tag)))
cat(sprintf("ACC\t%s\t%s\n", predfile2, mean(d2$tag==gold$tag)))

cat("\nContingency table of system correctness indicators\n\n")
# print(mcnemar.test(d1$tag==gold$tag, d2$tag==gold$tag))
t = table(sys1correct=d1$tag==gold$tag, sys2correct=d2$tag==gold$tag)
print(t)
cat("\nExact McNemar: test for the proportion of time system2 is better than system1, when they disagree (and excluding cases where both are wrong); this is just a binomial test on the off-diagonals of the above contingency table.\n")
print(binom.test(t[1,2], t[1,2]+t[2,1]))

cat("System agreement with each other\n")
print(table(d1$tag==d2$tag))
print(table(d1$tag==d2$tag) / nrow(d1))

############

cat("\n\nSentence accuracy\n\n")
library(plyr)
d1$corr = d1$tag==gold$tag
d2$corr = d2$tag==gold$tag
sentacc1 = daply(d1,.(sentid),function(x) mean(x$corr))
sentacc2 = daply(d2,.(sentid),function(x) mean(x$corr))
sentcorr1 = daply(d1,.(sentid),function(x) all(x$corr))
sentcorr2 = daply(d2,.(sentid),function(x) all(x$corr))
cat(sprintf("SENTACC\t%s\t%s\n", predfile1, mean(sentcorr1)))
cat(sprintf("SENTACC\t%s\t%s\n", predfile2, mean(sentcorr2)))
cat("\nExact McNemar for sentence correctness\n")
t = table(sentcorr1, sentcorr2)
print(t)
print(binom.test(t[1,2], t[1,2]+t[2,1]))
