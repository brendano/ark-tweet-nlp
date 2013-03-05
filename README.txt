CMU ARK Twitter Part-of-Speech Tagger v0.3
http://www.ark.cs.cmu.edu/TweetNLP/

Basic usage for released version
================================

Requires Java 6.  To run the tagger on example data, try:

    java -Xmx500m -jar ark-tweet-nlp-0.3.2.jar examples/example_tweets.txt

where the jar file is the one included in the release download.
The tagger outputs tokens, predicted part-of-speech tags, and confidences.
Use the "--help" flag for more information.  On Unix systems, "./runTagger.sh"
invokes the tagger; e.g.

    ./runTagger.sh examples/example_tweets.txt
    ./runTagger.sh --help

We also include a script that invokes just the tokenizer:

    ./twokenize.sh examples/example_tweets.txt

You may have to adjust the parameters to "java" depending on your system.

If instead you are using a source checkout, see docs/hacking.txt for info.

Information
===========

Version 0.3 of the tagger is much faster and more accurate.  Please see the
tech report on the website for details.

For the Java API, see src/cmu/arktweetnlp; especially Tagger.java.
See also documentation in docs/ and src/cmu/arktweetnlp/package.html.

This tagger is described in the following two papers, available at the website.
Please cite these if you write a research paper using this software.

Part-of-Speech Tagging for Twitter: Annotation, Features, and Experiments
Kevin Gimpel, Nathan Schneider, Brendan O'Connor, Dipanjan Das, Daniel Mills,
  Jacob Eisenstein, Michael Heilman, Dani Yogatama, Jeffrey Flanigan, and 
  Noah A. Smith
In Proceedings of the Annual Meeting of the Association
  for Computational Linguistics, companion volume, Portland, OR, June 2011.
http://www.ark.cs.cmu.edu/TweetNLP/gimpel+etal.acl11.pdf

Part-of-Speech Tagging for Twitter: Word Clusters and Other Advances
Olutobi Owoputi, Brendan O'Connor, Chris Dyer, Kevin Gimpel, and
  Nathan Schneider.
Technical Report, Machine Learning Department. CMU-ML-12-107. September 2012.

Contact
=======

Please contact Brendan O'Connor (brenocon@cs.cmu.edu) and Kevin Gimpel
(kgimpel@cs.cmu.edu) if you encounter any problems.
