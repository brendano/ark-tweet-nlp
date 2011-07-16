package edu.cmu.cs.lti.ark.tweetnlp.twitter_anno;

import com.twitter.common.text.token.TokenStream;
import com.twitter.common.text.token.TokenizedCharSequence;
import com.twitter.common.text.token.TokenizedCharSequence.Token;
import com.twitter.common.text.token.attribute.CharSequenceTermAttribute;
import com.twitter.common.text.token.attribute.TokenTypeAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Attribute;

import java.util.Iterator;

/**
 * Annotated example illustrating major features of {@link com.twitter.common.text.DefaultTextTokenizer}.
 */
public class MyTokenizerUsageExample {
  private static final String[] famousTweets = {
      // http://twitter.com/#!/BarackObama/status/992176676
      "We just made history. All of this happened because you gave your time, talent and passion."
        + "All of this happened because of you. Thanks",
      // http://twitter.com/#!/jkrums/status/1121915133
      "http://twitpic.com/135xa - There's a plane in the Hudson."
        + " I'm on the ferry going to pick up the people. Crazy.",
      // http://twitter.com/#!/carlbildt/status/73498110629904384
      "@khalidalkhalifa Trying to get in touch with you on an issue.",
      // http://twitter.com/#!/SHAQ/status/75996821360615425
      "im retiring Video: http://bit.ly/kvLtE3 #ShaqRetires"
  };

  public static void main(String[] args) {
    // This is the canonical way to create a token stream.
//    DefaultTextTokenizer tokenizer =
//        new DefaultTextTokenizer.Builder().setKeepPunctuation(true).build();
//    TokenStream stream = tokenizer.getDefaultTokenStream();
// BTO: above turns out to be a TokenizedCharSequenceStream

    TokenStream stream = new CMUPOSTagger();

    // We're going to ask the token stream what type of attributes it makes available. "Attributes"
    // can be understood as "annotations" on the original text.
    System.out.println("Attributes available:");
    Iterator<Class<? extends Attribute>> iter = stream.getAttributeClassesIterator();
    while (iter.hasNext()) {
      Class<? extends Attribute> c = iter.next();
      System.out.println(" - " + c.getCanonicalName());
    }
    System.out.println("");

    // We're now going to iterate through a few tweets and tokenize each in turn.
    for (String tweet : famousTweets) {
      // We're first going to demonstrate the "token-by-token" method of consuming tweets.
      System.out.println("Processing: " + tweet);
      // Reset the token stream to process new input.
      stream.reset(tweet);

      // Now we're going to consume tokens from the stream.
      int tokenCnt = 0;
      while (stream.incrementToken()) {
          // TODO these all don't work


        // CharSequenceTermAttribute holds the actual token text. This is preferred over
        // TermAttribute because it avoids creating new String objects.

        CharSequenceTermAttribute termAttribute = stream
            .getAttribute(CharSequenceTermAttribute.class);

        // OffsetAttribute holds indexes into the original String that the current token occupies.
        // The startOffset is character position is inclusive, the endOffset is exclusive.

        OffsetAttribute offsetAttribute = stream.getAttribute(OffsetAttribute.class);

        // TokenTypeAttribute holds, as you'd expect, the type of the token.
        TokenTypeAttribute typeAttribute = stream.getAttribute(TokenTypeAttribute.class);
        System.out.println(String.format("token %2d (%3d, %3d) type: %12s, token: '%s'",
                tokenCnt, offsetAttribute.startOffset(), offsetAttribute.endOffset(),
                typeAttribute.getType().name, termAttribute.getTermCharSequence()));
        tokenCnt++;
      }
      System.out.println("");

      // We're now going to demonstrate the TokenizedCharSequence API.
      // This should produce exactly the same result as above.
//      tokenCnt = 0;
//      System.out.println("Processing: " + tweet);
//      TokenizedCharSequence tokSeq = tokenizer.tokenize(tweet);
//      for (Token tok : tokSeq.getTokens()) {
//        System.out.println(String.format("token %2d (%3d, %3d) type: %12s, token: '%s'",
//            tokenCnt, tok.getOffset(), tok.getOffset() + tok.getLength(),
//            tok.getType().name, tok.getTerm()));
//        tokenCnt++;
//      }
//      System.out.println("");
    }
  }
}
