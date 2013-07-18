package cmu.arktweetnlp.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import util.Arr;
import util.ChainInfer;
import util.U;

import cmu.arktweetnlp.util.BasicFileIO;
import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.Triple;
import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.util.Pair;

/**
 * This contains
 *
 * (1) Feature and label vocabularies (therefore knowledge of numberization)
 * (2) Model coefficients (and knowledge how to flattenize them for LBFGS's sake)
 * (3) Decoding/posterior and gradient computation
 * 
 * We include inference routines for both MEMM and CRF models here.
 * Routines specific to one of them have a _MEMM or _CRF suffix.
 */
public class Model {
	public Vocabulary labelVocab;
	public Vocabulary featureVocab;

	/** 
	 * dim: N_labels 
	 **/
	public double[] biasCoefs;

	/** 
	 * dim: (N_labels x N_labels) 
	 **/
	public double[][] edgeCoefs;

	/** 
	 * dim: (N_base_features x N_labels)
	 **/
	public double[][] observationFeatureCoefs;


	public Model() {
		labelVocab = new Vocabulary();
		featureVocab = new Vocabulary();
	}
	
	public int numLabels;	//initialized in loadModelFromText

	//	public int startMarker() {
//		assert labelVocab.isLocked();
//		int lastLabel = labelVocab.size() - 1;
//		return lastLabel+1;
//	}

	public void lockdownAfterFeatureExtraction() {
		labelVocab.lock();
		featureVocab.lock();
		allocateCoefs(labelVocab.size(), featureVocab.size());
	}

	public void allocateCoefs(int numLabels, int numObsFeats) {
		observationFeatureCoefs = new double[numObsFeats][numLabels];
		edgeCoefs = new double[numLabels][numLabels];
		biasCoefs = new double[numLabels];
	}
	
	public ChainInfer.Marginals inferPosteriorFullMarginals_CRF(ModelSentence sentence) {
		double[][] edgeFactors = Arr.exp(edgeCoefs);
		double[][] obsFactors = new double[sentence.T][labelVocab.size()];
		for (int t=0; t<sentence.T; t++) {
			computeObsScores(t, sentence, obsFactors[t]);
		}
		Arr.expInPlace(obsFactors);
		ChainInfer.Marginals m = ChainInfer.forwardBackward(obsFactors, edgeFactors);
		return m;
	}

	/** predict a new sentence, assuming a CRF model. store marginal posteriors and MBR decoding. */
	public void decodePosterior_CRF(ModelSentence sentence) {
		int T = sentence.T;
		sentence.labels = new int[T];
		sentence.confidences = new double[T];
		ChainInfer.Marginals m = inferPosteriorFullMarginals_CRF(sentence);
		sentence.labelPosteriors = m.labelMarginals;
		for (int t=0; t<T; t++) {
			sentence.labels[t] = Arr.argmax(m.labelMarginals[t]);
			sentence.confidences[t] = m.labelMarginals[t][ sentence.labels[t] ];
		}
	}

	/** Computes unnormalized log-potentials.
	 * CLOBBERS labelScores **/
	public void computeObsScores(int t, ModelSentence sentence, double[] labelScores) {
		computeBiasScores(labelScores);
		computeObservedFeatureScores(t, sentence, labelScores);
	}

	/** Adds into labelScores **/
	public void computeBiasScores(double[] labelScores) {
		for (int k=0; k < numLabels; k++) {
			labelScores[k] += biasCoefs[k]; 
		}
	}

	/** Adds into labelScores **/
	public void computeObservedFeatureScores(int t, ModelSentence sentence, double[] labelScores) {
		for (int k=0; k < numLabels; k++) {
			for (Pair<Integer,Double> pair : sentence.observationFeatures.get(t)) {
				labelScores[k] += observationFeatureCoefs[pair.first][k] * pair.second;
			}
		}
	}
	public double[] ThreewiseMultiply(double[] a, double[] b, double[] c) {
		if ((a.length != b.length) || (b.length!=c.length)) {
			throw new RuntimeException();
		}
		double[] result = new double[a.length];
		for(int i = 0; i < result.length; i++){
			result[i] = a[i] * b[i] * c[i];
		}
		return result;
	}

