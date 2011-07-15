THIS IS NOT DONE YET - if you run it, it will give JUNK tags

http://www.ark.cs.cmu.edu/TweetNLP/


To run the tagger
-----------------

The only dependency is Java 6.  Run:

    ./runTagger.sh -input example_tweets.txt -output tagged_tweets.txt

And, `./runTagger.sh -help`  gives an overview of commandline options.


To build from source
--------------------

  scripts/compile.sh


To train and evalute the tagger
-------------------------------

(something very different, can be messy since no one will ever do it)


Directories
-----------
 * runTagger.sh  is the script you probably want

 * lib/          has runtime dependencies
 * lib_build/    has buildtime dependency
 * scripts/      helps you build and run
 * src/          has the actual source code (mostly java, and one bit of scala)


IDE notes
---------

We include the Scala library and compiler to hopefully make a fresh build
painless.  We've also used Eclipse and IDEA too.  You have to either install
the appropriate Scala plugin, or just compile the Scala separately
(`compile_scala.sh`) use the IDE's Java support for everything else.

