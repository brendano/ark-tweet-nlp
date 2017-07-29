package cmu.arktweetnlp.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.pirkaengine.mobile.Emoji;

import java.util.ArrayList;
import java.util.List;

public class EmojiUtil {

    public static Pair<String, List<Emoji>> filterEmoji(String text) {
        StringBuffer term   = new StringBuffer("");
        List<Emoji>  emojis = new ArrayList<Emoji>();

        if (text != null && !text.isEmpty()) {
            Emoji em;
            for (int i = 0; i < text.length(); ) {
                final int codePoint = text.codePointAt(i);
                em = codePoint < 0x1FFFF ? Emoji.charOf(codePoint) : null;
                if (null == em) {
                    final String chars = new String(Character.toChars(codePoint));
                    term.append(chars);
                } else {
                    emojis.add(em);
                }
                i += Character.charCount(codePoint);
            }
        }

        Pair<String, List<Emoji>> out = new ImmutablePair<String, List<Emoji>>(term.toString(), emojis);
        return out;
    }
}