	/**
	 * Training-only
	 * 
	 * add-in loglik gradient (direction of higher likelihood), and return the loglik of the sentence
	 **/
	public double computeGradientAndLL_CRF(ModelSentence sentence, double[] grad) {
		assert grad.length == flatIDsize();
		int T = sentence.T;
		double ll = 0;

		ChainInfer.Marginals m = inferPosteriorFullMarginals_CRF(sentence);
		
		for (int t=0; t<T; t++) {
			int y = sentence.labels[t];
			ll += Math.log(m.labelMarginals[t][y]);
			
			for (int k=0; k < numLabels; k++) {
				double delta = (y==k ? 1 : 0) - m.labelMarginals[t][k]; 
				grad[biasFeature_to_flatID(k)] += delta;
				for (Pair<Integer,Double> fv : sentence.observationFeatures.get(t)) {
					grad[observationFeature_to_flatID(fv.first, k)] += delta * fv.second;
				}
				if (t < T-1) {
					for (int next=0; next<numLabels; next++) {
						double empir = (y==k && sentence.labels[t+1]==next) ? 1 : 0;
						double pp = m.pairMarginals[t][k][next];
						grad[edgeFeature_to_flatID(k, next)] += empir - pp;
					}
				}
			}
		}
		return ll;
	}

	/////////////////////////////////////////////////////////

	// Flat-version conversion routines
	// (If this was C++ we could do something clever with memory layout instead to avoid this.)
	// (Or we could do said clever things in Java atop a flat representation, but that would be painful.)

	public void setCoefsFromFlat(double[] flatCoefs) {
		for (int k=0; k<numLabels; k++) {
			biasCoefs[k] = flatCoefs[biasFeature_to_flatID(k)];
		}
		for (int prevLabel=0; prevLabel<numLabels; prevLabel++) {
			for (int k=0; k<numLabels; k++) {
				edgeCoefs[prevLabel][k] = flatCoefs[edgeFeature_to_flatID(prevLabel, k)];
			}
		}
		for (int feat=0; feat < featureVocab.size(); feat++) {
			for (int k=0; k < numLabels; k++) {
				observationFeatureCoefs[feat][k] = flatCoefs[observationFeature_to_flatID(feat, k)];
			}
		}
	}

	public double[] convertCoefsToFlat() {
		double[] flatCoefs = new double[flatIDsize()];
		for (int k=0; k<numLabels; k++) {
			flatCoefs[biasFeature_to_flatID(k)] = biasCoefs[k];
		}
		for (int prevLabel=0; prevLabel<numLabels; prevLabel++) {
			for (int k=0; k<numLabels; k++) {
				flatCoefs[edgeFeature_to_flatID(prevLabel, k)] = edgeCoefs[prevLabel][k];
			}
		}
		for (int feat=0; feat < featureVocab.size(); feat++) {
			for (int k=0; k < numLabels; k++) {
				flatCoefs[observationFeature_to_flatID(feat, k)] = observationFeatureCoefs[feat][k];
			}
		}
		return flatCoefs;
	}

	/////////////////////////////////////////////////////////////////////////

	public int flatIDsize() {
		int K = labelVocab.size();
		int J = featureVocab.size();
		// bias terms + edge features + observation features
		return K + K*K + J*K;
	}
	public int biasFeature_to_flatID(int label) {
		return label;
	}
	public int edgeFeature_to_flatID(int before, int current) {
		int K = labelVocab.size();
		return K + before*K + current;
	}
	public int observationFeature_to_flatID(int featID, int label) {
		int K = labelVocab.size();
		return K + K*K + featID*K + label;
	}

	//////////////////////////////////////////////////

	/*
     todo, think about binary format.  idea


     NumLabels\n[[binary blob for biases]][[binary blob for edge coefs]]
     NumObsFeats\n[[binary blob for obs feats]]

     where NumLabels and NumObsFeats are plaintext.
     there is no separator after the binary blobs, you infer that from NumLabels and NumObsFeats

	 */


