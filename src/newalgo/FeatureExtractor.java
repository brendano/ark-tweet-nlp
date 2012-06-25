package newalgo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import newalgo.util.Util;

import edu.stanford.nlp.util.Pair;

/**
 * Extracts features and numberizes them
 * Also numberizes other things if necessary (e.g. label numberizations for MEMM training)
 */
public class FeatureExtractor {

    /** Only use the model for vocabulary and dimensionality info. **/
    private Model model;
    private ArrayList<FeatureExtractorInterface> allFeatureExtractors;
    public boolean isTrainingTime;
    public boolean dumpMode = false;

    public FeatureExtractor(Model model, boolean isTrainingTime) {
        this.model = model;
        this.isTrainingTime = isTrainingTime;
        assert model.labelVocab.isLocked();
        initializeFeatureExtractors();
    }

    /**
     * Does feature extraction on one sentence.
     * 
     * Input: textual representation of sentence
     * Output: fills up modelSentence with numberized features
     */
    public void computeFeatures(Sentence linguisticSentence, ModelSentence modelSentence) {
        int T = linguisticSentence.T();
        assert linguisticSentence.T() > 0;
        computeObservationFeatures(linguisticSentence, modelSentence);
        
        if (isTrainingTime) {
            for (int t=0; t < T; t++) {
                modelSentence.labels[t] = model.labelVocab.num( linguisticSentence.labels.get(t) );
            }
            computeCheatingEdgeFeatures(linguisticSentence, modelSentence);
        }
    }

    /**
     * Peak at the modelSentence to see its labels -- for training only!
     * @param sentence
     * @param modelSentence
     */
    private void computeCheatingEdgeFeatures(Sentence sentence, ModelSentence modelSentence) {
    	assert isTrainingTime;
        modelSentence.edgeFeatures[0] = model.startMarker();
        for (int t=1; t < sentence.T(); t++) {
            modelSentence.edgeFeatures[t] = modelSentence.labels[t-1];
        }
    }

    private void computeObservationFeatures(Sentence sentence, ModelSentence modelSentence) {
        PositionFeaturePairs pairs = new PositionFeaturePairs();

        // Extract in featurename form
        for (FeatureExtractorInterface fe : allFeatureExtractors) {
        	fe.addFeatures(sentence.tokens, pairs);
        }
        
        // Numberize
        
        for (int i=0; i < pairs.size(); i++) {
            int t = pairs.labelIndexes.get(i);
            String fName = pairs.featureNames.get(i);
            int fID = model.featureVocab.num(fName);
            if ( ! isTrainingTime && fID == -1) {
            	// Skip OOV features at test time.
            	// Note we have implicit conjunctions from base features, so
            	// these are base features that weren't seen for *any* label at training time -- of course they will be useless for us...
            	continue;
            }
            double fValue = pairs.featureValues.get(i);
            modelSentence.observationFeatures.get(t).add(new Pair<Integer,Double>(fID, fValue));
        }
        if (dumpMode) {
        	Util.p("");
        	for (int t=0; t < sentence.T(); t++) {
        		System.out.printf("%s\n\t", sentence.tokens.get(t));
        		for (Pair<Integer,Double> fv : modelSentence.observationFeatures.get(t)) {
        			System.out.printf("%s ", model.featureVocab.name(fv.first));
        		}
        		System.out.printf("\n");
        	}
        }
    }


    public interface FeatureExtractorInterface {
        /**
         * Input: sentence
         * Output: labelIndexes, featureIDs through positionFeaturePairs
         *
         * We want to yield a sequence of (t, featID) pairs, to be conjuncted against label IDs at position t.
         * Represent as parallel arrays.  Ick yes, but we want to save object allocations (is this crazy?)
         * This method should append to both of them.
         */
        public void addFeatures(List<String> tokens, PositionFeaturePairs positionFeaturePairs);
    }

    public static class PositionFeaturePairs {
        public ArrayList<Integer> labelIndexes;
        public ArrayList<String> featureNames;
        public ArrayList<Double> featureValues;
        
        public PositionFeaturePairs() {
            labelIndexes = new ArrayList<Integer>();
            featureNames = new ArrayList<String>();
            featureValues = new ArrayList<Double>();
        }
        public void add(int labelIndex, String featureID) {
        	add(labelIndex, featureID, 1.0);
        }
        public void add(int labelIndex, String featureID, double featureValue) {
            labelIndexes.add(labelIndex);
            featureNames.add(featureID);
            featureValues.add(featureValue);
        }
        public int size() { return featureNames.size(); }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    // Actual feature extractors
    
    // for performance, figuring out a numberization approach faster than string concatenation might help
    // internet suggests that String.format() is slower than string concat
    // maybe can reuse a StringBuilder object? Ideally, would do direct manipulation of a char[] with reuse.
    
    private void initializeFeatureExtractors() {
        allFeatureExtractors = new ArrayList<FeatureExtractorInterface>();
        allFeatureExtractors.add(new WordformFeatures());
        allFeatureExtractors.add(new SimpleOrthFeatures());

        allFeatureExtractors.add(new Positions());

//        allFeatureExtractors.add(new Suffixes());
//        allFeatureExtractors.add(new CapitalizationFeatures());
//        allFeatureExtractors.add(new URLFeatures());
	}
    
    public class WordformFeatures implements FeatureExtractorInterface {
        public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
            for (int t=0; t < tokens.size(); t++) {
                String tok = tokens.get(t);
                pairs.add(t, "Word|" + tok);
            }
        }
    }
    
    public class SimpleOrthFeatures implements FeatureExtractorInterface {
    	public Pattern hasDigit = Pattern.compile(".*[0-9].*");
    	/** TODO change to punctuation class, or better from Twokenize **/
    	public Pattern allPunct = Pattern.compile("^[^a-zA-Z0-9]*$");

    	public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
	    	for (int t=0; t < tokens.size(); t++) {
	    		String tok = tokens.get(t);
	    		
	    		if (hasDigit.matcher(tok).matches())
	    			pairs.add(t, "HasDigit");
	    		
	    		if (tok.charAt(0) == '@')
	    			pairs.add(t, "InitAt");
	    		
	    		if (tok.charAt(0) == '#')
	    			pairs.add(t, "InitHash");
	    		
	    		if (allPunct.matcher(tok).matches())
	    			pairs.add(t, "AllPunct");
	    	}
        }    
    }

    public class Positions implements FeatureExtractorInterface {	
        public void addFeatures(List<String> tokens, PositionFeaturePairs pairs) {
        	pairs.add(0, "t=0");
        }
    }

}
