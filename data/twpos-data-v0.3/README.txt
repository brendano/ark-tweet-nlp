============================================================================

    Twitter Part-of-Speech Annotated Data
    Carnegie Mellon University
    http://www.ark.cs.cmu.edu/TweetNLP

=============================================================================

  Description:

    This is release v0.3 of a data set of tweets manually annotated with
    coarse part- of-speech tags. The annotated data is divided into two
    groups:

      * "Oct27": 1827 tweets from 2010-10-27
      * "Daily547": 547 tweets, one per day from 2011-01-01 through 2012-06-30

    The "Oct27" dataset is further split into "train", "dev", and "test" subsets
    (the same splits in Gimpel et al. 2011).

    We distribute two data formats.  The "conll" format has one token per
    line, and a blank line to indicate a tweet boundary.  The "supertsv"
    format includes additional metainformation about tweets (and has a
    different column ordering).

    See the Owoputi and O'Connor (2012) tech report for more information.
    Available at http://www.ark.cs.cmu.edu/TweetNLP

    Also see the annotation guidelines, currently at:
    https://github.com/brendano/ark-tweet-nlp/blob/master/docs/annot_guidelines.md

  Contact:

    Please contact Brendan O'Connor (brenocon@cs.cmu.edu, http://brenocon.com)
    and Kevin Gimpel (kgimpel@cs.cmu.edu) with any questions about this
    release.

  Changes:

    Version 0.3 (2012-09-19): Added new Daily547 data, fixed inconsistencies
    in Oct27 data (see anno_changes/).  Documented in the 2012 tech report.

    Version 0.2.1 (2012-08-01): License changed from GPL to CC-BY.

    Version 0.2 (2011-08-15): Based on an improved Twitter tokenizer.  After
    the new tokenizer was run, tweets with differing tokenizations were
    reannotated following the same guidelines as the initial release.

    Version 0.1 (2011-04-26): First release.

  References:

    The following papers describe this dataset.  If you use this data in a
    research publication, we ask that you cite this (the original paper):

    Kevin Gimpel, Nathan Schneider, Brendan O'Connor, Dipanjan Das,
      Daniel Mills, Jacob Eisenstein, Michael Heilman, Dani Yogatama, Jeffrey
      Flanigan, and Noah A. Smith.

    Part-of-Speech Tagging for Twitter: Annotation, Features, and Experiments.
    In Proceedings of the Annual Meeting of the Association for Computational
      Linguistics, companion volume, Portland, OR, June 2011.

    Changes to the 0.3 version are described in

    Part-of-Speech Tagging for Twitter: Word Clusters and Other Advances
    Olutobi Owoputi, Brendan O'Connor, Chris Dyer, Kevin Gimpel, and
      Nathan Schneider.
    Technical Report, Machine Learning Department. CMU-ML-12-107.
    September 2012.


    

============================================================================

Copyright (C) 2011-2012
Kevin Gimpel, Nathan Schneider, Brendan O'Connor, Dipanjan Das, Daniel
Mills, Jacob Eisenstein, Michael Heilman, Dani Yogatama, Jeffrey Flanigan,
and Noah A. Smith 
Language Technologies Institute, Carnegie Mellon University

This data is made available under the terms of the Creative Commons
Attribution 3.0 Unported license ("CC-BY"):
http://creativecommons.org/licenses/by/3.0/

