Annotation Guidelines for Twitter Part-of-Speech Tagging
========================================================

Authors: Kevin Gimpel, Nathan Schneider, Brendan O'Connor

2012-09-20, for the 0.3 data release (http://www.ark.cs.cmu.edu/TweetNLP/)


Introduction
------------

Social media text differs markedly from traditional written genres like newswire, 
including in ways that bear on linguistic analysis schemes. Here we describe __a 
part-of-speech (POS) tagset used to annotate tweets__, elaborating on 
[this ACL 2011 paper with many authors](http://www.cs.cmu.edu/~nasmith/papers/gimpel+etal.acl11.pdf).
That paper discusses design goals, introduces the tagset, and presents experimental 
results for a supervised tagger. (The annotated datasets have since been expanded and the 
tagger improved.)

Table 1 of the paper provides an overview of the 25 tags. Each is represented with a 
single ASCII symbol. In brief:

* __Nominal__  
  `N` common noun  
  `O` pronoun (personal/WH; not possessive)  
  `^` proper noun  
  `S` nominal + possessive  
  `Z` proper noun + possessive  
* __Other open-class words__  
  `V` verb incl. copula, auxiliaries  
  `A` adjective  
  `R` adverb  
  `!` interjection  
* __Other closed-class words__  
  `D` determiner  
  `P` pre- or postposition, or subordinating conjunction  
  `&` coordinating conjunction  
  `T` verb particle  
  `X` existential _there_, predeterminers  
* __Twitter/online-specific__  
  `#` hashtag (indicates topic/category for tweet)  
  `@` at-mention (indicates another user as a recipient of a tweet)  
  `~` discourse marker, indications of continuation of a message across multiple tweets
  `U` URL or email address  
  `E` emoticon  
* __Miscellaneous__  
  `$` numeral  
  `,` punctuation  
  `G` other abbreviations, foreign words, possessive endings, symbols, garbage
* __Other Compounds__  
  `L` nominal + verbal (e.g. _i'm_), verbal + nominal (_let's_, _lemme_)  
  `M` proper noun + verbal  
  `Y` `X` + verbal  

Since our tokenization scheme avoids splitting most surface words, we opt for 
complex parts of speech where necessary; for instance, _Noah’s_ would receive the `Z` 
(proper noun + possessive) tag.

Tokenization
------------

The __twokenize__ tool included with the ARK TweetNLP package handles tokenization. 
It seeks to identify word-external punctuation tokens and emoticons — not an easy task given 
Twitter’s lack of orthographic orthodoxy!

When multiple words are written together without spaces, or when there are spaces 
between all characters of a word, or when the tokenizer fails to split words, 
we tag the resulting tokens as `G`. This is illustrated by the bolded tokens in the 
following examples:

* It's Been Coldd :/ __iGuess__ It's Better Than Beingg Hot Tho . Where Do Yuhh Live At ? @iKnow\_YouWantMe
* This yearâs almond crop is a great one . And the crop is being shipped fresh to __youâŠNow__ !
* RT @Mz\_GoHAM Um ..... #TeamHeat ........ wut Happend ?? Haha &lt;== #TeamCeltics showin that ass what's __good...That's__ wat happened !!! LMAO
* \#uWasCoolUntil you unfollowed me ! __R E T W E E T__ if you Hate when people do that for no reason .

<!--
28873458992   L V A E G L A P V A R , R V O V P ,
28899336035   D S N N V D A $ , & D N V V V A P G ,
28847830495   # O V O , G G G G G G G P O V R N V D P D N ,
-->

We decided not to manually fix tokenization errors (like in the third example above) 
before POS annotation. We made this decision because we want to be able to run our 
tagger on new text that is tokenized automatically, so we need to _train_ the tagger on annotated 
data that is tokenized in the same way.

Penn Treebank Conventions
-------------------------

Generally, we followed the Penn Treebank (PTB) WSJ conventions in determining parts of speech.
However, there are many inconsistencies in the PTB annotations. We attempted to follow 
the majority convention for a particular use of a word, but in some cases we did not. 
Specific cases which caused difficulty for annotators or necessitated a departure from the PTB 
approach are discussed below.

Gerunds and Participles
-----------------------

Verb forms are nearly always given a verbal tag in the PTB (VBG for _-ing_ forms, 
VBD for _-ed_ forms), so we generally tag them as `V` in our dataset. 
However, PTB sometimes tags as nouns or adjectives words such as 
_upcoming_, _annoying_, _amazing_, _scared_, 
_related_ (as in _the related article_), and _unexpected_. 
We follow the PTB in tagging these as adjectives or nouns, appropriately.

Numbers and Values
------------------

* __Cardinal numbers__ are tagged as `$`.
* __Ordinal numbers__ are typically tagged as adjectives, following the PTB, except for cases like 
_28th October_, in which _28th_ is tagged as `$`.
* __Times__  
  Following the Treebank, _A.M._ and _P.M._ are common nouns, while time zones (_EST_, etc.) are proper nouns.
* __Days, months, and seasons__  
  Like the PTB, days of the week and months of the year are always tagged as proper nouns, while seasons are common nouns.
* __Street addresses__  
  We follow the PTB convention of tagging numbers (house numbers, street numbers, and zip codes) as `$` and all other words 
  in the address as proper nouns. Consider the following PTB example:
  - 153/CD East/NNP 53rd/CD St./NNP
  
  However, this is not entirely consistent in the PTB. Certain street numbers in the PTB are tagged as proper nouns:
  - Fifth/NNP Ave/NNP
  
  Annotators are to use their best judgment in tagging street numbers.
* __Cardinal directions__ (_east_, _NNW_) referred to in isolation (not as a modifier or part of a name) are common nouns
* __Units of measurement__ are common nouns, even if they come from a person’s name (like _Celsius_)

<!-- TODO: inconsistency in the data: °C/N in 95974936845352960 but °F/^ in 28883465935 -->

Time and location nouns modifying verbs
---------------------------------------

In the PTB, time and location nouns (words like _yesterday_/_today_/_tomorrow_, _home_/_outside_, etc.) 
that modify verbs are inconsistently labeled. 
The words _yesterday_/_today_/_tomorrow_ are nearly always tagged as nouns, even when modifying verbs.
For example, in the PTB _today_ is tagged as NN 336 times and RB once. We note, however, that sometimes 
the parse structure can be used to disambiguate the NN tags. When used as an adverb, _today_ is often 
the sole child of an NP-TMP, e.g.,
  - (NP-SBJ (DT These) (JJ high) (NNS rollers) )
      (VP (VBD took) 
        (NP (DT a) (JJ big) (NN bath) )
        (NP-TMP (NN today) )))

When used as a noun, it is often the sole child of an NP, e.g.,

  - (PP-TMP (IN until) (NP (NN today) ))

Since we are not annotating parse structure, it is less clear what to do with our data. In attempting 
to be consistent with the PTB, we typically tagged _today_ as a noun. 

The PTB annotations are less clear for words like _home_. Of the 24 times that _home_ appears as the 
sole child under a _DIR_ (direction) nonterminal, it is annotated as
  - (ADVP-DIR (NN home) ) 14 times
  - (ADVP-DIR (RB home) ) 6 times
  - (NP-DIR (NN home) ) 3 times
  - (NP-DIR (RB home) ) 1 time

Manual inspection of the 24 occurrences revealed no discernible difference in usage that would 
warrant these differences in annotation. As a result of these inconsistencies, we decided to let 
annotators use their best judgment when annotating these types of words in tweets, 
asking them to refer to the PTB and to previously-annotated data to improve consistency. 

<!-- [TODO: examples] [actually, I see at least one PTB example 
that is _go home/RB_. Another possibility is to treat these as intransitive prepositions: 
_go home/P_, _go outside/P_.] -->


Names
-----

In general, every noun within a proper name should be tagged as a proper noun (`^`):
* Jesse/^ and/& the/D Rippers/^ 
* the/D California/^ Chamber/^ of/P Commerce/^

Company and web site names (_Twitter_, _Yahoo ! News_) are tagged as proper nouns.

<!-- the/DT California/NNP Chamber/NNP of/IN Commerce/NNP -- this was a PTB example, I have substituted Twitter tags -->

Function words are only ever tagged as proper nouns
if they are not behaving in a normal syntactic fashion, e.g. _Ace/^ of/^ Base/^_.

* __Personal names__  
  Titles/forms of address with personal names should be tagged as proper nouns: 
  _Mr._, _Mrs._, _Sir_, _Aunt_, _President_, _Captain_
  
  On their own—not preceding someone’s given name or surname—they are common nouns, 
  even in the official name of an office: _President/^ Obama/^ said/V_, 
  _the/D president/N said/V_, _he/O is/V president/N of/P the/D U.S./^_

* __Titles of works__  
  In PTB, simple titles like _Star Wars_ and _Cheers_ are tagged as proper nouns, 
  but titles with more extensive phrasal structure are tagged as ordinary phrases:
  
  - A/DT Fish/NN Called/VBN Wanda/NNP
  
  Note that _Fish_ is tagged as NN (common noun). Therefore, we adopt the following 
  rule: __titles containing only nouns should be tagged as proper nouns, and other titles as ordinary phrases__.


<!--Friday Night Lights = ^^^ ?-->
<!--Justin Bieber's " My World " -- 28867570795-->

Prepositions and Particles
--------------------------

To differentiate between prepositions and verb particles (e.g., _out_ in _take out_), we asked annotators to 
use the following test: 

 - if you can insert an adverb within the phrasal verb, it's probably a preposition rather than a particle:
  - turn slowly into/P a monster
  - *take slowly out/T the trash

Some other verb particle examples:
* what's going on/T
* check it out/T
* shout out/T
  - abbreviations like _s/o_ and _SO_ are tagged as `V`

_this_ and _that_: Demonstratives and Relativizers
------------------------------------

PTB almost always tags demonstrative _this_/_that_ as a determiner, but in cases where it is 
used pronominally, it is immediately dominated by a singleton NP, e.g.

* (NP (DT This)) is Japan

For our purposes, since we do not have parse trees and want to straightforwardly use the tags 
in POS patterns, we tag such cases as pronouns:

* i just orgasmed over __this/O__

<!-- 28139103509815297 -->

as opposed to

* __this/D__ wind is serious

<!-- 194552682147610625 -->

Words where we were careful about the `D`/`O` distinction include, but are not limited 
to: _that, this, these, those, dat, daht, dis, tht_.

When _this_ or _that_ is used as a relativizer, we tag it as `P` (never `O`):

* You should know , __that/P__ if you come any closer ...
* Never cheat on a woman __that/P__ holds you down

<!-- 87519526148780032 115821393559552000 -->

(Version 0.2 of the ACL-2011 data often used this/D for nominal usage, but was somewhat inconsistent.
For the 0.3 release, we changed the tags on the ACL-2011 tweets to conform to the new style; all Daily547 tags conform as well.)

WH-word relativizers are treated differently than the above: they are sometimes tagged as `O`, sometimes as `D`, but never as `P`.

<!--  [TODO: should we normalize them somehow? or another can of worms?] -->

Quantifiers and Referentiality
------------------------------

* A few non-prenominal cases of _some_ are tagged as pronouns (_get some/O_). However, we use _some/D of_, _any/D of_, _all/D of_.
* _someone_, _everyone_, _anyone_, _somebody_, _everybody_, _anybody_, _nobody_, _something_, _everything_, _anything_, and _nothing_ are almost always tagged as nouns
* _one_ is usually tagged as a number, but occasionally as a noun or pronoun when it is referential (inconsistent)
* _none_ is tagged as a noun
* _all_, _any_ are almost always tagged as a (pre)determiner or adverb
* _few_, _several_ are tagged as an adjective when used as a modifier, and as a noun elsewhere
* _many_ is tagged as an adjective
* _lot_, _lots_ (meaning a large amount/degree of something) are tagged as nouns

<!-- TODO: some apparent inconsistencies in the data: someone/O, anyone/O, any1/O, all/O, Everybody/O. many/A in 28914826190, 28897684962 are not prenominal and thus might be reconsidered in light of 'few', 'several', and 'many'. Also, I think most/A in 28905710274 ought to be an adverb. -->

Hashtags and At-mentions
------------------------

As discussed in the [ACL paper](http://www.cs.cmu.edu/~nasmith/papers/gimpel+etal.acl11.pdf), 
__hashtags__ used within a phrase or sentence are not 
distinguished from normal words. However, when the hashtag is external to the syntax 
and merely serves to categorize the tweet, it receives the `#` tag.

__At-mentions__ _always_ receive the `@` tag, even though they occasionally double 
as words within a sentence.


Multiword Abbreviations
------------------------

Some of these have natural tag correspondences: _lol_ (laughing out loud) is typically 
an exclamation, tagged as `!`; _idk_ or _iono_ (I don’t know) can be tagged as `L` 
(nominal + verbal).

Miscellaneous kinds of abbreviations are tagged with `G`:
* _ily_ (I love you)
* _wby_ (what about you)
* _mfw_ (my face when)

<!-- removed let's from list above since we tag it as L. was: _let's_ (let us) -->
<!-- TODO: buy'em should also be tagged as L, but in the data it is V. -->

Metalinguistic Mentions
-----------------------

Mentions of a word (typically in quotes) are tagged as if the word had been used normally:

* RT @HeyKikO Every girl lives for the " unexpected hugs from behind " moments &lt; I wouldn't say " __live__ "... but they r nice

  > Here _live_ is tagged as a verb.

Clipping
--------

Due to space constraints, words at the ends of tweets are sometimes cut off.
We attempt to tag the partial word as if it had not been clipped. If the tag is unclear, 
we fall back to `G`:


* RT @ucsantabarbara : Tonight's memorial for Lucas Ransom starts at 8:00 p.m. and is being held at the open space at the corner of Del __Pla__ ...  

  > We infer that _Pla_ is a clipped proper name, and accordingly tag it as `^`.

* RT @BroderickWalton : Usually the people that need our help the most are the ones that are hardest 2 get through 2 . Be patient , love on __t__ ...  

  > The continuation is unclear, so we fall back to _t/G_.

<!--
28841569916   ~ @ ~ S N P ^ ^ V P $ N & V V V P D A N P D N P ^ ^ ~
28905710274   ~ @ ~ R D N P V D N D A V D N D V R P V P P , V N , V P G ~
-->

<!--Why does the wifi on my boyfriend& #8217 ; s macbook pro have speed ...: We were trying to figure out why download sp ... http://bit.ly/dbpcE1
"sp" is clearly trimmed due to space constraints, so we tag it as G.-->

Symbols, Arrows, etc.
---------------------

<!--
28860873076   ~ @ ~ V O , L A G U
28841534211   U G # $ ^ ^ , A ^ , N D ^ , V
-->

* RT @YourFavWhiteGuy : Helppp meeeee . I'mmm meltiiinngggg --&gt; http://twitpic.com/316cjg
* http://bit.ly/cLRm23 &lt;-- #ICONLOUNGE 257 Trinity Ave , Downtown Atlanta ... Party This Wednesday ! RT

These arrows (_--&gt;_ and _&lt;--_) are tagged as `G`. But see the next section for 
Twitter discourse uses of arrows, which receive the `~` tag.


Twitter Discourse Tokens: Retweets, Continuation Markers, and Arrow Deixis
--------------------------------------------------------------------------

A common phenomenon in Twitter is the __“retweet construction”__, shown in the following example:

<!--28841537250   ~ @ ~ ^ V D N P O ,-->

* RT @Solodagod : Miami put a fork in it ...

The _RT_ indicates that what follows is a “retweet” of another tweet. Typically it is 
followed by a Twitter username in the form of an at-mention followed by a colon (:). In this 
construction, we tag both the _RT_ and _:_ as `~`.

It is often the case that, due to the presence of the retweet header information, there is 
not enough space for the entirety of the original tweet:

<!--28841503214-->
<!--~ @ ~ P P D N P ^ , O V R V T D N , O V A N , V O V D N P V D G ~-->
<!--huma = G-->

* RT @donnabrazile : Because of the crisis in Haiti , I must now turn down the volume . We are one people . Let us find a way to show our huma ...

Here, the final _..._ is also tagged as `~` because it is not 
intentional punctuation but rather indicates that the tweet has been cut short 
due to space limitations. (cf. ["Clipping" above](#clipping))

Aside from retweets, a common phenomenon in tweets is posting a link to a news story or other 
item of interest on the Internet. Typically the headline/title and beginning of the article 
begins the tweet, followed by _..._ and the URL:

<!--28841540324-->
<!--A ^ N N V N P , R A A N , A N V T P D A N ~ U-->

* New NC Highway Signs Welcome Motorists To " Most Military Friendly State ": New signs going up on the major highways ... http://bit.ly/cPNH6e  

  > Since the ellipsis indicates that the text in the tweet is continued elsewhere (namely 
at the subsequent URL), we tag it as `~`. 

Sometimes instead of _..._, the token _cont_ (short for “continued”) is used to indicate 
continuation:

<!--28936861036   O V O V V D A N O V P , V ^ ^ N , P P O V L P O ~ @ ~ ^ , ~ , U-->

* I predict I won't win a single game I bet on . Got Cliff Lee today , so if he loses its on me RT @e\_one : Texas ( cont ) http://tl.gd/6meogh  

  > Here, _cont_ is tagged as `~` and the surrounding parentheses are tagged as punctutation.

Another use of `~` is for tokens that indicate that one part of a tweet is a response to 
another part, particularly when used in an RT construction. Consider:

<!--RT @ayy_yoHONEY : . #walesaid dt @its_a_hair_flip should go DOWNTOWN ! Lol !!.. #jammy --&gt;~LMAO !! Man BIANCA !!!-->

<!--28860458956   ~ @ ~ A N V Z N P ^ ~ O V O V P O ,-->

* RT @Love\_JAsh : First time seeing Ye's film on VH1 « -What do you think about it ?  

  > The _«_ indicates that the text after it is a response to the text before it, and is therefore tagged with `~`.

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

Nonstandard Spellings
---------------------

We aim to choose tags that reflect what is _meant_ by a token, 
even in the face of typographic errors, spelling errors, or intentional nonstandard spellings.
For instance, in

* I'm here are the game! But not with the tickets from your brother. Lol

it is assumed that _at_ was intended instead of _are_, so _are_ is labeled as a preposition 
(`P`). Likewise, missing or extraneous apostrophes (e.g., _your_ clearly intended as “you are”) 
should not influence the choice of tag.


Direct Address
--------------

Words such as _girl_, _miss_, _man_, and _dude_ are often used vocatively in 
tweets directed to another person. They are tagged as nouns in such cases:

<!--
28909766051   ~ @ ~ V P @ N P ^ Z A G V O V O V N , ~ ! ,
28841447123   O V T V N N , V D N O V V , ! ,
-->

* RT @iLoveTaraBriona : Shout-out to @ImLuckyFeCarter definition of lil webbies i-n-d-e-p-e-n-d-e-n-t --&gt; do you know what means __man__ ??? &lt;&lt; Ayyye !
* I need to go home __man__ . Got some thangs I wanna try . Lol .

On the other hand, when such words do not refer to an individual but provide general 
emphasis, they are tagged as interjections (`!`):

<!--
28851460183   ~ @ ~ , # G @ V V N , ! , # ~ ! , ! ^ ,
28848789014   ^ A N , ! D # V R A ,
28853024982   ! D # V D R A N V P O ,
-->

* RT @ayy\_yoHONEY : . #walesaid dt @its\_a\_hair\_flip should go DOWNTOWN ! Lol !!.. #jammy --&gt;~LMAO !! __Man__ BIANCA !!!
* Bbm yawn face * __Man__ that #napflow felt so refreshing .
* __Man__ da #Lakers have a fucking all-star squad fuck wit it !!


<!--
asian european greek -- all A

So have to get it all in. all is D

So = P when used in beginning of sentence

28887923159, 28913460489, 28917281684 = good examples for why this is hard
good example: 28841534211
-->