	public void saveModelAsText(String outputFilename) throws IOException {
		BufferedWriter writer = BasicFileIO.openFileToWriteUTF8(outputFilename);
		PrintWriter out = new PrintWriter(writer);

		for (int k=0; k<numLabels; k++) {
			out.printf("***BIAS***\t%s\t%g\n", labelVocab.name(k), biasCoefs[k]);
		}
		for (int prevLabel=0; prevLabel < numLabels; prevLabel++) {
			for (int curLabel=0; curLabel < numLabels; curLabel++) {
				out.printf("***EDGE***\t%s %s\t%s\n", prevLabel, curLabel, edgeCoefs[prevLabel][curLabel]);
			}
		}
		assert featureVocab.size() == observationFeatureCoefs.length;
		for (int f=0; f < featureVocab.size(); f++) {
			for (int k=0; k < numLabels; k++) {
				if (observationFeatureCoefs[f][k]==0) continue;
				out.printf("%s\t%s\t%g\n", featureVocab.name(f), labelVocab.name(k), observationFeatureCoefs[f][k]);
			}
		}

		out.close();
		writer.close();
	}

	public static Model loadModelFromText(String filename) throws IOException {
		Model model = new Model();
		BufferedReader reader = BasicFileIO.openFileOrResource(filename);
		String line;

		ArrayList<Double> biasCoefs = 
				new ArrayList<Double>();
		ArrayList< Triple<Integer, Integer, Double> > edgeCoefs = 
				new ArrayList< Triple<Integer, Integer, Double> >();
		ArrayList< Triple<Integer, Integer, Double> > obsCoefs  = 
				new ArrayList< Triple<Integer, Integer, Double> >();

		while ( (line = reader.readLine()) != null ) {
			String[] parts = line.split("\t");
			if ( ! parts[0].equals("***BIAS***")) break;

			model.labelVocab.num(parts[1]);
			biasCoefs.add(Double.parseDouble(parts[2]));
		}
		model.labelVocab.lock();
		model.numLabels = model.labelVocab.size();
		do {
			String[] parts = line.split("\t");
			if ( ! parts[0].equals("***EDGE***")) break;
			String[] edgePair = parts[1].split(" ");
			int prev = Integer.parseInt(edgePair[0]);
			int cur  = Integer.parseInt(edgePair[1]);
			edgeCoefs.add(new Triple(prev, cur, Double.parseDouble(parts[2])));
		} while ( (line = reader.readLine()) != null );
		do {
			String[] parts = line.split("\t");
			int f = model.featureVocab.num(parts[0]);
			int k = model.labelVocab.num(parts[1]);
			obsCoefs.add(new Triple(f, k, Double.parseDouble(parts[2])));
		} while ( (line = reader.readLine()) != null );
		model.featureVocab.lock();

		model.allocateCoefs(model.labelVocab.size(), model.featureVocab.size());

		for (int k=0; k<model.numLabels; k++) {
			model.biasCoefs[k] = biasCoefs.get(k);
		}
		for (Triple<Integer,Integer,Double> x : edgeCoefs) {
			model.edgeCoefs[x.getFirst()][x.getSecond()] = x.getThird();
		}
		for (Triple<Integer,Integer,Double> x : obsCoefs) {
			model.observationFeatureCoefs[x.getFirst()][x.getSecond()] = x.getThird();
		}
		reader.close();
		return model;
	}

	/**
	 * Copies coefs from sourceModel into destModel.
	 * For observation features, only copies features that exist in both.
	 * (Therefore if a feature exists in destModel but not sourceModel, it's not touched.)
	 */
	public static void copyCoefsForIntersectingFeatures(Model sourceModel, Model destModel) {		
		int K = sourceModel.numLabels;

		// We could do the name-checking intersection trick for label vocabs, but punt for now
		if (K != destModel.numLabels) throw new RuntimeException("label vocabs must be same size for warm-start");
		for (int k=0; k < K; k++) {
			if ( ! destModel.labelVocab.name(k).equals(sourceModel.labelVocab.name(k))) {
				throw new RuntimeException("label vocabs must agree for warm-start");
			}
		}

		destModel.biasCoefs = ArrayUtil.copy(sourceModel.biasCoefs);
		destModel.edgeCoefs = ArrayUtil.copy(sourceModel.edgeCoefs);

		// observation features need the intersection
		for (int sourceFeatID=0; sourceFeatID < sourceModel.featureVocab.size(); sourceFeatID++) {
			String featName = sourceModel.featureVocab.name(sourceFeatID);
			if (destModel.featureVocab.contains(featName)) {
				int destFeatID = destModel.featureVocab.num(featName);
				destModel.observationFeatureCoefs[destFeatID] = ArrayUtil.copy(
						sourceModel.observationFeatureCoefs[sourceFeatID] );
			}
		}
	}
	
