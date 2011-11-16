package edu.cmu.cs.lti.ark.tweetnlp   // comment this out to run as script, scala is weird

import scala.collection.JavaConversions._

/*
 TweetMotif is licensed under the Apache License 2.0: 
 http://www.apache.org/licenses/LICENSE-2.0.html
 Copyright Brendan O'Connor, Michel Krieger, and David Ahn, 2009-2010.
*/

/*

 twokenize.scala -- a little Twitter tokenizer,
 tested for English and some other European languages.
 
 Twokenize.tokenize("@hellocalyclops =))=))=)) Oh well.")
 => ["@hellocalyclops", "=))", "=))", "=))", "Oh", "well", "."]
 
 Invoking main() takes tweet texts on stdin and outputs space-separated tokenizations.
 
 Code History
 * Original version in TweetMotif in Python (2009-2010, github.com/brendano/tweetmotif)
   having two forks:
   - (2011) Scala port and improvements by David Snyder (dsnyder@cs.utexas.edu)
   			and Jason Baldridge (jasonbaldridge@gmail.com)
   			https://bitbucket.org/jasonbaldridge/twokenize/
   - (2011) Modifications for POS tagging by Kevin Gimpel (kgimpel@cs.cmu.edu)
            and Daniel Mills (dpmills@cs.cmu.edu)
 * Merge to Scala by Brendan O'Connor, for ARK TweetNLP package (2011-06)

 Original paper:
  
 TweetMotif: Exploratory Search and Topic Summarization for Twitter.
 Brendan O'Connor, Michel Krieger, and David Ahn.
 ICWSM-2010 (demo track)
 http://brenocon.com/oconnor_krieger_ahn.icwsm2010.tweetmotif.pdf
 
 ---
 
 Scala port of Brendar O'Connor's twokenize.py

 This is not a direct port, as some changes were made in the aim of
 simplicity.

 - David Snyder (dsnyder@cs.utexas.edu)
   April 2011

 Modifications to more functional style, fix a few bugs, and making
 output more like twokenize.py. Added abbrevations. Tweaked some
 regex's to produce better tokens.

 - Jason Baldridge (jasonbaldridge@gmail.com)
   June 2011
*/

/**
 * TODO
 *  - byte offsets should be added here. can easily re-align
 *  since the only munged characters are whitespace (hopefully)
 */

import scala.util.matching.Regex
import collection.JavaConversions._

object Twokenize {

  val Contractions = """(?i)(\w+)(n't|'ve|'ll|'d|'re|'s|'m)$""".r
  val Whitespace = """\s+""".r

  val punctChars = """['“\".?!,:;]"""
  val punctSeq   = punctChars+"""+"""
  val entity     = """&(amp|lt|gt|quot);"""

  //  URLs
  // The "web-only" version of Gruber's URL regex: http://daringfireball.net/2010/07/improved_regex_for_matching_urls
  // For twitter, we probably can ignore non-web UR[IL]s.
  val url        = """\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'\".,<>?«»“”‘’]))"""


  // Numeric
  val timeLike   = """\d+:\d+"""
  val numNum     = """\d+\.\d+"""
  val numberWithCommas = """(\d+,)+?\d{3}""" + """(?=([^,]|$))"""

  // Abbreviations
  val boundaryNotDot = """($|\s|[“\"?!,:;]|""" + entity + ")" 
  val aa1  = """([A-Za-z]\.){2,}(?=""" + boundaryNotDot + ")"
  val aa2  = """[^A-Za-z]([A-Za-z]\.){1,}[A-Za-z](?=""" + boundaryNotDot + ")"
  val standardAbbreviations = """\b([Mm]r|[Mm]rs|[Mm]s|[Dd]r|[Ss]r|[Jj]r|[Rr]ep|[Ss]en|[Ss]t)\."""
  val arbitraryAbbrev = "(" + aa1 +"|"+ aa2 + "|" + standardAbbreviations + ")"

  val separators  = "(--+|―)"
  val decorations = """[♫]+"""
  val thingsThatSplitWords = """[^\s\.,]"""
  val embeddedApostrophe = thingsThatSplitWords+"""+'""" + thingsThatSplitWords + """+"""

