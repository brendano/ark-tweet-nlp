package cmu.arktweetnlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;
import java.util.regex.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import cmu.arktweetnlp.impl.DefaultPatternContext;
import cmu.arktweetnlp.impl.NoOpEmojiExtractor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.pirkaengine.mobile.Emoji;

/**
 * Twokenize -- a tokenizer designed for Twitter text in English and some other European languages.
 * This is the Java version. If you want the old Python version, see: http://github.com/brendano/tweetmotif
 *
 * This tokenizer code has gone through a long history:
 *
 * (1) Brendan O'Connor wrote original version in Python, http://github.com/brendano/tweetmotif
 *        TweetMotif: Exploratory Search and Topic Summarization for Twitter.
 *        Brendan O'Connor, Michel Krieger, and David Ahn.
 *        ICWSM-2010 (demo track), http://brenocon.com/oconnor_krieger_ahn.icwsm2010.tweetmotif.pdf
 * (2a) Kevin Gimpel and Daniel Mills modified it for POS tagging for the CMU ARK Twitter POS Tagger
 * (2b) Jason Baldridge and David Snyder ported it to Scala
 * (3) Brendan bugfixed the Scala port and merged with POS-specific changes
 *     for the CMU ARK Twitter POS Tagger
 * (4) Tobi Owoputi ported it back to Java and added many improvements (2012-06)
 *
 * Current home is http://github.com/brendano/ark-tweet-nlp and http://www.ark.cs.cmu.edu/TweetNLP
 *
 * There have been at least 2 other Java ports, but they are not in the lineage for the code here.
 */
public class Twokenize {

    private static final PatternContext DEFAULT_PATTERN_CONTEXT = new DefaultPatternContext();
    private static final EmojiExtractor DEFAULT_EMOJI_EXTRACTOR = new NoOpEmojiExtractor();

    /**
     * Represents the results of tokenizing a tweet.
     */
    protected static class TwokenizedTweet {
        private final String originalText;
        private final List<List<String>> splitTokens;
        private final List<List<String>> preservedTokens;
        private final List<Emoji> emojis;

        public TwokenizedTweet(String originalText, List<List<String>> splitGoodTokens, List<List<String>> badTokens, List<Emoji> emojis) {
            this.originalText = originalText;
            this.splitTokens = splitGoodTokens;
            this.preservedTokens = badTokens;
            this.emojis = emojis;
        }

        /**
         * @return The original Tweet text
         */
        public String getOriginalText() {
            return originalText;
        }

        /**
         * @return The tokens that were split into single tokens
         */
        public List<List<String>> getSplitTokens() {
            return splitTokens;
        }

        /**
         * @return The preserved tokens that were not split up. E.g. what's, http://test.com, etc.
         */
        public List<List<String>> getPreservedTokens() {
            return preservedTokens;
        }

        /**
         * @return The emojis detected in the tweet
         */
        public List<Emoji> getEmojis() {
            return emojis;
        }
    }


    // The main work of tokenizing a tweet.
    private static List<String> simpleTokenize (final String text, final PatternContext patterns, final EmojiExtractor emojiExtractor) {
        final TwokenizedTweet twokenizedTweet = tokenizeTweet(text, patterns, emojiExtractor);

        //  Reinterpolate the 'good' and 'bad' Lists, ensuring that
        //  additonal tokens from last good item get included
        List<String> zippedStr= new ArrayList<String>();
        int i;
        for(i=0; i < twokenizedTweet.getPreservedTokens().size(); i++) {
            zippedStr = addAllnonempty(zippedStr, twokenizedTweet.getSplitTokens().get(i));
            zippedStr = addAllnonempty(zippedStr,twokenizedTweet.getPreservedTokens().get(i));
        }
        zippedStr = addAllnonempty(zippedStr,twokenizedTweet.getSplitTokens().get(i));

        // BTO: our POS tagger wants "ur" and "you're" to both be one token.
        // Uncomment to get "you 're"
        /*ArrayList<String> splitStr = new ArrayList<String>(zippedStr.size());
        for(String tok:zippedStr)
        	splitStr.addAll(splitToken(tok));
        zippedStr=splitStr;*/

        return zippedStr;
    }

