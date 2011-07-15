package edu.cmu.cs.lti.ark.tweetnlp.twitter_anno;

import org.apache.lucene.util.Attribute;

public class CMUPOSAttribute implements Attribute {

    /** One-character tagname -- the official format as seen in the annotated training data.
     * We could move to an enum but that would be more work to maintain.
     */
    public String tag;

    /** The token (just a string) which this tag tags */
    public String token;

    public CMUPOSAttribute(String token, String tag) {
        this.token = token;
        this.tag = tag;
    }
}