  //  Emoticons
  val normalEyes = "(?iu)[:=]"
  val wink = "[;]"
  val noseArea = "(|o|O|-|[^a-zA-Z0-9 ])"
  val happyMouths = """[D\)\]]+"""
  val sadMouths = """[\(\[]+"""
  val tongue = "[pP]"
  val otherMouths = """[doO/\\]+""" // remove forward slash if http://'s aren't cleaned

  // mouth repetition examples:
  // @aliciakeys Put it in a love song :-))
  // @hellocalyclops =))=))=)) Oh well

  def OR(parts: String*) = {
    "(" + parts.toList.mkString("|") + ")"
  }

  val emoticon = OR(
      // Standard version  :) :( :] :D :P
      OR(normalEyes, wink) + noseArea + OR(tongue, otherMouths, sadMouths, happyMouths),
      
      // reversed version (: D:  use positive lookbehind to remove "(word):"
      // because eyes on the right side is more ambiguous with the standard usage of : ;
      """(?<=( |^))""" + OR(sadMouths,happyMouths,otherMouths) + noseArea + OR(normalEyes, wink) 
      
      // TODO japanese-style emoticons
      // TODO should try a big precompiled lexicon from Wikipedia, Dan Ramage told me (BTO) he does this
  	)

  def allowEntities(pat: String)= {
    // so we can write patterns with < and > and let them match escaped html too
    pat.replace("<", "(<|&lt;)").replace(">", "(>|&gt;)")
  }
  
  val Hearts = allowEntities("""(<+/?3+)""")

  val Arrows = allowEntities("""(<*[-=]*>+|<+[-=]*>*)""")

  // BTO 2011-06: restored Hashtag, AtMention protection (dropped in original scala port) because it fixes
  // "hello (#hashtag)" ==> "hello (#hashtag )"  WRONG
  // "hello (#hashtag)" ==> "hello ( #hashtag )"  RIGHT
  // "hello (@person)" ==> "hello (@person )"  WRONG
  // "hello (@person)" ==> "hello ( @person )"  RIGHT
  // ... Some sort of weird interaction with edgepunct I guess, because edgepunct 
  // has poor content-symbol detection.
  
  val Hashtag = """#[a-zA-Z0-9_]+""";  // also gets #1 #40 which probably aren't hashtags .. but good as tokens

  val AtMention = """@[a-zA-Z0-9_]+""";
  
  // I was worried this would conflict with at-mentions
  // but seems ok in sample of 5800: 7 changes all email fixes
  // http://www.regular-expressions.info/email.html
  val Bound = """(\W|^|$)"""
  val Email = "(?<=" +Bound+ """)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}(?=""" +Bound+")"

  // We will be tokenizing using these regexps as delimiters
  // Additionally, these things are "protected", meaning they shouldn't be further split themselves.
  val Protected  = new Regex(
    OR(
      Hearts,
      Arrows,
      emoticon,
      url,
      Email,
      entity,
      timeLike,
      numNum,
      numberWithCommas,
      punctSeq,
      arbitraryAbbrev,
      separators,
      decorations,
      embeddedApostrophe,
      Hashtag, 
      AtMention
     ))
  
  // Edge punctuation
  // Want: 'foo' => ' foo '
  // While also:   don't => don't
  // the first is considered "edge punctuation".
  // the second is word-internal punctuation -- don't want to mess with it.
  // BTO (2011-06): the edgepunct system seems to be the #1 source of problems these days.  
  // I remember it causing lots of trouble in the past as well.  Would be good to revisit or eliminate.
  
  // Note the 'smart quotes' (http://en.wikipedia.org/wiki/Smart_quotes)
  val edgePunctChars    = """'"“”‘’«»{}\(\)\[\]\*"""
  val edgePunct    = "[" + edgePunctChars + "]"
  val notEdgePunct = "[a-zA-Z0-9]" // content characters
  val offEdge = """(^|$|:|;|\s)"""  // colon here gets "(hello):" ==> "( hello ):"
  val EdgePunctLeft  = new Regex(offEdge + "("+edgePunct+"+)("+notEdgePunct+")")
  val EdgePunctRight = new Regex("("+notEdgePunct+")("+edgePunct+"+)" + offEdge)