    protected static TwokenizedTweet tokenizeTweet(final String text, final PatternContext patterns, final EmojiExtractor emojiExtractor) {
        // Do the no-brainers first
        String splitPunctText = patterns.splitEdgePunctuation(text);

        int textLength = splitPunctText.length();

        // BTO: the logic here got quite convoluted via the Scala porting detour
        // It would be good to switch back to a nice simple procedural style like in the Python version
        // ... Scala is such a pain.  Never again.

        // Find the matches for subsequences that should be protected,
        // e.g. URLs, 1.0, U.N.K.L.E., 12:53
        Matcher matches = patterns.getProtectedTokenPattern().matcher(splitPunctText);
        //Storing as List[List[String]] to make zip easier later on
        List<List<String>> bads = new ArrayList<List<String>>();	//linked list?
        List<Pair<Integer,Integer>> badSpans = new ArrayList<Pair<Integer,Integer>>();
        while(matches.find()){
            // The spans of the "bads" should not be split.
            if (matches.start() != matches.end()){ //unnecessary?
                List<String> bad = new ArrayList<String>(1);
                bad.add(splitPunctText.substring(matches.start(),matches.end()));
                bads.add(bad);
                badSpans.add(new ImmutablePair<Integer, Integer>(matches.start(),matches.end()));
            }
        }

        // Create a list of indices to create the "goods", which can be
        // split. We are taking "bad" spans like
        //     List((2,5), (8,10))
        // to create
        ///    List(0, 2, 5, 8, 10, 12)
        // where, e.g., "12" here would be the textLength
        // has an even length and no indices are the same
        List<Integer> indices = new ArrayList<Integer>(2+2*badSpans.size());
        indices.add(0);
        for(Pair<Integer,Integer> p:badSpans){
            indices.add(p.getLeft());
            indices.add(p.getRight());
        }
        indices.add(textLength);

        // Group the indices and map them to their respective portion of the string
        List<List<String>> splitGoods = new ArrayList<List<String>>(indices.size()/2);
        final List<Emoji> emojis = new ArrayList<Emoji>();
        for (int i=0; i<indices.size(); i+=2) {
            String goodstr = splitPunctText.substring(indices.get(i),indices.get(i+1));


            final Pair<String, List<Emoji>> goodStrAndEmojis = emojiExtractor.extractEmojis(goodstr);
            goodstr = goodStrAndEmojis.getLeft();

            emojis.addAll(goodStrAndEmojis.getRight());

            List<String> splitstr = Arrays.asList(goodstr.trim().split(" "));
            splitGoods.add(splitstr);
        }


        return new TwokenizedTweet(text, splitGoods, bads, emojis);
    }

    private static List<String> addAllnonempty(List<String> master, List<String> smaller){
        for (String s : smaller){
            String strim = s.trim();
            if (strim.length() > 0)
                master.add(strim);
        }
        return master;
    }

    /** Assume 'text' has no HTML escaping. **/
    public static List<String> tokenize(String text) {
        return simpleTokenize(DEFAULT_PATTERN_CONTEXT.squeezeWhitespace(text), DEFAULT_PATTERN_CONTEXT, DEFAULT_EMOJI_EXTRACTOR);
    }

    /** Assume 'text' has no HTML escaping. **/
    public static List<String> tokenize(final String text, final PatternContext patternContext, final EmojiExtractor emojiExtractor) {
        return simpleTokenize(patternContext.squeezeWhitespace(text), patternContext, emojiExtractor);
    }

    /**
     * Tokenizes the given text and applies the given categorization function to categorize the tokens into groups
     */
    public static<T> Map<T, List<String>> tokenizeIntoCategories(final String text, final TokenCategorizer<T> categorizer) {
        return tokenizeIntoCategories(text, categorizer, DEFAULT_PATTERN_CONTEXT, DEFAULT_EMOJI_EXTRACTOR);
    }

    /**
     * Same as tokenizeIntoCategories but uses a custom PatternContext and EmojiExtractor
     */
    public static<T> Map<T, List<String>> tokenizeIntoCategories(final String text, final TokenCategorizer<T> categorizer, final PatternContext patterns, final EmojiExtractor emojiExtractor) {
        final String cleaned = patterns.squeezeWhitespace(text);
        final TwokenizedTweet twokenizedTweet = tokenizeTweet(cleaned, patterns, emojiExtractor);
        final Map<T, List<String>> tokenCategories = categorizer.categorize(twokenizedTweet.getOriginalText(), twokenizedTweet.getSplitTokens(), twokenizedTweet.getPreservedTokens(), twokenizedTweet.getEmojis());
        return tokenCategories;
    }


    /**
     * This is intended for raw tweet text -- we do some HTML entity unescaping before running the tagger.
     *
     * This function normalizes the input text BEFORE calling the tokenizer.
     * So the tokens you get back may not exactly correspond to
     * substrings of the original text.
     */
    public static List<String> tokenizeRawTweetText(String text) {
        List<String> tokens = tokenize(normalizeTextForTagger(text));
        return tokens;
    }

    /**
     * Twitter text comes HTML-escaped, so unescape it.
     * We also first unescape &amp;'s, in case the text has been buggily double-escaped.
     */
    public static String normalizeTextForTagger(String text) {
        text = text.replaceAll("&amp;", "&");
        text = StringEscapeUtils.unescapeHtml4(text);
        return text;
    }

    /** Tokenizes tweet texts on standard input, tokenizations on standard output.  Input and output UTF-8. */
    public static void main(String[] args) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
        PrintStream output = new PrintStream(System.out, true, "UTF-8");
    	String line;
    	while ( (line = input.readLine()) != null) {
    		List<String> toks = tokenizeRawTweetText(line);
    		for (int i=0; i<toks.size(); i++) {
    			output.print(toks.get(i));
    			if (i < toks.size()-1) {
    				output.print(" ");
    			}
    		}
    		output.print("\n");
    	}
    }

}
