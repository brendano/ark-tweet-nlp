package newalgo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import newalgo.util.Util;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.berkeley.nlp.util.StringUtils;
import edu.berkeley.nlp.util.Triple;
import edu.cmu.cs.lti.ark.ssl.util.BasicFileIO;
import edu.stanford.nlp.math.ArrayMath;
import fig.basic.Pair;

/**
 * This contains
 *
 * (1) Feature and label vocabularies (therefore knowledge of numberization)
 * (2) Model coefficients (and knowledge how to flattenize them for LBFGS's sake)
 * (3) Decoding/posterior and gradient computation
 */
public class Model {
    public Vocabulary labelVocab;
    public Vocabulary featureVocab;

    /** 
     * dim: N_labels 
     **/
    public double[] biasCoefs;
    
    /** 
     * dim: (N_labels+1 x N_labels) 
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
    
    public int numLabels() { return labelVocab.size(); }

    public int startMarker() {
        assert labelVocab.isLocked();
        int lastLabel = labelVocab.size() - 1;
        return lastLabel+1;
    }

    public void lockdownAfterFeatureExtraction() {
        labelVocab.lock();
        featureVocab.lock();
        allocateCoefs(labelVocab.size(), featureVocab.size());
    }
    
    public void allocateCoefs(int numLabels, int numObsFeats) {
        observationFeatureCoefs = new double[numObsFeats][numLabels];
        edgeCoefs = new double[numLabels+1][numLabels];
        biasCoefs = new double[numLabels];
    }

    /**
     * "given labels" i.e. at trainingtime labels are observed.  this isn't really decoding, but rather, compute 
     * the funny incremental posteriors you get from an MEMM (that don't have a proper full-model marginalization
     * interpretation like a CRF does. no?)
     * 
     * @param sentence: Input.
     * @returns posterior: Output. dim (T x N_label)
     */
    public double[][] decodeGivenLabels(ModelSentence sentence) {
    	double[][] posterior = new double[sentence.T][labelVocab.size()];
    	double[] labelScores = new double[numLabels()];
    	for (int t=0; t<sentence.T; t++) {
        	// start in log space
    		computeLabelScores(t, sentence, labelScores);
    		// switch to exp space
    		ArrayUtil.expInPlace(labelScores);
    		double Z = ArrayUtil.sum(labelScores);
    		
    		for (int k=0; k<numLabels(); k++) {
    			posterior[t][k] = labelScores[k] / Z;
    		}
//    		if (Math.random() < 0.00001)
//    			System.out.printf("\n%s has %.3g\n%s\n", sentence.labels[t],
//    					posterior[t][sentence.labels[t]], Util.sp(posterior[t]));
    	}
    	return posterior;
    }
    
    /** 
     * THIS CLOBBERS THE LABELS, stores its decoding into them.
     * Does progressive rolling edge feature extraction
     * 
     * question: how to infer posterior marginals?
     **/
	public void greedyDecode(ModelSentence sentence) {
		int T = sentence.T;
		sentence.labels = new int[T];
		sentence.edgeFeatures[0] = startMarker();
    	double[] labelScores = new double[numLabels()];
    	for (int t=0; t<T; t++) {
    		computeLabelScores(t, sentence, labelScores);
    		sentence.labels[t] = ArrayMath.argmax(labelScores);
    		if (t < T-1)
    			sentence.edgeFeatures[t+1] = sentence.labels[t];
    	}
	}

    public void computeLabelScores(int t, ModelSentence sentence, double[] labelScores) {
    	Arrays.fill(labelScores, 0);
    	computeBiasScores(labelScores);
    	computeEdgeScores(t, sentence, labelScores);
    	computeObservedFeatureScores(t, sentence, labelScores);
    }
    
    public void computeBiasScores(double[] labelScores) {
    	for (int k=0; k < numLabels(); k++) {
    		labelScores[k] += biasCoefs[k]; 
    	}
    }
    
    public void computeEdgeScores(int t, ModelSentence sentence, double[] labelScores) {
//    	Util.p(sentence.edgeFeatures);
    	int prev = sentence.edgeFeatures[t];
    	for (int k=0; k < numLabels(); k++) {
    		labelScores[k] += edgeCoefs[prev][k];
    	}
    }
    
    public void computeObservedFeatureScores(int t, ModelSentence sentence, double[] labelScores) {
    	for (int k=0; k < numLabels(); k++) {
    		for (int obsFeat : sentence.observationFeatures.get(t)) {
    			labelScores[k] += observationFeatureCoefs[obsFeat][k];
    		}
    	}
    }

    /**
     * Training-only
     * 
     * add-in loglik gradient (direction of higher likelihood) **/
    public void computeGradient(ModelSentence sentence, double[] grad) {
    	assert grad.length == flatIDsize();
        int T = sentence.T;
        double[][] posterior = decodeGivenLabels(sentence);

        for (int t=0; t<T; t++) {        	
        	int prevLabel = sentence.edgeFeatures[t];
        	int y = sentence.labels[t];

        	// add empirical counts, subtract model-expected-counts
        	for (int k=0; k < numLabels(); k++) {
        		double p = posterior[t][k];
        		int empir = y==k ? 1 : 0;
        		grad[biasFeature_to_flatID(k)] 						+= empir - p;
        		grad[edgeFeature_to_flatID(prevLabel, k)] 			+= empir - p;
        		for (int obsFeat : sentence.observationFeatures.get(t)) {
        			grad[observationFeature_to_flatID(obsFeat, k)] 	+= empir - p;
        		}
        	}
        }
    }
    
