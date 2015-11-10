package cmu.arktweetnlp;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TwokenizeTest {

    public static final String INPUT_1 = "What's the greatest invention of all time? — Tumblr. http://t.co/IPZPnKqVk2";
    public static final List<String> EXPECTED_TOKENS_1 = Arrays.asList("What's", "the", "greatest", "invention", "of", "all", "time", "?", "—", "Tumblr", ".", "http://t.co/IPZPnKqVk2");

    public static final String INPUT_2 = "Looking for Apple Mac repairers near Naas....Anyone able to help? #kildare  https://t.co/1lhLT6EtWs";
    public static final List<String> EXPECTED_TOKENS_2 = Arrays.asList("Looking", "for", "Apple", "Mac", "repairers", "near", "Naas", "....", "Anyone", "able", "to", "help", "?", "#kildare", "https://t.co/1lhLT6EtWs");

    public static final String INPUT_3 = "RT @EKM94: The best thing I've seen on Twitter all day. http://t.co/lhYh13jUD0";
    public static final List<String> EXPECTED_TOKENS_3 = Arrays.asList("RT", "@EKM94", ":", "The", "best", "thing", "I've", "seen", "on", "Twitter", "all", "day", ".", "http://t.co/lhYh13jUD0");

    public static final String INPUT_4 = "Butterball Turkey Bacon Only $.54 At Walgreens! via Couponing For 4 - Starting the week of 6/28, ... http://t.co/0AdaJsqwIR";
    public static final List<String> EXPECTED_TOKENS_4 = Arrays.asList("Butterball", "Turkey", "Bacon", "Only", "$", ".", "54", "At", "Walgreens", "!", "via", "Couponing", "For", "4", "-", "Starting", "the", "week", "of", "6/28", ",", "...", "http://t.co/0AdaJsqwIR");

    public static final String INPUT_5 = "RT @beingactress: ♥Taking joy in living is a woman’s best cosmetic♥  @actressharshika http://t.co/AF8Bl69Uyu";
    public static final List<String> EXPECTED_TOKENS_5 = Arrays.asList("RT", "@beingactress", ":", "♥", "Taking", "joy", "in", "living", "is", "a", "woman’s", "best", "cosmetic", "♥", "@actressharshika", "http://t.co/AF8Bl69Uyu");

    public static final String INPUT_6 = "@larysaG Well thanks! Making me feel better already lol I'm Nancy btw :) Nice to meet u! I'll try remembering that when I'm terrified there.";
    public static final List<String> EXPECTED_TOKENS_6 = Arrays.asList("@larysaG", "Well", "thanks", "!", "Making", "me", "feel", "better", "already", "lol", "I'm", "Nancy", "btw", ":)", "Nice", "to", "meet", "u", "!", "I'll", "try", "remembering", "that", "when", "I'm", "terrified", "there", ".");

    public static final String INPUT_7 = "*✲ﾟ*｡✧٩(･ิᴗ･ิ๑)۶ luke hemmings from 5sos you make me happy i love you so much , follow me please?@luke5sos*✲ﾟ*｡✧٩(･ิᴗ･ิ๑)۶ 77";
    public static final List<String> EXPECTED_TOKENS_7 = Arrays.asList("*✲ﾟ*", "｡✧٩", "(･ิᴗ･ิ๑)", "۶", "luke", "hemmings", "from", "5sos", "you", "make", "me", "happy", "i", "love", "you", "so", "much", ",", "follow", "me", "please", "?", "@luke5sos", "*✲ﾟ*", "｡✧٩", "(･ิᴗ･ิ๑)", "۶", "77");


    @Test
    public void itShouldTokenizeTweets() throws Exception {
        final List<String> tokens1 = Twokenize.tokenize(INPUT_1);
        assertFalse(tokens1.isEmpty());
        assertEquals(EXPECTED_TOKENS_1.size(), tokens1.size());
        assertEquals(EXPECTED_TOKENS_1, tokens1);



        final List<String> tokens2 = Twokenize.tokenize(INPUT_2);
        assertFalse(tokens2.isEmpty());
        assertEquals(EXPECTED_TOKENS_2.size(), tokens2.size());
        assertEquals(EXPECTED_TOKENS_2, tokens2);


        final List<String> tokens3 = Twokenize.tokenize(INPUT_3);
        assertFalse(tokens3.isEmpty());
        assertEquals(EXPECTED_TOKENS_3.size(), tokens3.size());
        assertEquals(EXPECTED_TOKENS_3, tokens3);


        final List<String> tokens4 = Twokenize.tokenize(INPUT_4);
        assertFalse(tokens4.isEmpty());
        assertEquals(EXPECTED_TOKENS_4.size(), tokens4.size());
        assertEquals(EXPECTED_TOKENS_4, tokens4);


        final List<String> tokens5 = Twokenize.tokenize(INPUT_5);
        assertFalse(tokens5.isEmpty());
        assertEquals(EXPECTED_TOKENS_5.size(), tokens5.size());
        assertEquals(EXPECTED_TOKENS_5, tokens5);


        final List<String> tokens6 = Twokenize.tokenize(INPUT_6);
        assertFalse(tokens6.isEmpty());
        assertEquals(EXPECTED_TOKENS_6.size(), tokens6.size());
        assertEquals(EXPECTED_TOKENS_6, tokens6);


        final List<String> tokens7 = Twokenize.tokenize(INPUT_7);
        assertFalse(tokens7.isEmpty());
        assertEquals(EXPECTED_TOKENS_7.size(), tokens7.size());
        assertEquals(EXPECTED_TOKENS_7, tokens7);
    }

    @Test
    public void itShouldProduceASingleTokenFromAOneWordTweet() {
        final String[] oneTokenInputs = {
                "test",
                "Test",
                "?",
                "!",
                ".",
                "http://test.com",
                ":)"
        };

        for (final String input : oneTokenInputs) {
            assertEquals(Arrays.asList(input), Twokenize.tokenize(input));
        }
    }

    @Test
    public void itShouldProduceAnEmptyListOfTokensForTheEmptyString() {
        final List<String> tokens = Twokenize.tokenize("");
        assertTrue(tokens.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void itShouldThrowNullPointerExceptionWithNullInput() {
        Twokenize.tokenize(null);
    }
}
