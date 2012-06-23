package edu.cmu.cs.lti.ark.ssl.pos.crf;


import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.cmu.cs.lti.ark.ssl.pos.EdgeFeatureExtractor;
import edu.cmu.cs.lti.ark.ssl.pos.VertexFeatureExtractor;
import fig.basic.Pair;

public class CRFObjectiveFunction implements DifferentiableFunction {
	
	private int[][] observations;
	private int[][] goldLabels;
	private int numLabels;
	private double sigma;
	private Counts counts;
	private int numFeatures;
	
	double lastValue;
	double[] lastDerivative;
	double[] lastX;
	
	public CRFObjectiveFunction(int[][] observations0, 
								int[][] goldLabels0, 
								int numLabels0, 
								double sigma0,
								VertexFeatureExtractor vertexExtractor,
								EdgeFeatureExtractor edgeExtractor,
								int numFeatures0) {
		observations = observations0;
		goldLabels = goldLabels0;
		numLabels = numLabels0;
		numFeatures = numFeatures0;
		this.counts = new Counts(vertexExtractor, edgeExtractor, numLabels, numFeatures);
		this.sigma = sigma0;
	}

	public int dimension() {
		return numFeatures;
	}

	public double valueAt(double[] x) {
		ensureCache(x);
		return lastValue;
	}

	public double[] derivativeAt(double[] x) {
		ensureCache(x);
		return lastDerivative;
	}

	private void ensureCache(double[] x) {
		if (requiresUpdate(lastX, x)) {
			Pair<Double, double[]> currentValueAndDerivative = calculate(x);
			lastValue = currentValueAndDerivative.getFirst();
			lastDerivative = currentValueAndDerivative.getSecond();
			lastX = DoubleArrays.clone(x);
		}
	}
	
	private boolean requiresUpdate(double[] lastX, double[] x) {
		if (lastX == null) return true;
		for (int i = 0; i < x.length; i++) {
			if (lastX[i] != x[i]) return true;
		}
		return false;
	}
	
	public Pair<Double, double[]> calculate(double[] x) {
		double objective = 0.0;
		double[] derivatives = new double[dimension()];
		double[] empiricalCounts = counts.getEmpiricalCounts(observations, goldLabels);
		for (int l=0; l < empiricalCounts.length; l++) {
				objective -= empiricalCounts[l] * x[l];
				derivatives[l] -= empiricalCounts[l];
		}
		System.out.println("Objective till now:" + objective);
		Pair<Double, double[]> results = counts.getLogNormalizationAndExpectedCounts(observations, x);
		System.out.println(results.getFirst());
		objective += results.getFirst();
		double[] expectedCounts = results.getSecond();
		for (int l=0; l < expectedCounts.length; l++) {
				derivatives[l] += expectedCounts[l];
		}
		for (int i = 0; i < x.length; ++i) {
			double weight = x[i];
			objective += (weight * weight) / (2 * sigma * sigma);
			derivatives[i] += (weight) / (sigma * sigma);
		}
		return Pair.makePair(objective, derivatives);
	}
}
