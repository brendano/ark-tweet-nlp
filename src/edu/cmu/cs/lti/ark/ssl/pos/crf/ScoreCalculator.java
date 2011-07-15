package edu.cmu.cs.lti.ark.ssl.pos.crf;

import java.io.Serializable;
import java.util.List;

import edu.berkeley.nlp.util.ArrayUtil;
import edu.cmu.cs.lti.ark.ssl.pos.EdgeFeatureExtractor;
import edu.cmu.cs.lti.ark.ssl.pos.VertexFeatureExtractor;
import fig.basic.Pair;

public class ScoreCalculator implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2014072565140252832L;
	private VertexFeatureExtractor vertexExtractor;
	private EdgeFeatureExtractor edgeExtractor;
	private int numLabels;
	
	public ScoreCalculator(VertexFeatureExtractor vertexExtractor0,
						   EdgeFeatureExtractor edgeExtractor0,
						   int numLabels0) {
		vertexExtractor = vertexExtractor0;
		edgeExtractor = edgeExtractor0;
		numLabels = numLabels0;
	}

	public double[][] getScoreMatrix(int[] sequence, int index, double[] w) {
		double[][] M = getLinearScoreMatrix(sequence, index, w);
		for (int i=0; i<M.length; i++) {
			M[i] = ArrayUtil.exp(M[i]);
		}
		return M;
	}
	
	public double[] getVertexScores(int[] sequence, int index, double[] w) {
		return ArrayUtil.exp(getLinearVertexScores(sequence, index, w));
	}
	
	public double[][] getLinearScoreMatrix(int[] sequence, int index, double[] w) {
		double[][] M = new double[numLabels][numLabels];
		for (int vc = 0; vc<numLabels; vc++) {
			List<Pair<Integer, Double>> vcFeatures = 
				vertexExtractor.extractFeatures(vc, sequence[index]);
			double vertexScore = dotProduct(vcFeatures, w);
			for (int vp = 0; vp<numLabels; vp++) {
				List<Pair<Integer, Double>> vpFeatures =
					edgeExtractor.extractFeatures(vp, vc);
				double edgeScore = dotProduct(vpFeatures, w);
				M[vp][vc] = vertexScore + edgeScore;
			}
		}
		return M;
	}
	
	public double[] getLinearVertexScores(int[] sequence, int index, double[] w) {
		double[] s = new double[numLabels];
		for (int vc = 0; vc<numLabels; vc++) {
			List<Pair<Integer, Double>> features = 
				vertexExtractor.extractFeatures(vc, sequence[index]);
			double vertexScore = dotProduct(features, w);
			s[vc] = vertexScore;
		}
		return s;
	}

	private double dotProduct(List<Pair<Integer, Double>> features, double[] w) {
		double val = 0.0;
		for(Pair<Integer, Double> feature: features) {
			int index = feature.getFirst();
			double value = feature.getSecond();
			val += value * w[index];
		}
		return val;
	}
}