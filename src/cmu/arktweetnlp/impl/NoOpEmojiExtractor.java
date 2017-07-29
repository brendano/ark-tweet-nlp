package cmu.arktweetnlp.impl;

import cmu.arktweetnlp.EmojiExtractor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.pirkaengine.mobile.Emoji;

import java.util.Collections;
import java.util.List;

public class NoOpEmojiExtractor implements EmojiExtractor {
    @Override
    public Pair<String, List<Emoji>> extractEmojis(String text) {
        return new ImmutablePair<String, List<Emoji>>(text, Collections.<Emoji>emptyList());
    }
}
