package cmu.arktweetnlp;

import java.util.regex.Pattern;

/**
 * Interface for the collection of Patterns needed by the Twokenize module.
 * Allows users to pass in a custom set of patterns or use the DefaultPatternContext bundled with the library.
 */
public interface PatternContext {
    /**
     * @return A pattern that can be used to detect contractions
     */
    public Pattern getContractionPattern();

    /**
     * @return A pattern that can be used to detect whitespace
     */
    public Pattern getWhitespacePattern();

    /**
     * @return A pattern that can be used to detect any desired
     * "protected" tokens -- tokens that should not be split any further.
     */
    public Pattern getProtectedTokenPattern();

    /**
     * @return A pattern that can be used to detect left edge punctuation
     */
    public Pattern getLeftEdgePunctuationPattern();

    /**
     * @return A pattern that can be used to detect right edge punctuation
     */
    public Pattern getRightEdgePunctuationPattern();


    public String splitEdgePunctuation(String input);

    /**
     * @return Trims multiple consecutive white spaces into a single
     * space. E.g. "foo   bar " => "foo bar"
     */
    public String squeezeWhitespace(String input);
}
