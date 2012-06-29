package newalgo;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds textual and linguistic information for a sentence.
 * Theoretically could add additional textual,syntactic,etc. annotations as inputs
 */
public class Sentence {
    public List<String> tokens;
    /** This is intended to be null for runtime, used only for training **/
    public List<String> labels;

    public Sentence() {
        this.tokens = new ArrayList<String>();
        this.labels = new ArrayList<String>();
    }
    
    public int T() {
        return tokens.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int t = 0; t < T(); t++) {
            sb.append(tokens.get(t)).append("/").append(labels.get(t));
            sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

}