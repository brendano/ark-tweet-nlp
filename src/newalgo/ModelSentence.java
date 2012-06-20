package newalgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One sequence structure -- typically, for one sentence
 * This is the model's view of a sentence -- only deals with non-textual numberized versions of everything
 */
public class ModelSentence {
    public int T;

    /** Runtime inferred, Trainingtime observed.
     * dim T
     **/
    public int labels[];

    /** Runtime observed, Trainingtime observed.
     * This is an array-of-arrays of feature IDs.  Only handles binary features.
     * dim (T x M_t)  (variable # nnz featvals per t.)
     **/
//    public int observationFeatures[][];
    public ArrayList<ArrayList<Integer>> observationFeatures;

    /** Runtime observed, Trainingtime observed (for MEMM).
     * dim T st: edgeFeatures[t] = ID of label@(t-1).
     * values in 0..(N_labels-1), plus extra higher numbers for markers (see Model)
     **/
    public int edgeFeatures[];

    ModelSentence(int T) {
        this.T = T;
        labels = new int[T];
        edgeFeatures = new int[T];
        observationFeatures = new ArrayList<ArrayList<Integer>>();
        for (int t=0; t<T; t++) {
            observationFeatures.add( new ArrayList<Integer>() );
        }
        Arrays.fill(labels, -1);
        Arrays.fill(edgeFeatures, -1);
    }
}
