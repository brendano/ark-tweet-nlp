CMU ARK Twitter Part-of-Speech Tagger v0.3
http://www.ark.cs.cmu.edu/TweetNLP/

Basic usage
===========

Requires Java 6.  To run the tagger from unix shell:

    ./runTagger.sh examples/example_tweets.txt

The tagger outputs tokens, predicted part-of-speech tags, and confidences.
For more information:

    ./runTagger.sh --help

We also include a script that invokes just the tokenizer:

    ./twokenize.sh examples/example_tweets.txt

Information
===========

Version 0.3 of the tagger is 40 times faster and more accurate.  Please see the tech report on the website for details.

This tagger is described in the following two papers, available at the website.  Please cite this if you write a research paper using this software.

  Part-of-Speech Tagging for Twitter: Annotation, Features, and Experiments
  Kevin Gimpel, Nathan Schneider, Brendan O'Connor, Dipanjan Das, Daniel Mills,
  Jacob Eisenstein, Michael Heilman, Dani Yogatama, Jeffrey Flanigan, and Noah A. Smith
  In Proceedings of the Annual Meeting of the Association for Computational
  Linguistics, companion volume, Portland, OR, June 2011.
  http://www.ark.cs.cmu.edu/TweetNLP/gimpel+etal.acl11.pdf

Contact
=======

Please contact Brendan O'Connor (brenocon@cmu.edu) and Kevin Gimpel (kgimpel@cs.cmu.edu) if you encounter any problems.
