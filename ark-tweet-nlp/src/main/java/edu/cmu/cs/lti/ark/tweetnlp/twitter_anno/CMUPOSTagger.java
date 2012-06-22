package edu.cmu.cs.lti.ark.tweetnlp.twitter_anno;

import com.twitter.common.text.token.TokenStream;
import edu.cmu.cs.lti.ark.tweetnlp.TweetTaggerInstance;
import edu.cmu.cs.lti.ark.tweetnlp.Twokenize;
import org.apache.lucene.util.Attribute;

import java.util.ArrayList;
import java.util.List;

/** Implements the Twitter annotations API CMUPOSTagger interface.
 * See MyTokenizerUsageExample for what that actually means.
 */
public class CMUPOSTagger extends TokenStream {

    // the POS for the current token -- to be accessed in a criminally stateful manner.
    private CMUPOSAttribute posAttr;
    public CMUPOSAttribute getPOSAttr() { return posAttr; }

    // internal state for incrementing around
    private String tweetText;
    private List<String> tweetTags;
    private List<String> tweetTokens;
    private int tokenIndex = -1;

    public CMUPOSTagger() {
        this.posAttr = addAttribute(CMUPOSAttribute.class);  // WTF does this do?
    }

    @Override
    public boolean incrementToken() {
        if (tokenIndex == -1) throw new RuntimeException("haven't given a tweet to tag");
        if (tokenIndex >= tweetTokens.size()) {
            return false;
        }

        posAttr.setTag(tweetTags.get(tokenIndex));
        posAttr.setToken(tweetTokens.get(tokenIndex));

        tokenIndex++;

        return true;
    }

    @Override
    public void reset(CharSequence input) {
        this.tweetTokens = Twokenize.tokenizeForTagger(input.toString());
        this.tweetTags = doTagging(tweetTokens);
        this.tokenIndex = 0;
    }

    private List<String> doTagging(List<String> toks) {
        return TweetTaggerInstance.getInstance().getTagsForOneSentence(toks);
//        return dummyTagging(toks);
    }

    private static List<String> dummyTagging(List<String> toks) {
        ArrayList<String> tags = new ArrayList<String>();
        for (String tok : toks) tags.add("N");
        return tags;
    }

    /** most minimal possible runner */
    public static void main(String[] args) {
        TokenStream stream = new CMUPOSTagger();
        stream.reset("This is what I want to tag.");
        while (stream.incrementToken()) {
            CMUPOSAttribute posAttribute = stream.getAttribute(CMUPOSAttribute.class);
            System.out.printf("token= %s \t| POS= %s\n", posAttribute.getToken(), posAttribute.getTag());
        }
    }
}
