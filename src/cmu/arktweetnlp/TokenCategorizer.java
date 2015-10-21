package cmu.arktweetnlp;


import org.pirkaengine.mobile.Emoji;
import java.util.*;

/**
 * Interface for objects that know how to group tokens output from Twokenize
 * into categories of a given type. E.g. mapping certain token types to an enum.
 * @param <T>
 */
public interface TokenCategorizer<T> {
    public Map<T, List<String>> categorize(final String text, final List<List<String>> splitTokens, final List<List<String>> protectedTokens, final List<Emoji> emojis);
}

