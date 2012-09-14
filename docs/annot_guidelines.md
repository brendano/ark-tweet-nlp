Annotation Guidelines for Twitter Part-of-Speech Tagging
========================================================

Authors: Kevin Gimpel and Nathan Schneider
2012-09-14


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

* __Nominal, Nominal + Verbal__  
  `N` common noun  
  `O` pronoun (personal/WH; not possessive)  
  `S` nominal + possessive  
  `^` proper noun  
  `Z` proper noun + possessive  
  `L` nominal + verbal  
  `M` proper noun + verbal  
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
  `Y` `X` + verbal  
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

Since our tokenization scheme avoids splitting most surface words, so we opt for 
complex parts of speech where necessary; for instance, _Noah’s_ would receive the `Z` 
(proper noun + possessive) tag.

Tokenization
------------

The __twokenize__ tool included with the ARK TweetNLP package handles tokenization. 
It seeks to identify word-external punctuation tokens and emoticons—not an easy task given 
Twitter’s lack of orthographic orthodoxy!

When multiple words are written together without spaces, or when there are spaces 
between all characters of a word, or when the tokenizer fails to split words, 
we tag the resulting tokens as `G`. This is illustrated by the bolded tokens in the 
following examples:

<!--28873458992-->
<!--L V A E G L A P V A R , R V O V P ,-->
<!--28899336035-->
<!--D S N N V D A $ , & D N V V V A P G ,-->
<!--28847830495-->
<!--# O V O , G G G G G G G P O V R N V D P D N ,-->

* It's Been Coldd :/ __iGuess__ It's Better Than Beingg Hot Tho . Where Do Yuhh Live At ? @iKnow\_YouWantMe
* This yearâs almond crop is a great one . And the crop is being shipped fresh to __youâŠNow__ !
* RT @Mz\_GoHAM Um ..... #TeamHeat ........ wut Happend ?? Haha &lt;== #TeamCeltics showin that ass what's __good...That's__ wat happened !!! LMAO
* #uWasCoolUntil you unfollowed me ! __R E T W E E T__ if you Hate when people do that for no reason .

Penn Treebank Conventions
-------------------------

Generally, we followed the Penn Treebank (PTB) WSJ conventions in determining parts of speech.
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
  
  However, certain street numbers in the PTB are tagged as proper nouns:
  - Fifth/NNP Ave/NNP
  
  Annotators are to use their best judgment in tagging street numbers.
* __Cardinal directions__ (_east_, _NNW_) are common nouns [TODO: in all cases?]
* __Units of measurement__ are common nouns, even if they come from a person’s name (like _Celsius_)

Time and location nouns modifying verbs
---------------------------------------

Words like _yesterday_/_today_/_tomorrow_, _home_/_outside_, etc. should be treated as 
nouns even when modifying verbs: [TODO: examples] [actually, I see at least one PTB example 
that is _go home/RB_. Another possibility is to treat these as intransitive prepositions: 
_go home/P_, _go outside/P_.]


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


Pronouns
--------

[demonstrative pronouns, _one_/_something_, etc.—are we sticking to PTB on these?]


Prepositions, Particles, and Adverbs
------------------------------------

[TODO: attempt to clarify these]


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
* _let's_ (let us), _buy'em_ (buy them)
* _mfw_ (my face when)

Clipping
--------

Due to space constraints, words at the ends of tweets are sometimes cut off.
We attempt to tag the partial word as if it had not been clipped. If the tag is unclear, 
we fall back to `G`:

<!--28841569916-->
<!--~ @ ~ S N P ^ ^ V P $ N & V V V P D A N P D N P ^ ^ ~-->
<!--28905710274-->
<!--~ @ ~ R D N P V D N D A V D N D V R P V P P , V N , V P G ~-->

* RT @ucsantabarbara : Tonight's memorial for Lucas Ransom starts at 8:00 p.m. and is being held at the open space at the corner of Del __Pla__ ...  

  > We infer that _Pla_ is a clipped proper name, and accordingly tag it as `^`.

* RT @BroderickWalton : Usually the people that need our help the most are the ones that are hardest 2 get through 2 . Be patient , love on __t__ ...  

  > The continuation is unclear, so we fall back to _t/G_.

<!--Why does the wifi on my boyfriend& #8217 ; s macbook pro have speed ...: We were trying to figure out why download sp ... http://bit.ly/dbpcE1
"sp" is clearly trimmed due to space constraints, so we tag it as G.-->

Symbols, Arrows, etc.
---------------------

<!--28860873076-->
<!--~ @ ~ V O , L A G U-->
<!--28841534211-->
<!--U G # $ ^ ^ , A ^ , N D ^ , V-->

* RT @YourFavWhiteGuy : Helppp meeeee . I'mmm meltiiinngggg --&gt; http://twitpic.com/316cjg
* http://bit.ly/cLRm23 &lt;-- #ICONLOUNGE 257 Trinity Ave , Downtown Atlanta ... Party This Wednesday ! RT

These arrows (_--&gt;_ and _&lt;--_) are tagged as `G`. But see the next section for 
Twitter discourse uses of arrows, which receive the `~` tag.


Twitter Discourse Tokens: Retweets, Continuation Markers, and Arrow Deixis
--------------------------------------------------------------------------

A common phenomenon in Twitter is the __“retweet construction”__, shown in the following example:

<!--28841537250-->
<!--~ @ ~ ^ V D N P O ,-->

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

<!--28936861036-->
<!--O V O V V D A N O V P , V ^ ^ N , P P O V L P O ~ @ ~ ^ , ~ , U-->

* I predict I won't win a single game I bet on . Got Cliff Lee today , so if he loses its on me RT @e\_one : Texas ( cont ) http://tl.gd/6meogh  

  > Here, _cont_ is tagged as `~` and the surrounding parentheses are tagged as punctutation.

Another use of `~` is for tokens that indicate that one part of a tweet is a response to 
another part, particularly when used in an RT construction. Consider:

<!--RT @ayy_yoHONEY : . #walesaid dt @its_a_hair_flip should go DOWNTOWN ! Lol !!.. #jammy --&gt;~LMAO !! Man BIANCA !!!-->

<!--28860458956-->
<!--~ @ ~ A N V Z N P ^ ~ O V O V P O ,-->

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

<!--28909766051-->
<!--~ @ ~ V P @ N P ^ Z A G V O V O V N , ~ ! ,-->
<!--28841447123-->
<!--O V T V N N , V D N O V V , ! ,-->

* RT @iLoveTaraBriona : Shout-out to @ImLuckyFeCarter definition of lil webbies i-n-d-e-p-e-n-d-e-n-t --&gt; do you know what means __man__ ??? &lt;&lt; Ayyye !
* I need to go home __man__ . Got some thangs I wanna try . Lol .

On the other hand, when such words do not refer to an individual but provide general 
emphasis, they are tagged as interjections (`!`):

<!--28851460183-->
<!--~ @ ~ , # G @ V V N , ! , # ~ ! , ! ^ ,-->
<!--28848789014-->
<!--^ A N , ! D # V R A ,-->
<!--28853024982-->
<!--! D # V D R A N V P O ,-->

* RT @ayy\_yoHONEY : . #walesaid dt @its\_a\_hair\_flip should go DOWNTOWN ! Lol !!.. #jammy --&gt;~LMAO !! __Man__ BIANCA !!!
* Bbm yawn face * __Man__ that #napflow felt so refreshing .
* __Man__ da #Lakers have a fucking all-star squad fuck wit it !!


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