	/////////////////////////////////////////////
	
	/**
	 * Training-only
	 * 
	 * add-in loglik gradient (direction of higher likelihood) **/
	public double computeGradientAndLL_MEMM(ModelSentence sentence, double[] grad) {
		assert grad.length == flatIDsize();
		int T = sentence.T;
		double[][] posterior = inferPosteriorGivenLabels_MEMM(sentence);
		
		double ll = 0;

		for (int t=0; t<T; t++) {        	
			int prevLabel = sentence.edgeFeatures[t];
			int y = sentence.labels[t];
			ll += Math.log(posterior[t][y]);

			// add empirical counts, subtract model-expected-counts
			for (int k=0; k < numLabels; k++) {
				double p = posterior[t][k];
				int empir = y==k ? 1 : 0;
				grad[biasFeature_to_flatID(k)]                      += empir - p;
				if (prevLabel != -1) {
					grad[edgeFeature_to_flatID(prevLabel, k)]           += empir - p;	
				}
				for (Pair<Integer,Double> fv : sentence.observationFeatures.get(t)) {
					grad[observationFeature_to_flatID(fv.first, k)] += (empir - p) * fv.second;
				}
			}
		}
		return ll;
	}

	/** Adds into labelScores **/
	public void computeEdgeScores_MEMM(int t, ModelSentence sentence, double[] labelScores) {
		int prev = sentence.edgeFeatures[t];
		if (prev==-1) return;
		for (int k=0; k < numLabels; k++) {
			labelScores[k] += edgeCoefs[prev][k];
		}
	}
	
	/** Computes unnormalized log-potentials.
	 * CLOBBERS labelScores **/
	public void computeLabelScores_MEMM(int t, ModelSentence sentence, double[] labelScores) {
		Arrays.fill(labelScores, 0);
		computeBiasScores(labelScores);
		computeEdgeScores_MEMM(t, sentence, labelScores);
		computeObservedFeatureScores(t, sentence, labelScores);
	}

	/** 
	 * THIS CLOBBERS THE LABELS, stores its decoding into them.
	 * Does progressive rolling edge feature extraction
	 **/
	public void decodeGreedy_MEMM(ModelSentence sentence, boolean storeConfidences) {
		int T = sentence.T;
		sentence.labels = new int[T];
		
		if (storeConfidences) {
			sentence.confidences = new double[T];
			sentence.labelPosteriors = new double[T][numLabels];
		}

		double[] labelScores = new double[numLabels];
		for (int t=0; t<T; t++) {
			computeLabelScores_MEMM(t, sentence, labelScores);
			sentence.labels[t] = ArrayMath.argmax(labelScores);
			if (t < T-1)
				sentence.edgeFeatures[t+1] = sentence.labels[t];
			if (storeConfidences) {
				ArrayMath.expInPlace(labelScores);
				double Z = ArrayMath.sum(labelScores);
				ArrayMath.multiplyInPlace(labelScores, 1.0/Z);
				sentence.confidences[t] = labelScores[ sentence.labels[t] ];
				sentence.labelPosteriors[t] = Arr.copy(labelScores);
			}
		}
	}

	/**
	 * "given labels" i.e. at trainingtime labels are observed.
	 * You hide the current one and predict it given you know the previous.
	 * So you get funny incremental posteriors per position that an MEMM uses at trainingtime.
	 * (They don't have a proper full-model posterior marginal
	 * interpretation like a CRF forward-backward-computed posterior does. no?)
	 * 
	 * @param sentence - must its have .labels set
	 * @returns posterior marginals, dim (T x N_label)
	 */
	public double[][] inferPosteriorGivenLabels_MEMM(ModelSentence sentence) {
		double[][] posterior = new double[sentence.T][labelVocab.size()];
		double[] labelScores = new double[numLabels];
		for (int t=0; t<sentence.T; t++) {
			// start in log space
			computeLabelScores_MEMM(t, sentence, labelScores);
			// switch to exp space
			ArrayUtil.expInPlace(labelScores);
			double Z = ArrayUtil.sum(labelScores);

			for (int k=0; k<numLabels; k++) {
				posterior[t][k] = labelScores[k] / Z;
			}
		}
		return posterior;
	}

}
