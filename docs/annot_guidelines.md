Annotation Guidelines for Twitter Part-of-Speech Tagging
========================================================

Authors: Kevin Gimpel, Nathan Schneider, ...?

This document is a DRAFT. Note that comments ported from the 2011-02-23 LaTeX version are retained as HTML comments.

Recent Issues
-------------

### Nathan's notes from annotation ###

* days of week, months are ^
* a big issue with the default tags: 'that' functioning as a pronoun but tagged as D.
 - Kevin points out that always tagging 'that' as a determiner follows the PTB (where the distinction can be seen not in the POS tag but in the containing phrase—the pronoun-like use is annotated as a DT which is its own NP).
 - I have been tagging that/O but those are easy converted as postprocessing if we want to ensure consistency.

Difficult cases:

* 25244937167568896 x2 - emoticon? 2 times?
* 26042601522073600 that one - one tagged as pronoun
* 26085492428636161 Prime Minister
* 26085492428636161 cash-for-sex
* 28569472516235264 ass clitic
* 30656193286377472 mention of the word "aint" (metalinguistic)
* 30656193286377472 yes, no
* 32189630715535361 you two
* 32468904269844480 Let's (verbal + nominal)? 38756380194254849 buy'em
* 32942659601440768 (several issues)
* 36246374219522048 vocative 'dude' (noun? interjection?)
  - per below, should be a noun
* 36246374219522048 down there
* 36949436257013760, 37310741828603905 Valentine's Day (^ ^ or Z N?)
* 37935252550860800 up for grabs
* 38756380194254849 All-star Weekend (^ ^ or N N?)
* 41198546862616576 Trueshit (! or N?)
* 43341785887543296 "It kinda feels like a junk food kinda day." - second _kinda_ is really _kind of_ 
  - tagged as G
* 49149970200272896 "Xbox Kinect FAIL"
  - FAIL as N (because it would be _spectacular FAIL_, not _*spectacularly FAIL_)
* 49559011401531392 "SHM" = shower, hair, makeup
  - tagged as N per below...this would be a good example to list
