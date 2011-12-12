package edu.cmu.cs.lti.ark.tweetnlp.twitter_anno;

import org.apache.lucene.util.AttributeImpl;

/** warning, the boilerplate-like inherited methods haven't been tested much -BTO */
public class CMUPOSAttributeImpl extends AttributeImpl implements CMUPOSAttribute {
    private String tag;

    private String token;

    public CMUPOSAttributeImpl() {
        System.out.println("construct");
    }

    public CMUPOSAttributeImpl(String token, String tag) {
        this.setToken(token);
        this.setTag(tag);
    }


    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void clear() {
        this.token = null;
        this.tag = null;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        if (target instanceof CMUPOSAttributeImpl) {
            ((CMUPOSAttributeImpl) target).setTag(getTag());
            ((CMUPOSAttributeImpl) target).setToken(getToken());
        }
    }

    @Override
    public boolean equals(Object other) {
        return other != null
        && other instanceof CMUPOSAttributeImpl
        && ((CMUPOSAttributeImpl) other).tag == this.tag;
    }

    @Override
    public int hashCode() {
        return getTag().hashCode();
    }

    public Object clone() {
        CMUPOSAttributeImpl result = (CMUPOSAttributeImpl) super.clone();
        return result;
    }



}