	public double computeLogLik(ModelSentence s) {
		double[][] posterior = decodeGivenLabels(s);
		double loglik = 0;
		for (int t=0; t < s.T; t++) {
			int y = s.labels[t];
			loglik += Math.log(posterior[t][y]);
		}
		return loglik;
	}
    
    public void setCoefsFromFlat(double[] flatCoefs) {
    	for (int k=0; k<numLabels(); k++) {
    		biasCoefs[k] = flatCoefs[biasFeature_to_flatID(k)];
    	}
    	for (int prevLabel=0; prevLabel<numLabels()+1; prevLabel++) {
    		for (int k=0; k<numLabels(); k++) {
    			edgeCoefs[prevLabel][k] = flatCoefs[edgeFeature_to_flatID(prevLabel, k)];
    		}
    	}
    	for (int feat=0; feat < featureVocab.size(); feat++) {
    		for (int k=0; k < numLabels(); k++) {
    			observationFeatureCoefs[feat][k] = flatCoefs[observationFeature_to_flatID(feat, k)];
    		}
    	}
    }

    /////////////////////////////////////////////////////////////////////////

    public int flatIDsize() {
        int K = labelVocab.size();
        int J = featureVocab.size();
        // bias terms + edge features + observation features
        return K + (K+1)*K + J*K;
    }
    private int biasFeature_to_flatID(int label) {
        return label;
    }
    private int edgeFeature_to_flatID(int before, int current) {
        int K = labelVocab.size();
        return K + before*K + current;
    }
    private int observationFeature_to_flatID(int featID, int label) {
        int K = labelVocab.size();
        return K + (K+1)*K + featID*K + label;
    }
    
    public boolean isUnregularized(int flatFeatID) {
        int K = labelVocab.size();
    	return flatFeatID < K + (K+1)*K;
    }
//    private int flatID_to_biasFeature(int id) {
//        return id;
//    }
//    private int flatID_to_edgeFeatureBefore(int id) {
//        int K = labelVocab.size();
//        return (int)( (id - K) / (K+1) );
//    }
//    private int flatID_to_edgeFeatureAfter(int id) {
//        int K = labelVocab.size();
//        return (id - K) % (K+1);
//    }
//    private int flatID_to_observationFeature(int id) {
//        int K = labelVocab.size();
//        return id - K - (K+1)*K;
//    }

	public void saveModelAsText(String outputFilename) throws IOException {
		BufferedWriter writer = BasicFileIO.openFileToWrite(outputFilename);
		PrintWriter out = new PrintWriter(writer);
		
		for (int k=0; k<numLabels(); k++) {
			out.printf("***BIAS***\t%s\t%g\n", labelVocab.name(k), biasCoefs[k]);
		}
		for (int prevLabel=0; prevLabel < numLabels()+1; prevLabel++) {
			for (int curLabel=0; curLabel < numLabels(); curLabel++) {
				out.printf("***EDGE***\t%s %s\t%s\n", prevLabel, curLabel, edgeCoefs[prevLabel][curLabel]);
			}
		}
		assert featureVocab.size() == observationFeatureCoefs.length;
		for (int f=0; f < featureVocab.size(); f++) {
			for (int k=0; k < numLabels(); k++) {
				if (observationFeatureCoefs[f][k]==0) continue;
				out.printf("%s\t%s\t%g\n", featureVocab.name(f), labelVocab.name(k), observationFeatureCoefs[f][k]);
			}
		}
		
		out.close();
		writer.close();
	}
	
	public static Model loadModelFromText(String filename) throws IOException {
		Model model = new Model();
        BufferedReader reader = BasicFileIO.openFileToRead(filename);
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
        
        while ( (line = reader.readLine()) != null ) {
        	String[] parts = line.split("\t");
        	if ( ! parts[0].equals("***EDGE***")) break;
    		String[] edgePair = parts[1].split(" ");
    		int prev = Integer.parseInt(edgePair[0]);
    		int cur  = Integer.parseInt(edgePair[1]);
    		edgeCoefs.add(new Triple(prev, cur, Double.parseDouble(parts[2])));
        }
        while ( (line = reader.readLine()) != null ) {
        	String[] parts = line.split("\t");
    		int f = model.featureVocab.num(parts[0]);
    		int k = model.labelVocab.num(parts[1]);
			obsCoefs.add(new Triple(f, k, Double.parseDouble(parts[2])));
    	}
        model.featureVocab.lock();
        
        model.allocateCoefs(model.labelVocab.size(), model.featureVocab.size());
        
        for (int k=0; k<model.numLabels(); k++) {
        	model.biasCoefs[k] = biasCoefs.get(k);
        }
        for (Triple<Integer,Integer,Double> x : edgeCoefs) {
        	model.edgeCoefs[x.getFirst()][x.getSecond()] = x.getThird();
        }
        for (Triple<Integer,Integer,Double> x : obsCoefs) {
        	model.observationFeatureCoefs[x.getFirst()][x.getSecond()] = x.getThird();
        }        
        return model;
	}

}