* 49931213963665408 "mfw" = my face when (not sure I've ever seen an abbreviation ending with a complementizer!)
  - tagged as G
* 51858412669124608 at-mentions @wordpress and @joomla are clearly used within the sentence. cf. 58666191618719744, 65450293604777984
  - we still use @ regardless of context (unlike with hashtags)
* Citizens United? Media Lab? are the nouns here N or ^?
  - (clarifying the discussion below) For most proper names, all content words are ^ regardless of whether there is internal syntactic structure. An exception is made for titles of works which have an "internal interpretation" independent of the reference to the work, and which contain non-nouns: for such cases the title is tagged as if it were a normal phrase. Function words are only tagged with ^ if they are not behaving in a normal syntactic fashion, e.g. Ace/^ of/^ Base/^.

#### Preposition-y things ####

difficult decision in annotation: preposition-like verb modifiers. preposition, particle, or adverb? i am only tagging as particles (T) intransitive prepositions for verbs which can be transitive (e.g., 'up' in 'wake up'/'wake up his friend'). I do not use T for the rare verbs and adjectives that can serve as particles (cut *short*, let *go*). For 41852812245598208 (stick around) I went with P because 'stick around' cannot be transitive (I think 'stick around the house' is [stick [around the house]], not [[stick around\] \[the house]]).


Introduction
------------

* TODO: provide tag set, maybe same table as is in the submission.

Penn Treebank Conventions
-------------------------

Generally, we followed the Penn Treebank (PTB) conventions in doing this annotation.

We discuss some of the PTB conventions we followed because these are often 
cases of disagreement among annotators.

### Gerunds ###

[I think “gerund” is a narrower term than you mean here---you probably want something like “verb forms.” --NSS]

Gerunds are nearly always tagged as VBG in the PTB, so we generally tag them 
as V in our data set. However, certain gerunds are sometimes tagged as nouns or 
adjectives in the PTB, such as the following: _upcoming_, _annoying_, _amazing_, _scared_, 
_related_ (as in _the related article_), and _unexpected_. 
We follow the PTB in tagging these as adjectives or nouns, appropriately. 

When times are mentioned along with _AM_ or _PM_, we tag these as nouns, like the PTB.

For tagging numberic tokens, cardinals are tagged as `$`.
Ordinals are typically tagged as adjectives, following the PTB, except for examples like 
“28th October”, in which “28th” is tagged as `$`. 

### Proper Nouns ###

Regarding titles of books, movies, songs, etc., we decided to use the following convention, inspired by the Treebank:

In the Treebank, short titles are tagged as proper nouns like “Star/NNP Wars/NNPS” and “Cheers/NNP” but then longer titles are different, like the movie title “A/DT Fish/NN Called/VBN Wanda/NNP”. Note that “Fish” is tagged as NN, not NNP. Following these observations, we decided to use the following convention: if the title in question contains only nouns, then mark them all as `^`.  But if the title contains multiple parts of speech, then tag each word appropriately, only using `^` if the word corresponds to an actual proper noun, like “Wanda” above.  

For non-compositional names such as the band name “Ace of Base”, we tag each word as a proper noun.
Compositional names are tagged like the following: 
<!--band _Jesse and the Rippers_ are tagged as -->
_Jesse_/^ _and_/& _the_/D _Rippers_/^ 
<!--Similarly, -->
“the/DT California/NNP Chamber/NNP of/IN Commerce/NNP”

Company and web site names are tagged as proper nouns, such as _Twitter_ and _Yahoo ! News_.

Like the PTB, days of the week and months of the year are always tagged as proper nouns, while seasons are common nouns.

<!--Friday Night Lights = ^^^ ?-->
<!--Justin Bieber's " My World " -- 28867570795-->
<!--companies, blog names, website names (if they are company names) are ^-->

<!--#### Addresses ####-->
To tag street addresses, we follow the PTB convention of tagging numbers 
(house numbers, street numbers, and zip codes) as $ and all other words 
in the address as proper nouns. 
Consider the following example from the PTB:

* (1) 153/CD East/NNP 53rd/CD St./NNP


But certain street numbers in the PTB are tagged as proper nouns, e.g., “Fifth/NNP Ave/NNP”. 
Annotators use their best judgment to determine whether to tag the cardinal street number. 

Phenomena in Twitter
--------------------

And there are many phenomena that are frequent in Twitter that are not so frequent in the PTB.

<!--The "G" tag is used for several categories of tokens. -->

### Multi-Word Abbreviations ###

We tag abbreviations that include multiple parts-of-speech as G, like the following examples: 

* (2) ily (i love you)
* (3) wby (what about you)
* (4) idk (i don't know)

TODO: change idk to nominal+verbal.

### Partial Words ###

Due to space constraints, words at the ends of tweets are sometimes cut off.
If it is apparent from context what the partial word is (or at least what its tag is most likely to be), we tag the partial word as that tag. If it is unclear, we tag it as G. Consider the following examples:

<!--28905710274-->

* (5) RT @BroderickWalton : Usually the people that need our help the most are the ones that are hardest 2 get through 2 . Be patient , love on t ...

<!--~ @ ~ R D N P V D N D A V D N D V R P V P P , V N , V P G ~-->
<!--28841569916-->

* (6) RT @ucsantabarbara : Tonight's memorial for Lucas Ransom starts at 8:00 p.m. and is being held at the open space at the corner of Del Pla ...

<!--~ @ ~ S N P ^ ^ V P $ N & V V V P D A N P D N P ^ ^ ~-->

In example `(5)` the “t” at the end is tagged as G, since it 
is clearly truncated due to space constraints and it is unknown what tag the 
full word would receive. However, in example `(6)`, even though “Pla” is 
presumably a partial word, we assume that it is a proper noun since it is part 
of a street or place name and begins with a capital letter, so we tag it 
as `^`.

<!--Why does the wifi on my boyfriend& #8217 ; s macbook pro have speed ...: We were trying to figure out why download sp ... http://bit.ly/dbpcE1
"sp" is clearly trimmed due to space constraints, so we tag it as G.-->

### Tokenization Challenges ###

When multiple words are written together without spaces, or when spaces are inserted between all characters of a word, or when the tokenizer fails to split words, we tag the resulting tokens as G.

For example:

<!--28873458992-->

* (7) It's Been Coldd :/ iGuess It's Better Than Beingg Hot Tho . Where Do Yuhh Live At ? @iKnow_YouWantMe

<!--L V A E G L A P V A R , R V O V P ,-->

We tag “iGuess” as G.

<!--28899336035-->

* (8) This yearâs almond crop is a great one . And the crop is being shipped fresh to youâŠNow !

<!--D S N N V D A $ , & D N V V V A P G ,-->

“youâŠNow” is tagged as G.

* (9) RT @Mz_GoHAM Um ..... #TeamHeat ........ wut Happend ?? Haha &lt;== #TeamCeltics showin that ass what's good...That's wat happened !!! LMAO
The tokenizer erroneously left “good...That's” as a single token, which we tag as G.

<!--28847830495-->

* (10) #uWasCoolUntil you unfollowed me ! R E T W E E T if you Hate when people do that for no reason .

<!--# O V O , G G G G G G G P O V R N V D P D N ,-->

All tokens in “R E T W E E T” are tagged as G.

### Symbols, Arrows, etc. ###

<!--28860873076-->

* (11) RT @YourFavWhiteGuy : Helppp meeeee . I'mmm meltiiinngggg --&gt; http://twitpic.com/316cjg

<!--~ @ ~ V O , L A G U-->
<!--28841534211-->

* (12) http://bit.ly/cLRm23 &lt;-- #ICONLOUNGE 257 Trinity Ave , Downtown Atlanta ... Party This Wednesday ! RT

<!--U G # $ ^ ^ , A ^ , N D ^ , V-->

The arrow (“--&gt;”) is tagged as G. In some cases, arrows 
like these are used to indicate when a comment is a response to another tweet, 
in which case the tag `~` is used. Section XYZ below shows examples 
of this.

### Twitter Discourse Tokens ###

A common phenomenon in Twitter is the “retweet construction”, shown in the following example:

<!--28841537250-->

* (13) RT @Solodagod : Miami put a fork in it ...

<!--~ @ ~ ^ V D N P O ,-->

The token “RT” indicates that what follows is a “retweet” of another tweet. Typically it is 
followed by a Twitter username in the form of an at-mention followed by a colon (:). In this 
construction, we tag both the “RT” and “:” as `~`.

It is often the case that, due to the presence of the retweet header information, there is 
not enough space for the entirety of the original tweet, as in the 
following example:

<!--28841503214-->

* (14) RT @donnabrazile : Because of the crisis in Haiti , I must now turn down the volume . We are one people . Let us find a way to show our huma ...

<!--~ @ ~ P P D N P ^ , O V R V T D N , O V A N , V O V D N P V D G ~-->
<!--huma = G-->

In this tweet, in addition to tagging the “RT” and “:” as `~`, 
the final “...” is also tagged as `~` because it is not 
intentional punctuation but rather indicates that the tweet has been cut short 
due to space limitations. 

Aside from retweets, a common phenomenon in tweets is posting a link to a news story or other 
item of interest on the internet. Typically the headline/title and beginning of the article 
begins the tweet, followed by “...” and the URL, as in the following example:

<!--28841540324-->

* (15) New NC Highway Signs Welcome Motorists To " Most Military Friendly State ": New signs going up on the major highways ... http://bit.ly/cPNH6e

<!--A ^ N N V N P , R A A N , A N V T P D A N ~ U-->

Since the “...” is used to indicate that the text in the tweet is continued elsewhere (namely at the subsequent URL), we tag it as `~`. Sometimes instead of “...”, the token “cont” (abbreviation for “continued”) is used to indicate continuation, as in the following:

<!--28936861036-->

* (16) I predict I won't win a single game I bet on . Got Cliff Lee today , so if he loses its on me RT @e_one : Texas ( cont ) http://tl.gd/6meogh

<!--O V O V V D A N O V P , V ^ ^ N , P P O V L P O ~ @ ~ ^ , ~ , U-->

Here, “cont” is tagged as `~` and the surrounding parentheses are tagged as puncutation.

Another use for `~` is for tokens that indicate that one part of a tweet is a response to another part, particularly when used in an RT construction. Consider the following example:

<!--RT @ayy_yoHONEY : . #walesaid dt @its_a_hair_flip should go DOWNTOWN ! Lol !!.. #jammy --&gt;~LMAO !! Man BIANCA !!!-->

<!--28860458956-->

* (17) RT @Love_JAsh : First time seeing Ye's film on VH1 « -What do you think about it ?

<!--~ @ ~ A N V Z N P ^ ~ O V O V P O ,-->

The “«” token indicates that the text after it is a response to the text before it, and is therefore tagged with `~`. 

<!--arrows = ~-->

<!--if a tweet is clearly cut off due to space constraints and ends with "...", we tag the ellipsis as `~`.-->
<!--If the thought appears to be complete and the author merely uses "..." to indicate ordinary ellipsis, we tag it as "," (punctuation).-->

<!--"Audio: \u203a Lupe Fiasco - The show goes on \u203a \u203a 1st Single\u201dOff \u201cLasers\u2019s\u201d Album http:\/\/tumblr.com\/xlnngu9sg"-->


<!--when RT is used as a V, tag it as V-->

<!--RT as verb example -- 28920085623-->

<!--28864005243 for ~ and heart (but wide char)-->

<!--28924724042
RT~ @lilduval@ AD waitressN inP theD stripN clubN ain'tV nothingN but& aD stripperN witP aD apronN <-~ Ha! !, @MayaKisseZ
-->

<!--PublicA and& privateA jobsN |~ Angry^ Bear^ :~ TheD wifeN and& IO usedV toP workV retailN forP theD California^ State^ Parks^ ., IfP ourD paG ...~ http://bit.ly/9EeCmY-->

<!--28849968113 no tilde
compare with 28849968113
-->

<!--
Photo : http://tumblr.com/xohnc4qk5
: is not ~
-->

<!--
RT @Mz_GoHAM Um ..... #TeamHeat ........ wut Happend ?? Haha &lt;== #TeamCeltics showin that ass what's good...That's wat happened !!! LMAO
good...That's is G
also good example for ~
-->

<!--
non-~ ellipsis -- 28841498542
-->

<!--
28851460183
RT~ @ayy_yoHONEY@ :~ ., #walesaid# dtG @its_a_hair_flip@ shouldV goV DOWNTOWNN !, Lol! !!.., #jammy# --&gt;~ LMAO! !!, Man! BIANCA^ !!!,
-->

### Misspellings and Creative Spellings ###

Words are tagged how they are assumed to be meant, which may not be how they are written.
These may occur due to typographic errors, spelling errors, or intentional nonstandard spellings.
E.g.,
“I'm here are the game! But not with the tickets from your brother. Lol”
It is assumed that the author meant to use “at” instead of “are” (possibly due to a typo and then auto-completion), so “are” is labeled as a preposition (P).

### Words of Direct Address ###

Words of direct address such as “girl”, “miss”, “man”, and “dude” are used frequently in 
tweets directed to another person. They are tagged as nouns. Consider the following example:

<!--28909766051-->

* (18) RT @iLoveTaraBriona : Shout-out to @ImLuckyFeCarter definition of lil webbies i-n-d-e-p-e-n-d-e-n-t --&gt; do you know what means man ??? &lt;&lt; Ayyye !

<!--~ @ ~ V P @ N P ^ Z A G V O V O V N , ~ ! ,-->
<!--28841447123-->

* (19) I need to go home man . Got some thangs I wanna try . Lol .

<!--O V T V N N , V D N O V V , ! ,-->

In these two examples, “man” is tagged as N. More generally, words of direct 
address are tagged as nouns when they refer to an actual person, and 
interjections otherwise, as in the following examples:

<!--28851460183-->

* (20) RT @ayy\_yoHONEY : . #walesaid dt @its_a_hair_flip should go DOWNTOWN ! Lol !!.. #jammy --&gt;~LMAO !! Man BIANCA !!!

<!--~ @ ~ , # G @ V V N , ! , # ~ ! , ! ^ ,-->
<!--28848789014-->

* (21) Bbm yawn face * Man that #napflow felt so refreshing .

<!--^ A N , ! D # V R A ,-->
<!--28853024982-->

* (22) Man da #Lakers have a fucking all-star squad fuck wit it !!

<!--! D # V D R A N V P O ,-->

In all these cases, “Man” is tagged as an interjection (!).

<!--
Discuss verb particles -- insert adverb test
what's going on = on is T
check it out = out is T

s/o SO shout out = V V V T
-->
<!--
asian european greek -- all A

So have to get it all in. all is D

So = P when used in beginning of sentence

28887923159, 28913460489, 28917281684 = good examples for why this is hard
good example: 28841534211

"For those that are confused..." - that is D
things that I can do -- that is D
-->