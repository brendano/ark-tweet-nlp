package cmu.arktweetnlp;

import org.apache.commons.lang3.tuple.Pair;
import org.pirkaengine.mobile.Emoji;
import java.util.List;


/**
 * Interface for objects that know how to extract emojis from text.
 */
public interface EmojiExtractor {
    public Pair<String, List<Emoji>> extractEmojis(final String text);
}
