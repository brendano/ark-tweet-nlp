package cmu.arktweetnlp.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultPatternContextTest {

    final DefaultPatternContext context = new DefaultPatternContext();

    @Test
    public void itShouldSqeeuzeWhitespace() {
        assertEquals("", context.squeezeWhitespace(""));
        assertEquals("Hello world.", context.squeezeWhitespace("Hello world."));
        assertEquals("Hello world.", context.squeezeWhitespace("Hello      world."));
    }

    @Test(expected = NullPointerException.class)
    public void itShouldThrowNullPointerWhenSqueezingNull() {
        context.squeezeWhitespace(null);
    }

    @Test
    public void itShouldSplitEdgePunctuation() {
        assertEquals("", context.splitEdgePunctuation(""));

        final String split = context.splitEdgePunctuation("*hello*");
        assertEquals("* hello *", split);
    }

    @Test(expected = NullPointerException.class)
    public void itShouldThrowNullPointerWhenSplittingEdgePunctOnNull() {
        context.squeezeWhitespace(null);
    }



}
