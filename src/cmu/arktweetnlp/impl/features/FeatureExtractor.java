package cmu.arktweetnlp.impl.features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.util.Util;
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
	public FeatureExtractor(Model model, boolean isTrainingTime) throws IOException{
		this.model = model;
		this.isTrainingTime = isTrainingTime;
		assert model.labelVocab.isLocked();
		initializeFeatureExtractors();
	}

	
	public static Logger log = Logger.getLogger("FeatureExtractor");
	
	/**
	 * Does feature extraction on one sentence.
	 * 
	 * Input: textual representation of sentence
	 * Output: fills up modelSentence with numberized features
	 */
	public void computeFeatures(Sentence linguisticSentence, ModelSentence modelSentence) {
		int T = linguisticSentence.T();
		assert linguisticSentence.T() > 0; //TODO: handle this when assertions are off
		computeObservationFeatures(linguisticSentence, modelSentence);
		if (isTrainingTime) {
			for (int t=0; t < T; t++) {
				modelSentence.labels[t] = model.labelVocab.num( linguisticSentence.labels.get(t) );
			}
			computeCheatingEdgeFeatures(linguisticSentence, modelSentence);
		}
	}

	/**
	 * Peek at the modelSentence to see its labels -- for training only!
	 * @param sentence
	 * @param modelSentence
	 */
	private void computeCheatingEdgeFeatures(Sentence sentence, ModelSentence modelSentence) {
		assert isTrainingTime;
		modelSentence.edgeFeatures[0] = -1;
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

		// Numberize.  This should be melded with the addFeatures() loop above, so no wasteful
		// temporaries that later turn out to be OOV... but is this really an issue?
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
		 * Output: labelIndexes, featureIDs/Values through positionFeaturePairs
		 *
		 * We want to yield a sequence of (t, featID, featValue) pairs,
		 * to be conjuncted against label IDs at position t.
		 * Represent as parallel arrays.  Ick yes, but we want to save object allocations (is this crazy?)
		 * This method should append to them.
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



	private void initializeFeatureExtractors() throws IOException {
		allFeatureExtractors = new ArrayList<FeatureExtractorInterface>();
		
		allFeatureExtractors.add(new WordClusterPaths());
		allFeatureExtractors.add(new WordListFeatures.POSTagDict());
		allFeatureExtractors.add(new WordListFeatures.MetaphonePOSDict());

		allFeatureExtractors.add(new MiscFeatures.NgramSuffix(20));
		allFeatureExtractors.add(new MiscFeatures.NgramPrefix(20));
		allFeatureExtractors.add(new MiscFeatures.PrevWord());
		allFeatureExtractors.add(new MiscFeatures.NextWord());
		allFeatureExtractors.add(new MiscFeatures.WordformFeatures());

		allFeatureExtractors.add(new MiscFeatures.CapitalizationFeatures());
		allFeatureExtractors.add(new MiscFeatures.SimpleOrthFeatures());
		allFeatureExtractors.add(new MiscFeatures.PrevNext());
		
		allFeatureExtractors.add(new WordListFeatures.Listofnames("proper_names"));
		allFeatureExtractors.add(new WordListFeatures.Listofnames("celebs")); //2012-08-09 version of freebase celebrity list
		allFeatureExtractors.add(new WordListFeatures.Listofnames("videogame")); //june 22 version of freebase video game list
		allFeatureExtractors.add(new WordListFeatures.Listofnames("mobyplaces"));	//moby dictionary of US locations
		allFeatureExtractors.add(new WordListFeatures.Listofnames("family"));
		allFeatureExtractors.add(new WordListFeatures.Listofnames("male"));
		allFeatureExtractors.add(new WordListFeatures.Listofnames("female"));
		
		allFeatureExtractors.add(new MiscFeatures.Positions());
		
		// These were disabled in the 0.3 release
		//allFeatureExtractors.add(new Prev2Words());
		//allFeatureExtractors.add(new Next2Words());
		//allFeatureExtractors.add(new MiscFeatures.URLFeatures());

	}


	// for performance, figuring out a numberization approach faster than string concatenation might help
	// internet suggests that String.format() is slower than string concat
	// maybe can reuse a StringBuilder object? Ideally, would do direct manipulation of a char[] with reuse.
	// Or, if we move to randomized feature hashing, there are far faster methods
	// e.g. http://www.hpl.hp.com/techreports/2008/HPL-2008-91R1.pdf
}


