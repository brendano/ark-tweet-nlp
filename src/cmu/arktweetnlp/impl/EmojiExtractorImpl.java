package cmu.arktweetnlp.impl;

import cmu.arktweetnlp.EmojiExtractor;
import cmu.arktweetnlp.util.EmojiUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.pirkaengine.mobile.Emoji;

import java.util.List;

public class EmojiExtractorImpl implements EmojiExtractor {
    @Override
    public Pair<String, List<Emoji>> extractEmojis(final String text) {
        return EmojiUtil.filterEmoji(text);
    }
}
