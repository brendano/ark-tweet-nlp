package cmu.arktweetnlp.util;

import org.junit.Test;
import org.pirkaengine.mobile.Emoji;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import static org.junit.Assert.*;

public class EmojiUtilTest {

    @Test
    public void itShouldProduceEmptyOutputForEmptyInput() {
        final Pair<String, List<Emoji>> emojis = EmojiUtil.filterEmoji("");
        assertTrue(emojis.getLeft().isEmpty());
        assertTrue(emojis.getRight().isEmpty());
    }

    @Test
    public void itShouldProduceEmptyOutputForNullInput() {
        final Pair<String, List<Emoji>> emojis = EmojiUtil.filterEmoji(null);
        assertTrue(emojis.getLeft().isEmpty());
        assertTrue(emojis.getRight().isEmpty());
    }

    @Test
    public void itShouldExtractEmojisFromText() {
        //"RT @Annam1181orM: @LotAgar @Dom70Bcn @paquifer1969 @V_alf_V @MnicaRebullCome PPS\uD83D\uDC7EE=\uD83D\uDC01Tots!Son\uD83D\uDC01\uD83D\uDC00=FRANKISTESfeixistesQ\uD83D\uDC00varen\uD83D\uDC01MATAR×ODI aCAT➡Ca…";


        Pair<String, List<Emoji>> emojis = null;

        emojis = EmojiUtil.filterEmoji("Hello \uD83D\uDC7E");
        assertEmojiEquals("Hello ", Arrays.asList(Emoji.ALIEN_MONSTER), emojis);

        emojis = EmojiUtil.filterEmoji("He\uD83D\uDC7Ello");
        assertEmojiEquals("Hello", Arrays.asList(Emoji.ALIEN_MONSTER), emojis);


        emojis = EmojiUtil.filterEmoji("This has 2 \uD83D\uDC7E \uD83D\uDC7D emojis!");
        assertEmojiEquals("This has 2   emojis!", Arrays.asList(Emoji.ALIEN_MONSTER, Emoji.EXTRATERRESTRIAL_ALIEN), emojis);
    }

    @Test
    public void itShouldLeaveUnknownEmojisUntouched() {
        final String text = "The following unicode characters are not a known emoji:\uD83D\uDC01. But this one is: \u27A1!";
        final Pair<String, List<Emoji>> emojis = EmojiUtil.filterEmoji(text);
        assertEmojiEquals("The following unicode characters are not a known emoji:\uD83D\uDC01. But this one is: !", Arrays.asList(Emoji.BLACK_RIGHTWARDS_ARROW), emojis);
    }

    private void assertEmojiEquals(final String expectedText, final Collection<Emoji> expectedEmoji, final Pair<String, List<Emoji>> actual) {
        assertTrue(actual != null);
        assertEquals(expectedText, actual.getLeft());
        assertEquals(expectedEmoji.size(), actual.getRight().size());
        assertEquals(expectedEmoji, actual.getRight());
    }
}
