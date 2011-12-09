package edu.cmu.cs.lti.ark.ssl.pos.crf;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import edu.cmu.cs.lti.ark.ssl.pos.EdgeFeatureExtractor;
import edu.cmu.cs.lti.ark.ssl.pos.VertexFeatureExtractor;
import fig.basic.Pair;

public class Counts { 
	
	private static Logger log = Logger.getLogger(Counts.class.getCanonicalName());
	
	private VertexFeatureExtractor vertexExtractor;
	private EdgeFeatureExtractor edgeExtractor;
	private Inference inf;
	private int numLabels;
	private int numFeatures;
	
	public Counts(VertexFeatureExtractor vertexExtractor, 
				  EdgeFeatureExtractor edgeExtractor, 
				  int numLabels,
				  int numFeatures) {
		this.vertexExtractor = vertexExtractor;
		this.edgeExtractor = edgeExtractor;
		this.inf = new Inference(numLabels, vertexExtractor, edgeExtractor);
		this.numLabels = numLabels;
		this.numFeatures = numFeatures;
	}
	
	public double[] getEmpiricalCounts(int[][] observations, int[][] goldLabels) {
		double[] counts = new double[numFeatures];
		Arrays.fill(counts, 0.0);
		int observationLength = observations.length;
		for (int o = 0; o < observationLength; o++) {
			int[] sequence = observations[o];
			int[] labelSequence = goldLabels[o];
			for (int i = 0; i < sequence.length; i++) {
				int goldLabel = labelSequence[i];
				List<Pair<Integer, Double>> vFeats = 
					vertexExtractor.extractFeatures(goldLabel, sequence[i]);
				for(Pair<Integer, Double> vFeat: vFeats) {
					counts[vFeat.getFirst()] += vFeat.getSecond();
				}				
				if (i>0) {
					int previousGoldLabel = labelSequence[i-1];
					List<Pair<Integer, Double>> eFeats =
						edgeExtractor.extractFeatures(previousGoldLabel, goldLabel);
					for(Pair<Integer, Double> eFeat: eFeats) {
						counts[eFeat.getFirst()] += eFeat.getSecond();
					}
				}
			}
		}
		return counts;
	}
	
	public Pair<Double, double[]> getLogNormalizationAndExpectedCounts(
										int[][] observations, 
										double[] w) {
		double[] counts = new double[numFeatures];
		Arrays.fill(counts, 0.0);
		double totalLogZ = 0.0;
		log.info("Computing expected counts");
		int index = 0;
		int observationLength = observations.length;
		for (int o = 0; o < observationLength; o++) {
			int[] sequence = observations[o];
			Pair<double[][], double[]> alphaAndFactors = 
									inf.getAlphas(sequence, w);
			Pair<double[][], double[]> betaAndFactors = 
									inf.getBetas(sequence, w);
			totalLogZ += inf.getLogNormalizationConstant(alphaAndFactors, betaAndFactors);
			double[][] vertexPosteriors = inf.getVertexPosteriors(alphaAndFactors, betaAndFactors);
			double[][][] edgePosteriors = inf.getEdgePosteriors(sequence, w, alphaAndFactors, betaAndFactors);
			for (int i = 0; i < sequence.length; i++) {
				for (int l=0; l<numLabels; l++) {
					List<Pair<Integer, Double>> vFeats = 
						vertexExtractor.extractFeatures(l, sequence[i]);
					for(Pair<Integer, Double> vFeat: vFeats) {
						counts[vFeat.getFirst()] += 
							vFeat.getSecond() * vertexPosteriors[i][l];
					}
				}	
				if (i>0) {
					for (int pl=0; pl<numLabels; pl++) {
						for (int cl=0; cl<numLabels; cl++) {
							List<Pair<Integer, Double>> eFeats = 
								edgeExtractor.extractFeatures(pl, cl);
							for(Pair<Integer, Double> eFeat: eFeats) {
								counts[eFeat.getFirst()] += 
									eFeat.getSecond() * edgePosteriors[i][pl][cl];
							}
						}
					}
				}			
			}  
			++index;
			if (index % 100 == 0) {
				log.info("Finished: "+ index + "/" + observationLength);
			}
		}		
		return Pair.makePair(totalLogZ, counts);
	}
}