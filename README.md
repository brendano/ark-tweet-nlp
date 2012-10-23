CMU ARK Twitter Part-of-Speech Tagger v0.3
http://www.ark.cs.cmu.edu/TweetNLP/

# Requirements
* maven 3.x
* java 1.6

# Installation/Building
1. Clone the repository
2. In your shell run: `./install_local_dependcies.sh`. This installs the third
party jars that are shipped in the lib directory into your local maven repository. 
You can easily remove them through running `./remove_local_dependencies.sh`. They 
are all somewhat prefixed through the groupId `edu.cmu.cs` so they do no harm to
other projects.
3. In the project directory run `mvn package`. This produces a big jar with all 
dependencies already included. After the successful build it can be found under
`target/ark-tweet-nlp-VERSION.jar` where `VERSION` is the current version of the 
project. 

# Training a model
After building the jar through `mvn package` you can train your own model or use
an already created [one (It is packaged with the released version.)](http://code.google.com/p/ark-tweet-nlp/downloads/detail?name=ark-tweet-nlp-0.3.tgz)

To train your own model run in the project directory:

`java -Xmx2g -cp target/ark-tweet-nlp-VERSION.jar cmu.arktweetnlp.Train data/twpos-data-v0.3/oct27.conll MY_MODEL_OUTPUT_FILE`

# Using the tagger
After you have a trained model you can simply run the tagger:
`java -jar target/ark-tweet-nlp-VERSION.jar --model MY_MODEL_OUTPUT_FILE examples/example_tweets.txt`

The tagger outputs tokens, predicted part-of-speech tags, and confidences. It 
could look like this (for more information run the command with the `--help` option):
```
Detected text input format
I predict I won't win a single game I bet on . Got Cliff Lee today , so if he loses its on me RT @e_one : Texas ( cont ) http://tl.gd/6meogh
O V O V V D A N O V P , V ^ ^ N , P P O V L P O ~ @ ~ ^ ,6meoghO ~ U
0.9981 0.9996 0.9978 0.9969 0.9992 0.9984 0.9634 0.9987 0.9937 0679.9994 0.7663 0.9829 0.9887 0.9939 0.9999 0.9904 0.9982 0.6070 0.9966 0.9965 0.9994 0.9701 0.9846 0.9987 0.9611 0.9983 0.9626 0.9985 0.9777 0.9096 0.9758 0.9970
```

You could also just use the tokenizer like this:
`java -jar target/ark-tweet-nlp-VERSION.jar --just-tokenize`

You may have to adjust the parameters to "java" depending on your system.

# Setting up Eclipse for development
In order to hack on the project simply run
`mvn eclipse:eclipse` and import the project afterwards through eclipse's import wizard. All should 
be set up properly. (Remember to install the shipped third party jars though!)

# Information
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

# Contact
Please contact Brendan O'Connor (brenocon@cs.cmu.edu) and Kevin Gimpel
(kgimpel@cs.cmu.edu) if you encounter any problems.
