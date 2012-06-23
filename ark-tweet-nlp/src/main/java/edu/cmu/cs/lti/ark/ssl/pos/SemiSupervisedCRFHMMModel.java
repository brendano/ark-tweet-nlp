package edu.cmu.cs.lti.ark.ssl.pos;


import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.cmu.cs.lti.ark.ssl.pos.crf.CRFObjectiveFunction;
import fig.basic.Pair;

public class SemiSupervisedCRFHMMModel implements DifferentiableFunction {

	private GradientGenSequenceModel hmmModel;
	private CRFObjectiveFunction crfModel;
	private int numFeatures;
	
	private double lastValue;
	private double[] lastDerivative;
	private double[] lastX;
	private double gamma;
	
	
	public SemiSupervisedCRFHMMModel(int[][] lObservations0, 
			int[][] goldLabels0,
			int[][] uObservations0,
			int numLabels0, 
			int numObservations0,
			double sigma0,
			VertexFeatureExtractor vertexExtractor,
			EdgeFeatureExtractor edgeExtractor,
			int numFeatures0, double gamma0,
			boolean storePosteriors) {
		hmmModel = 
			new GradientGenSequenceModel(uObservations0,
					null,
					numLabels0, 
					numObservations0,
					storePosteriors);
		crfModel = 
			new CRFObjectiveFunction(lObservations0, 
					goldLabels0, 
					numLabels0,
					sigma0, 
					vertexExtractor, 
					edgeExtractor, 
					numFeatures0);
		numFeatures = numFeatures0;
		gamma = gamma0;
	}

	public GradientGenSequenceModel getHMMModel() {
		return hmmModel;
	}
	
	public CRFObjectiveFunction getCRFModel() {
		return crfModel;
	}	
	
	public double[] derivativeAt(double[] x) {
		ensureCache(x);
		return lastDerivative;
	}

	public int dimension() {
		return numFeatures;
	}

	public double valueAt(double[] x) {
		ensureCache(x);
		return lastValue;
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
	
	private Pair<Double, double[]> calculate(double[] x) {
		Pair<Double, double[]> calcHMM = hmmModel.calculate(x);
		Pair<Double, double[]> calcCRF = crfModel.calculate(x);
		
		double totalValue = gamma * calcHMM.getFirst() + calcCRF.getFirst();
		
		int size = calcHMM.getSecond().length;
		double[] derivatives = new double[size];
		for (int i = 0; i < derivatives.length; i ++) {
			derivatives[i] = calcCRF.getSecond()[i] + gamma * calcHMM.getSecond()[i];
		}		
		return Pair.makePair(totalValue, derivatives);
	}
}