  def splitEdgePunct (input: String) = {
    var s = input
    s = EdgePunctLeft.replaceAllIn(s, "$1$2 $3")
    s = EdgePunctRight.replaceAllIn(s, "$1 $2$3")
    s
  }

  // The main work of tokenizing a tweet.
  def simpleTokenize (text: String) = {

	// Do the no-brainers first
    val splitPunctText = splitEdgePunct(text)

    val textLength = splitPunctText.length

    // Find the matches for subsequences that should be protected,
    // e.g. URLs, 1.0, U.N.K.L.E., 12:53
    val matches = Protected.findAllIn(splitPunctText).matchData.toList

    // The spans of the "bads" should not be split.
    val badSpans = matches map (mat => Tuple2(mat.start, mat.end))

    // Create a list of indices to create the "goods", which can be
    // split. We are taking "bad" spans like 
    //     List((2,5), (8,10)) 
    // to create 
    ///    List(0, 2, 5, 8, 10, 12)
    // where, e.g., "12" here would be the textLength
    val indices = (0 :: badSpans.foldRight(List[Int]())((x,y) => x._1 :: x._2 :: y)) ::: List(textLength)
    
    // Group the indices and map them to their respective portion of the string
    val goods = indices.grouped(2).map { x => splitPunctText.slice(x(0),x(1)) }.toList

    //The 'good' strings are safe to be further tokenized by whitespace
    val splitGoods = goods map { str => str.trim.split(" ").toList }

    //Storing as List[List[String]] to make zip easier later on 
    val bads = badSpans map { case(start,end) => List(splitPunctText.slice(start,end)) }

    //  Reinterpolate the 'good' and 'bad' Lists, ensuring that
    //  additonal tokens from last good item get included
    val zippedStr = 
      (if (splitGoods.length == bads.length) 
        splitGoods.zip(bads) map { pair => pair._1 ++ pair._2 }
      else 
        (splitGoods.zip(bads) map { pair => pair._1 ++ pair._2 }) ::: List(splitGoods.last)
     ).flatten

    // Split based on special patterns (like contractions) and check all tokens are non empty
    zippedStr.map(splitToken(_)).flatten.filter(_.length > 0)
  }  


  // "foo   bar" => "foo bar"
  def squeezeWhitespace (input: String) = Whitespace.replaceAllIn(input," ").trim

  // Final pass tokenization based on special patterns
  def splitToken (token: String) = {
    token match {
      // BTO: our POS tagger wants "ur" and "you're" to both be one token.
      // Uncomment to get "you 're"
//      case Contractions(stem, contr) => List(stem.trim, contr.trim)
      case token => List(token.trim)
    }
  }

  // Apply method allows it to be used as Twokenize(line) in Scala.
  def apply (text: String): List[String] = simpleTokenize(squeezeWhitespace(text))

  // More normal name for @apply@
  def tokenize (text: String): List[String] = apply(text)
    

  // Very slight normalization for AFTER tokenization.
  // The tokenization regexes are written to work on non-normalized text.
  // (to make byte offsets easier to compute)
  // Hm: 2+ repeated character normalization here?
  // No, that's more linguistic, should be further down the pipeline 
  def normalizeText(text: String) = {
    text.replaceAll("&lt;", "<").replaceAll("&gt;",">").replaceAll("&amp;","&")
  }

  def tokenizeForTagger (text: String): List[String] = {
    tokenize(text).map(normalizeText)
  }
  
  def tokenizeForTagger_J (text: String): java.util.List[String] = {
    tokenizeForTagger(text).toSeq
  }

  // Convenience method to produce a string representation of the 
  // tokenized tweet in a standard-ish format.
  def tokenizeToString (text: String): String = {
  	tokenizeForTagger(text).mkString(" ");
  }

  // Main method
  def main (args: Array[String]) = {
    // force stdin/stdout interpretation as UTF-8
    // and ignore the stupid JVM default settings (MacRoman? wtf??)
    Console.setOut(new java.io.PrintStream(System.out, true, "UTF8"))
    io.Source.fromInputStream(System.in, "UTF-8").getLines foreach {
      line => {
        println(tokenizeForTagger(line).reduceLeft(_ + " " + _))
      }
    }
  }

}
