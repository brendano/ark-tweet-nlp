package edu.cmu.cs.lti.ark.tweetnlp.twitter_anno;

import org.apache.lucene.util.Attribute;

public interface CMUPOSAttribute extends Attribute {


    /**
     * One-character tagname -- the official format as seen in the annotated training data.
     * We should move to an enum but that would be more work to maintain.
     */
    String getTag();

    /**
     * The token (just a string) which this tag tags
     */
    String getToken();

    void setToken(String token);
    void setTag(String tag);

    Object clone();
}
