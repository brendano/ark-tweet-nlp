These notes were taken by Nathan during Daily 547 annotations.  Not all final annotation decisions followed the below.


#### Preposition-y things ####

difficult decision in annotation: preposition-like verb modifiers. preposition, particle, or adverb?

* I am only tagging as particles (T) intransitive prepositions for verbs which can be transitive 
(e.g., 'up' in 'wake up'/'wake up his friend'). I do not use T for the rare verbs and adjectives that 
can serve as particles (cut *short*, let *go*). 'around' in 'turn around the company/turn the company around' 
is also a particle.
* For 41852812245598208 (stick around) I went with P because 'stick around' cannot be transitive 
(I think 'stick around the house' is [stick [around the house]], not [[stick around\] \[the house]]). 
* For 'turn (a)round' meaning rotate oneself to face another direction, I tagged as R. I guess this feels 
right because 'around' in this case indicates a path without relating two (overt or implied) entities. (The path is intrinsic?)

### Nathan's notes from annotation ###

(✓ = added below)

* ✓ proposed rule: titles/forms of address with personal names are ^: Mr., Mrs., Sir, Aunt, President, Captain.- 
On their own, they are common nouns, even in the official name of an office: _President/^ Obama/^ said/V_, _the/D president/N said/V_, _he/O is/V president/N of/P the/D U.S./^_
[PTB looks inconsistent, relying too much on capitalization]
* ✓ a big issue with the default tags: 'that' functioning as a pronoun but tagged as D.
  - Kevin points out that always tagging 'that' as a determiner follows the PTB (where the distinction can be seen not in the POS tag but in the containing phrase—the pronoun-like use is annotated as a DT which is its own NP).
  - I have been tagging that/O but those are easy converted as postprocessing if we want to ensure consistency.
  - similarly, I have noticed a couple instances of all/D which could arguably be pronouns (_all who/that VP_), but I haven't changed these.
* ✓ proposed rule: units of measurement are common nouns, even if they come from a person's name (like _Celsius_)
* ✓ proposed rule: cardinal directions (_east_, _NNW_) are common nouns (in all cases?)

Difficult cases:

* 25244937167568896 x2 - emoticon? 2 times?
* 26042601522073600 that one - one tagged as pronoun
* ✓ 26085492428636161 Prime Minister
* 26085492428636161 cash-for-sex
* 28569472516235264 ass clitic
* 30656193286377472 mention of the word "aint" (metalinguistic)
* 30656193286377472 yes, no
* 32189630715535361 you two
* ✓ 32468904269844480 Let's (verbal + nominal)? 38756380194254849 buy'em
* 32942659601440768 (several issues)
* ✓ 36246374219522048 vocative 'dude' (noun? interjection?)
  - per below, should be a noun
* 36246374219522048 down there
* 36949436257013760, 37310741828603905 Valentine's Day (^ ^ or Z N?)
* 37935252550860800 up for grabs
* 38756380194254849 All-star Weekend (^ ^ or N N?)
* 41198546862616576 Trueshit (! or N?)
* 43341785887543296 "It kinda feels like a junk food kinda day." - different senses of _kinda_! first one is a hedge; second one is really _kind of_ (like _type of_).
  - first tagged as R, second tagged as G
* 49149970200272896 "Xbox Kinect FAIL"
  - FAIL as N (because it would be _spectacular FAIL_, not _*spectacularly FAIL_)
* 49559011401531392 "SHM" = shower, hair, makeup
  - tagged as N per below...this would be a good example to list
* ✓ 49931213963665408 "mfw" = my face when (not sure I've ever seen an abbreviation ending with a complementizer!)
  - tagged as G
* ✓ 51858412669124608 at-mentions @wordpress and @joomla are clearly used within the sentence. cf. 58666191618719744, 65450293604777984
  - we still use @ regardless of context (unlike with hashtags)
* ✓ Citizens United? Media Lab? are the nouns here N or ^?
  - (clarifying the discussion below) For most proper names, all content words are ^ regardless of whether there is internal syntactic structure. An exception is made for titles of works which have an "internal interpretation" independent of the reference to the work, and which contain non-nouns: for such cases the title is tagged as if it were a normal phrase. Function words are only tagged with ^ if they are not behaving in a normal syntactic fashion, e.g. Ace/^ of/^ Base/^.
* 81421997929795585 "the other person feelings" - SAE would be person's/S, but should this just be N?